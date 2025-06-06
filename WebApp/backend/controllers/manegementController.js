const EarthquakeModel = require("../models/earthquakeModel.js");
const FireStationModel = require("../models/fireStationModel.js");
const FloodModel = require("../models/floodModel.js");
const LandLotModel = require("../models/landLotModel.js");
const LandSlideModel = require("../models/landSlideModel.js");
const LandUseModel = require("../models/landUseModel.js");



const modelCount = async (model, req, res) => {
    try {
        const count = await model.countDocuments();
        return res.end(count.toString());
    }
    catch(err){
        console.log("Error in modelCount:", err);
        return res.status(500).json({ message: "Internal server error..." });
    };
};



const modelInsert = async (model, req, res) => {
    console.log("Insert:", req.body);

    const data = req.body;

    try {
        await new model(data).save();
        return res.status(200).end();
    }
    catch(err){
        console.log("Error in modelInsert:", err);
        return res.status(500).end();
    };
};



const modelGet = async (model, req, res) => {
    const offset = parseInt(req.params.offset);
    const count = parseInt(req.params.count);

    if(isNaN(offset) || isNaN(count)){
        return res.status(500).json({ message: "Invalid params!" });
    };

    
    try {
        const list = await (model.find().sort({ id: "descending" }).skip(offset).limit(count));
        return res.json(list);
    }
    catch(err){
        console.log("Error in modelGet:", err);
        return res.status(500).json({ message: "Internal server error..." });
    };
};



module.exports = {

    countEarthquakes: async (req, res) => modelCount(EarthquakeModel, req, res),
    insertEarthquake: async (req, res) => modelInsert(EarthquakeModel, req, res),
    getEarthquakes: async (req, res) => modelGet(EarthquakeModel, req, res),

    countFireStations: async (req, res) => modelCount(FireStationModel, req, res),
    insertFireStation: async (req, res) => modelInsert(FireStationModel, req, res),
    getFireStations: async (req, res) => modelGet(FireStationModel, req, res),

    countFloods: async (req, res) => modelCount(FloodModel, req, res),
    insertFlood: async (req, res) => modelInsert(FloodModel, req, res),
    getFloods: async (req, res) => modelGet(FloodModel, req, res),

    countLandLots: async (req, res) => modelCount(LandLotModel, req, res),
    insertLandLot: async (req, res) => modelInsert(LandLotModel, req, res),
    getLandLots: async (req, res) => modelGet(LandLotModel, req, res),

    countLandSlides: async (req, res) => modelCount(LandSlideModel, req, res),
    insertLandSlide: async (req, res) => modelInsert(LandSlideModel, req, res),
    getLandSlides: async (req, res) => modelGet(LandSlideModel, req, res),

    countLandUse: async (req, res) => modelCount(LandUseModel, req, res),
    insertLandUse: async (req, res) => modelInsert(LandUseModel, req, res),
    getLandUse: async (req, res) => modelGet(LandUseModel, req, res),
    
};