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
    return res.status(401).json({ message: 'Authentication failed' });
  }
};


router.post("/login", userController.login);
router.post("/register", userController.register);


router.get("/profile/:id", authenticate, userController.getProfile);

router.put("/profile/update", authenticate, userController.updateProfile);

router.get("/check-session", authenticate, async (req, res) => {
  try {
    // Fetch the user from the database using the ID from the token
    const user = await userController.getUserById(req.userId);
    if (!user) {
      return res.status(404).json({ message: "User not found" });
    }
    // Return user info without sensitive data
    return res.status(200).json({
      user: {
        _id: user._id,
        username: user.username,
        email: user.email,
        profileImage: user.profileImage
      }
    });
  } catch (error) {
    console.error("Session check error:", error);
    return res.status(500).json({ message: "Server error" });
  }
});

// Profile picture routes
router.post("/profile/image", authenticate, upload.single('profileImage'), userController.uploadProfileImage);
router.delete("/profile/image", authenticate, userController.deleteProfileImage);

module.exports = router;
