const express = require("express");
const mapController = require("../controllers/mapController.js");
const router = express.Router();



router.get("/query/:bbox_data", mapController.mapQuery);
router.get("/find/:land_lot_id/:ko_id?", mapController.mapFind);

router.get("/earthquakes", mapController.queryEarthquakes);

router.post("/assess", mapController.assessArea);

router.post("/smartSelect", mapController.smartSelect);


module.exports = router;
