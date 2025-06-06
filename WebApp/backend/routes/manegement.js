const express = require("express");
const mgmtController = require("../controllers/manegementController.js");
const router = express.Router();



router.get("/earthquakes/count", mgmtController.countEarthquakes);
router.post("/earthquakes/insert", mgmtController.insertEarthquake);
router.get("/earthquakes/:offset/:count", mgmtController.getEarthquakes);

router.get("/fireStations/count", mgmtController.countFireStations);
router.post("/fireStations/insert", mgmtController.insertFireStation);
router.get("/fireStations/:offset/:count", mgmtController.getFireStations);

router.get("/floods/count", mgmtController.countFloods);
router.post("/floods/insert", mgmtController.insertFlood);
router.get("/floods/:offset/:count", mgmtController.getFloods);

router.get("/landLots/count", mgmtController.countLandLots);
router.post("/landLots/insert", mgmtController.insertLandLot);
router.get("/landLots/:offset/:count", mgmtController.getLandLots);

router.get("/landSlides/count", mgmtController.countLandSlides);
router.post("/landSlides/insert", mgmtController.insertLandSlide);
router.get("/landSlides/:offset/:count", mgmtController.getLandSlides);

router.get("/landUse/count", mgmtController.countLandUse);
router.post("/landUse/insert", mgmtController.insertLandUse);
router.get("/landUse/:offset/:count", mgmtController.getLandUse);


module.exports = router;
