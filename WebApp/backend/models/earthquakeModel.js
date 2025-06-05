const mongoose = require("mongoose");
const { pointSchema } = require("./GeoJsonSchema.js");
const Schema = mongoose.Schema;


const earthquakeSchema = new Schema({
    type: {
        type: String,
        required: true
    },

    id: {
        type: Number,
        required: true,
        unique: true,
    },

    geometry: pointSchema,

    properties: {
        timestamp: Date,
        magnitude: Number,
        depth: Number,
    }
});

earthquakeSchema.index({ geometry: "2dsphere" });


const eqModel = mongoose.model("earthquakes", earthquakeSchema);
module.exports = eqModel;