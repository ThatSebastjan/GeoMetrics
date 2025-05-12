const mongoose = require("mongoose");
const { polygon } = require("./GeoJsonSchema.js");
const Schema = mongoose.Schema;

const RABA_ID_MAP = {
    "1100": "Njiva",
    "1160": "Hmeljišče",
    "1180": "Trajne rastline na njivskih površinah",
    "1190": "Rastlinjak",
    "1211": "Vinograd",
    "1212": "Matičnjak",
    "1221": "Intenzivni sadovnjak",
    "1222": "Ekstenzivni oz. travniški sadovnjak",
    "1230": "Oljčnik",
    "1240": "Ostali trajni nasadi",
    "1300": "Trajni travnik",
    "1321": "Barjanski travnik",
    "1410": "Kmetijsko zemljišče v zaraščanju",
    "1420": "Plantaža gozdnega drevja",
    "1500": "Drevesa in grmičevje",
    "1600": "Neobdelano kmetijsko zemljišče",
    "1800": "Kmetijsko zemljišče, poraslo z gozdnim drevjem",
    "2000": "Gozd",
    "3000": "Pozidano in sorodno zemljišče",
    "4100": "Barje",
    "4210": "Trstičje",
    "4220": "Ostalo zamočvirjeno zemljišče",
    "5000": "Suho odprto zemljišče s posebnim rastlinskim pokrovom",
    "6000": "Odprto zemljišče brez ali z nepomembnim rastlinskim pokrovom",
    "7000": "Voda"
};


const landUseSchema = new Schema({
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
        OBJECTID: Number,
        RABA_PID: Number,
        RABA_ID: Number,
    }
});

landUseSchema.index({ geometry: "2dsphere" });


landUseSchema.methods.getType = () => {
    return RABA_ID_MAP[this.properties.RABA_ID];
};


const LandUse = mongoose.model("land_use", landUseSchema);
module.exports = LandUse;