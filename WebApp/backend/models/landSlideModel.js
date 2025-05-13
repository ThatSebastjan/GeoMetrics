const mongoose = require("mongoose");
const { polygonSchema } = require("./GeoJsonSchema.js");
const Schema = mongoose.Schema;

const LAND_SLIDE_TYPE_MAP = {
    "0": "Insignificant probability",
    "1": "Very low probability",
    "2": "Low probability",
    "3": "Medium probability",
    "4": "High probability",
    "5": "Very high probability",
};


const landSlideSchema = new Schema({
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
        LandSlideType: Number, //Custom property
    }
});

landSlideSchema.index({ geometry: "2dsphere" });


landUseSchema.methods.getType = () => {
    return LAND_SLIDE_TYPE_MAP[this.properties.LandSlideType];
};



module.exports = mongoose.model("land_slide", landSlideSchema);