const turf = require("@turf/turf");
const LandLotModel = require("../models/landLotModel.js");
const LandUseModel = require("../models/landUseModel.js");
const WaterBodyModel = require("../models/waterBodyModel.js");
const KoModel = require("../models/koModel.js");
const EarthquakeModel = require("../models/earthquakeModel.js");
const FloodModel = require("../models/floodModel.js");
const LandSlideModel = require("../models/landSlideModel.js");

const { calculateFloodRiskScore, calculateLandslideRiskScore } = require("../utils/riskAssessment.js");



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



/*
    Get water bodies near to a given land lot feature
    If no results are returned, the nearest water body is more than 200m away
*/
const getNearWaterBodies = async (polygon) => {
    const landLotCenter = turf.centerOfMass(polygon);
    let list = [];
    
    try {
        list = await WaterBodyModel.find({
            geometry: {
                $near: {
                    $geometry: landLotCenter.geometry,
                    $maxDistance: 200,
                },
            },
        });

    }
    catch(err){
        console.log("Error in getNearWaterBodies:", err);
        return [];
    };

    return list.map(el => {
        const vertices = turf.explode(turf.polygon(el.geometry.coordinates));
        const closestVertex = turf.nearest(landLotCenter, vertices);

        return {
            feature: el,
            distance: turf.distance(landLotCenter, closestVertex) * 1000, //km -> m 
        };
    });
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



//Get floods in a given polygon
const getFloodsInArea = async (polygon) => {

    try {
        const floods = await FloodModel.find({
            geometry: {
                $geoIntersects: {
                    $geometry: polygon.geometry,
                }
            }
        });

        return floods;
    }
    catch(err){
        console.log("Error in getFloodsInArea:", err);
        return [];
    };
};



//Get Landslides in a given polygon
const getLandSlidesInArea = async (polygon) => {

    try {
        const landSlides = await (LandSlideModel.find({
            geometry: {
                $geoIntersects: {
                    $geometry: polygon.geometry,
                }
            }
        }).sort({ "properties.LandSlideType": "descending"  }).limit(20).maxTimeMS(5000));

        return landSlides;
    }
    catch(err){
        console.log("Error in getLandSlidesInArea:", err);
        return [];
    };
};



//BBOX (array: [minx, miny, maxx, maxy]) to turf.polygon
const makePolygon = (bbox) => turf.polygon([[ [bbox[0], bbox[1]], [bbox[0], bbox[3]], [bbox[2], bbox[3]], [bbox[2], bbox[1]], [bbox[0], bbox[1]] ]]);



//Generate an assessmenet for a particular area with given parameters
const assessArea = async (areaPolygon, floodWeights = {}, landslideWeights = {}) => {

    //Default flood weights
    const floodDefaults = {
        floodType: 40,
        landUse: 30,
        slope: 20,
        proximity: 10,
    };

    //Default landslide weights
    const landslideDefaults = {
        landslideType: 40,
        slope: 30,
        landUse: 25,
        proximity: 5,
    };

    //Check valid properties
    for(const key in floodWeights){
        if(!floodDefaults.hasOwnProperty(key)){
            throw new Error(`Invalid flood wights property: ${key}`);
        };
    };

    for(const key in landslideWeights){
        if(!landslideDefaults.hasOwnProperty(key)){
            throw new Error(`Invalid landslide wights property: ${key}`);
        };
    };

    //Copy over defaults
    floodWeights = Object.assign(Object.assign({}, floodDefaults), floodWeights);
    landslideWeights = Object.assign(Object.assign({}, landslideDefaults), landslideWeights);


    //Calculate stuff
    let landUseCode = 3000; //Default in case of no data (should not happen)
    let floodRisk = 0;
    let landSlideRisk = 0;

    const areaLandUse = await getLandTypeInfo(areaPolygon);

    if(areaLandUse.length == 0){
        console.log("Error, no land use information for provided polygon!");
    }
    else {
        landUseCode = +areaLandUse[0][0]; //Keys are string by default
    };


    const nearWaterBodies = (await getNearWaterBodies(areaPolygon)).map(el => el.distance);


    const floods = await getFloodsInArea(areaPolygon);
    console.log("floods: ", floods);

    for(let i = 0; i < floods.length; i++){

        if(nearWaterBodies.length == 0){
            //NOTE: no data for slope yet, also water proximity is a random value that is greater than 200 which is max used in calculation
            const riskScore = calculateFloodRiskScore(floods[i].properties.FloodType, landUseCode, 0, 500);

            floodRisk = Math.max(riskScore, floodRisk);
        }
        else {
            let maxScore = 0;

            nearWaterBodies.forEach(dst => {
                const riskScore = calculateFloodRiskScore(floods[i].type, landUseCode, 0, dst); //NOTE: no data for slope yet
                maxScore = Math.max(riskScore, maxScore);
            });

            floodRisk = Math.max(maxScore, floodRisk);
        };        
    };



    const landSlides = await getLandSlidesInArea(areaPolygon);
    console.log("landSlides: ", landSlides);

    for(let i = 0; i < landSlides.length; i++){
        if(nearWaterBodies.length == 0){
            //NOTE: no data for slope yet, also water proximity is a random value that is greater than 200 which is max used in calculation
            const riskScore = calculateLandslideRiskScore(landSlides[i].properties.LandSlideType, 0, landUseCode, 500);
            landSlideRisk = Math.max(riskScore, landSlideRisk);
        }
        else {
            let maxScore = 0;

            nearWaterBodies.forEach(dst => {
                const riskScore = calculateLandslideRiskScore(landSlides[i].properties.LandSlideType, 0, landUseCode, dst);
                maxScore = Math.max(riskScore, maxScore);
            });

            landSlideRisk = Math.max(maxScore, landSlideRisk);
        };       
    };


    return {
        floodRisk,
        landSlideRisk
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