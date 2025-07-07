const mongoose = require("mongoose");
const Schema = mongoose.Schema;


const reportSchema = new Schema({
    lotId: {
        type: Number,
        required: true,
    },

    title: {
        type: String,
        required: true,
    },

    address: {
        type: String,
        required: true,
    },

    coordinates: {
        type: [Number],
        required: true,
    },

    summary: {
        type: String,
        required: true,
    },

    results: {
        floodRisk: {
            type: Number,
            required: true,
        },

        landSlideRisk: {
            type: Number,
            required: true,
        },

        earthQuakeRisk: {
            type: Number,
            required: true,
        },
    },

    owner: {
        type: Schema.Types.ObjectId,
        ref: "user"
    },

    date: {
        type: Date,
        default: Date.now,
    },
});


const reportModel = mongoose.model("reports", reportSchema);
module.exports = reportModel;