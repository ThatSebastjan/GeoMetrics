const turf = require("@turf/turf");
const LandLotModel = require("../models/landLotModel.js");
const LandUseModel = require("../models/landUseModel.js");



/*
    Get land type from land use intersection.
    Sorted by intersection area
    Returns array of elements -> [land use type, area fraction]
*/
const getLandTypeInfo = async (land_lot) => {
    const landLotArea = turf.area(land_lot.geometry);

    const landUse = await LandUseModel.find({
        geometry: {
            $geoIntersects: {
                $geometry: land_lot.geometry
            }
        }
    });

    const landLotPolygon = turf.polygon(land_lot.geometry.coordinates);
    const typeArea = {};

    landUse.forEach(l => {
        const intersection = turf.intersect(turf.featureCollection([landLotPolygon, turf.polygon(l.geometry.coordinates)]));

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

    const lots = await LandLotModel.find({
        geometry: {
            $geoIntersects: {
                $geometry: polygon.geometry,
            }
        }
    });

    return lots;
};



//BBOX (array: [minx, miny, maxx, maxy]) to turf.polygon
const make_polygon = (bbox) => turf.polygon([[ [bbox[0], bbox[1]], [bbox[0], bbox[3]], [bbox[2], bbox[3]], [bbox[2], bbox[1]], [bbox[0], bbox[1]] ]]);




module.exports = {


    //Get map features in a given bounding box
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
        const bbox_polygon = make_polygon(bbox);
        const prev_bbox_polygon = make_polygon(prev_bbox);

        const difference = turf.difference(turf.featureCollection([bbox_polygon, prev_bbox_polygon]));

        if(difference == null){
            return res.status(304).end();
        };

        const differenceArea = turf.area(difference.geometry);

        const t1 = Date.now();
        const landLots = await getLandLotsInArea(difference);
        const t_elapsed = Date.now() - t1;

        res.json({
            time: t_elapsed,
            data: landLots,
            area: differenceArea,
        });
    },
    
};