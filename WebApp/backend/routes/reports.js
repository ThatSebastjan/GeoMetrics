const express = require("express");
const reportController = require("../controllers/reportController.js");
const router = express.Router();

router.post("/", reportController.report);
router.get("/", reportController.query);
router.get("/blockchain", reportController.queryFromBlockchain);

module.exports = router;
