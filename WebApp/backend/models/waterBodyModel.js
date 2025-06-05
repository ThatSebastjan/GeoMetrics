const mongoose = require("mongoose");
const { polygonSchema } = require("./GeoJsonSchema.js");
const Schema = mongoose.Schema;


const SIRINA_ID_MAP = {
    "3": "2 do 5 m",
    "4": "5 do 10 m",
    "5": "10 do 20 m",
    "6": "20 do 50 m",
    "7": "50 do 100 m",
    "8": "nad 100 m",
};

const STALN_ID_MAP = {
    "3": "občasen",
    "4": "stalen",
};

const TIPTV_ID_MAP = {
    "1": "vodotok",
    "2": "razbremenilni kanal",
    "3": "padavinski jarek",
    "5": "melioracijski jarek",
    "6": "objekt (kanal) za posebno rabo vode",
};


const waterBodySchema = new Schema({
    type: {
        type: String,
        required: true
    },

    id: {
        type: Number,
        required: true,
        unique: true,
    },

    geometry: polygonSchema,

    properties: {
        OBJECTID: Number,
        IME: String,
        VRSTA_IM: String,
        STALN_ID: Number,
        SIRINA_ID: Number,
        TIPTV_ID: Number,
    }
});

waterBodySchema.index({ geometry: "2dsphere" });


waterBodySchema.methods.getWidth = () => {
    return SIRINA_ID_MAP[this.properties.SIRINA_ID];
};

waterBodySchema.methods.getOccurrence = () => {
    return STALN_ID_MAP[this.properties.STALN_ID];
};

waterBodySchema.methods.getType = () => {
    return TIPTV_ID_MAP[this.properties.TIPTV_ID];
};



const WaterBody = mongoose.model("water_bodies", waterBodySchema);
module.exports = WaterBody;