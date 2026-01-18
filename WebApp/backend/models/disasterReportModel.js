const mongoose = require("mongoose");
const { pointSchema } = require("./GeoJsonSchema.js");
const Schema = mongoose.Schema;


const disasterReportSchema = new Schema({
    type: {
        type: String,
        required: true
    },

    geometry: pointSchema,

    properties: {
        timestamp: {
            type: Date,
            default: Date.now,
        },

        type: {
            type: String,
        },
        
        severity: Number,
    }
});

disasterReportSchema.index({ geometry: "2dsphere" });


const drModel = mongoose.model("disaster_reports", disasterReportSchema);
module.exports = drModel;