const express = require("express");
const userController = require("../controllers/usersController");
const router = express.Router();
const jwt = require('jsonwebtoken');
const config = require('../config.js');
const upload = require('../utils/fileUpload');

const authenticate = (req, res, next) => {
  try {
    const authHeader = req.headers.authorization;
    if (!authHeader) {
      return res.status(401).json({ message: 'Authentication required' });
    }

    let token = authHeader;

    const decoded = jwt.verify(token, config.sessionSecret);
    req.userId = decoded.userId;
    next();
  } catch (error) {
    console.log("Auth error:", error);
    return res.status(401).json({ message: 'Authentication failed' });
  }
};


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

module.exports = router;
