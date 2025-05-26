const mongoose = require("mongoose");
const { pointSchema } = require("./GeoJsonSchema.js");
const Schema = mongoose.Schema;


const fireStationSchema = new Schema({
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
        location: String,
        address: String,
        city: String,
        description: String,
        telephoneNumber: String,
    }
});

fireStationSchema.index({ geometry: "2dsphere" });


const fsModel = mongoose.model("fireStations", fireStationSchema);
module.exports = fsModel;