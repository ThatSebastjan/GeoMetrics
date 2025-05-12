const mongoose = require("mongoose");
const { polygon } = require("./GeoJsonSchema.js");
const Schema = mongoose.Schema;


const landLotSchema = new mongoose.Schema({
    type: {
        type: String,
        required: true
    },

    id: {
        type: Number,
        required: true,
        unique: true,
    },

    geometry: polygon,

    properties: {
        ST_PARCELE: String,
        EID_PARCELA: String,
        OBJECTID: Number,
        KO_ID: Number,
        POVRSINA: Number,
    }
});

landLotSchema.index({ geometry: "2dsphere" });
landLotSchema.path("properties.ST_PARCELE").index(true);


module.exports = mongoose.model("land_lot", landLotSchema);