const express = require("express");
const userController = require("../controllers/usersController");
const router = express.Router();
const jwt = require('jsonwebtoken');
const config = require('../config.js');

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

router.get("/check-session", authenticate);


module.exports = router;
