const express = require("express");
const router = express.Router();
const detectController = require("../controllers/detectController.js");


router.post("/", detectController.detect);


module.exports = router;
