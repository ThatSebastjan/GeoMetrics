const mongoose = require("mongoose");
const Schema = mongoose.Schema;


const savedLotSchema = new Schema({
    name: {
        type: String,
        required: true,
    },

    address: {
        type: String,
        required: true,
    },

    OBJECTID: {
        type: Number,
        required: true,
    },

    coordinates: {
        type: [Number],
        required: true,
    },

    owner: {
        type: Schema.Types.ObjectId,
        ref: "user"
    }
});


const slModel = mongoose.model("saved_lots", savedLotSchema);
module.exports = slModel;