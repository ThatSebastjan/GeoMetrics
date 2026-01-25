const express = require("express");
const bcController = require("../controllers/blockchainController.js");
const router = express.Router();


router.get("/poll", bcController.poll);
router.post("/sync", bcController.sync);

router.get("/query", bcController.query);
router.post("/insert", bcController.insert);
router.delete("/clear", bcController.clear);


module.exports = router;
