const express = require("express");
const userController = require("../controllers/usersController");
const router = express.Router();
const upload = require('../utils/fileUpload');

const { authenticate } = require("../utils/utility");




router.post("/login", userController.login);
router.post("/register", userController.register);


router.get("/profile/:id", authenticate, userController.getProfile);

router.put("/profile/update", authenticate, userController.updateProfile);

router.get("/check-session", authenticate, userController.checkSession);

// Profile picture routes
router.post("/profile/image", authenticate, upload.single('profileImage'), userController.uploadProfileImage);
router.delete("/profile/image", authenticate, userController.deleteProfileImage);


router.get("/getSavedLots", authenticate, userController.getSavedLots);
router.post("/saveLot", authenticate, userController.saveLot);

router.delete("/savedLots", authenticate, userController.deleteSavedLot);


router.post("/saveReport", authenticate, userController.saveReport);
router.get("/getReports", authenticate, userController.getReports);
router.get("/getReportDetails/:id", authenticate, userController.getReportDetails);
router.delete("/report/:id", authenticate, userController.deleteReport);


module.exports = router;
