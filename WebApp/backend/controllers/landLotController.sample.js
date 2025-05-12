/*
    This is a example usage file
*/

const turf = require("@turf/turf");
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


module.exports = {

    /*
        Controller functions ...
    */

};