const fs = require("fs");
const turf = require("@turf/turf");
const LandLotModel = require("../models/landLotModel.js");
const LandUseModel = require("../models/landUseModel.js");
const KoModel = require("../models/koModel.js");
const EarthquakeModel = require("../models/earthquakeModel.js");


//NOTE: reading from /public folder as the same data is used for heat maps
const floodPointsData = JSON.parse(fs.readFileSync("./public/flood_point_features.geojson", "utf8"));
const landslidePointData = JSON.parse(fs.readFileSync("./public/landslide_point_features.geojson", "utf8"));


//Št. katastrske občine -> name map; populated on start
const KO_cache = {};


//Initializer routine
(async () => {
    const ko_list = await KoModel.find();

    ko_list.forEach(e => {
        KO_cache[e.id] = e.name;
    });
})();



/*
    Get land type from land use intersection.
    Sorted by intersection area
    Returns array of elements -> [land use type, area fraction]
*/
const getLandTypeInfo = async (polygon) => {
    const landLotArea = turf.area(polygon);
    let landUse = [];

    try {
        landUse = await LandUseModel.find({
            geometry: {
                $geoIntersects: {
                    $geometry: polygon.geometry
                }
            }
        });
    }
    catch(err){
        console.log("Error in getLandTypeInfo:", err);
        return [];
    };

    const typeArea = {};

    landUse.forEach(l => {
        const intersection = turf.intersect(turf.featureCollection([polygon, turf.polygon(l.geometry.coordinates)]));

        if(intersection != null){
            const intersectionArea = turf.area(intersection.geometry);

            if(typeArea[l.properties.RABA_ID] == null){
                typeArea[l.properties.RABA_ID] = 0;
            };

            typeArea[l.properties.RABA_ID] += intersectionArea;
        };
    });

    const val_area = Object.entries(typeArea)
        .sort((a, b) => b[1] - a[1])
        .map(e => [e[0], e[1] / landLotArea]);

    return val_area;
};



//Get land lots in a given polygon
const getLandLotsInArea = async (polygon) => {

    try {
        const lots = await LandLotModel.find({
            geometry: {
                $geoIntersects: {
                    $geometry: polygon.geometry,
                }
            }
        });

        return lots;
    }
    catch(err){
        console.log("Error in getLandLotsInArea:", err);
        return [];
    };
};



//Get near earthquakes
const getNearEarthquakes = async (point, distance) => {
    try {
        const list = await EarthquakeModel.find({
            geometry: {
                $near: {
                    $geometry: point,
                    $maxDistance: distance,
                },
            },
        });

        return list;
    }
    catch(err){
        console.log("Error in getNearEarthquakes", err);
        return [];
    };
};



//BBOX (array: [minx, miny, maxx, maxy]) to turf.polygon
const makePolygon = (bbox) => turf.polygon([[ [bbox[0], bbox[1]], [bbox[0], bbox[3]], [bbox[2], bbox[3]], [bbox[2], bbox[1]], [bbox[0], bbox[1]] ]]);



//Get features and their distance from collection with max distance from a provided point (point = turf.point)
const getNearFeatures = (collection, point, maxDist) => {
    const results = [];
    
    collection.features.forEach(f => {
        const dst = turf.distance(point, turf.point(f.geometry.coordinates));

        if(dst < maxDist) {
            results.push({ feature: f, distance: dst });
        };
    });

    return results;
};




