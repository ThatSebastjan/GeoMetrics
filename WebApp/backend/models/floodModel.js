const mongoose = require("mongoose");
const { polygonSchema } = require("./GeoJsonSchema.js");
const Schema = mongoose.Schema;

const FLOOD_TYPE_MAP = {
    "0": "Common",
    "1": "Rare",
    "2": "Rare catastrophic",
};


const floodSchema = new Schema({
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
        FloodType: Number, //Custom property
    }
});

floodSchema.index({ geometry: "2dsphere" });


floodSchema.methods.getType = () => {
    return FLOOD_TYPE_MAP[this.properties.FloodType];
};


module.exports = mongoose.model("floods", floodSchema);