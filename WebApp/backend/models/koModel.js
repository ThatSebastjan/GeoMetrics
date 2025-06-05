const mongoose = require("mongoose");
const Schema = mongoose.Schema;


const koSchema = new Schema({
    name: {
        type: String,
        required: true,
    },

    id: {
        type: Number,
        required: true,
        unique: true,
    }
});

koSchema.path("id").index(true);


const KO = mongoose.model("ko", koSchema);
module.exports = KO;