//Generate an assessmenet for a particular area with given parameters
const assessArea = async (areaPolygon) => {

    const areaCenter = turf.centerOfMass(areaPolygon);


    /*
        Flood score calculation (adjusted to match the heat-map)
    */
    let floodScore = 0;
    const MAX_FLOOD_DST = 0.630; //km - This seems to work the best (~0.62 is the distance between two data points)

    const calcFloodPointScore = (feature, dst, max_dst = MAX_FLOOD_DST) => {
        const score_map = { "0.2": 10, "0.5": 20, "0.8": 35 }; //Flood points for heatmap have score values; we have a different ranking system here
        const factor = 1 - (dst / max_dst);
        return score_map[feature.properties.score] * factor;
    };

    const nearFloodPoints = getNearFeatures(floodPointsData, areaCenter, MAX_FLOOD_DST);
    
    if(nearFloodPoints.length > 0){
        floodScore = nearFloodPoints.map(el => calcFloodPointScore(el.feature, el.distance)).reduce((acc, c) => acc + c);
        floodScore = Math.min(floodScore, 100);
    };


    /*
        Landslide score calculation (tried to match heat-map; steep drop-off with distance to exaggerate points with high risk)
        TODO: Data is not that dense; consider generating a more detailed set only for evaluation purposes?
    */
    let landslideScore = 0;
    const MAX_LANDSLIDE_DST = 0.300; //km
    const MAX_LANDSLIDE_SCORE = 143; //Maximum single point score

    const calcLandslideScore = (feature, dst, max_dst = MAX_LANDSLIDE_DST) => {
	    const factor = 1 - (dst / max_dst);
	    return Math.pow(feature.properties.score / MAX_LANDSLIDE_SCORE, 5) * factor * 100;
    };

    const nearLandslidePoints = getNearFeatures(landslidePointData, areaCenter, MAX_LANDSLIDE_DST);

    if(nearLandslidePoints.length > 0){
        landslideScore = nearLandslidePoints.map(el => calcLandslideScore(el.feature, el.distance)).reduce((acc, c) => acc + c);
        landslideScore = Math.min(landslideScore, 100);
    };


    /*
        Earthquake sore calculation (based on what seems ok given displayed data and some common sense)
        TODO: NEEDS A PROPER FORMULA! AS THIS ONE IS MADE UP AND DESIGNED IN A WAY TO SHOW WHAT LOOKS OK AND NOT WHAT IS ACTUALLY OK
    */
    let eqScore = 0;
    const MAX_EARTHQUAKE_DST = 20; //km

    const calcEarthquakeScore = (feature, dst, max_dst = MAX_EARTHQUAKE_DST) => {
        const factor = 1 - (dst / max_dst);
        return Math.pow((100 - feature.properties.depth) / 100, 2) * Math.pow(feature.properties.magnitude / 10, 2) * factor * 100;
    };

    const nearEQs = await getNearEarthquakes(areaCenter.geometry, MAX_EARTHQUAKE_DST * 1000);
    console.log("nearEQs:", nearEQs.length);

    if(nearEQs.length > 0){

        eqScore = nearEQs.map(el => {
            const distance = turf.distance(areaCenter, turf.point(el.geometry.coordinates));
            return calcEarthquakeScore(el, distance);
        })
        .reduce((acc, c) => acc + c);

        eqScore = Math.min(eqScore, 100);
    };


    return {
        floodRisk: floodScore,
        landSlideRisk: landslideScore,
        earthQuakeRisk: eqScore,
    };
};




module.exports = {


    //Get map features (land lots) in a given bounding box
    mapQuery: async (req, res) => {

        //Check bbox
        if(!req.params.bbox_data){
            return res.status(500).json({ message: "Invalid parameters" });
        };

        const bbox_data = req.params.bbox_data.split(",").map(e => +e);

        if(bbox_data.length != 8){
            return res.status(500).json({ message: "Invalid parameters" });
        };

        const prev_bbox = bbox_data.slice(0, 4);
        const bbox = bbox_data.slice(4);

        const from = turf.point([bbox[0], bbox[1]]);
        const to = turf.point([bbox[2], bbox[3]]);
        const distance = turf.distance(from, to); //in km

        if(distance > 10){
            return res.status(500).json({ message: "Out of range" });
        };


        //Get land lots in newly visible area
        const bbox_polygon = makePolygon(bbox);
        const prev_bbox_polygon = makePolygon(prev_bbox);

        const difference = turf.difference(turf.featureCollection([bbox_polygon, prev_bbox_polygon]));

        if(difference == null){
            return res.status(304).end();
        };

        //const differenceArea = turf.area(difference.geometry);
        const landLots = await getLandLotsInArea(difference);

        res.json({
            data: landLots,
            //area: differenceArea,
        });
    },



    //Find feature locations by identifier (št. parcele, katastrske občine)
    mapFind: async (req, res) => {

        if(!req.params.land_lot_id){
            return res.status(500).json({ message: "Missing required parameter" });
        };

        let ko_id = null;

        if(req.params.ko_id){
            ko_id = parseInt(req.params.ko_id);

            if(isNaN(ko_id)){
                return res.status(500).json({ message: "Invalid parameter" });
            };
        };

        try {
            let landLots = await LandLotModel.find({ "properties.ST_PARCELE": req.params.land_lot_id });

            if(ko_id != null){
                landLots = landLots.filter(l => l.properties.KO_ID == ko_id); //NOTE: filtered here as there are at most ~50 results
            };

            const results = landLots.map(l => {
                return {
                    ko_id: l.properties.KO_ID,
                    ko_name: KO_cache[l.properties.KO_ID] || "??",
                    st_parcele: l.properties.ST_PARCELE,
                    bbox: turf.bbox(turf.polygon(l.geometry.coordinates)),
                };
            });

            return res.json(results);
        }
        catch(err){
            console.log("Error in mapFind:", err);
            return res.status(500).json({ message: "Search error" });
        };
    },


    //Query earthquake points for heatmap
    queryEarthquakes: async (req, res) => {
        try {
            const list = await EarthquakeModel.find();
            return res.json(list);
        }
        catch(err){
            console.log("Error in queryEarthquakes:", err);
            return res.status(500).json({ message: "Query error" });
        };
    },



    //Assess risk for a given area
    assessArea: async (req, res) => {
        if(!req.body.bounds){
            return res.status(500).json({ message: "Missing required parameters!" });
        };

        const poly = turf.polygon(req.body.bounds);
        const result = await assessArea(poly); //NOTE: no params yet!

        return res.json(result);
    },
    
};