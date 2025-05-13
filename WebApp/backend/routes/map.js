const express = require("express");
const mapController = require("../controllers/mapController");
const router = express.Router();



router.get("/query/:bbox_data", mapController.mapQuery);


module.exports = router;
