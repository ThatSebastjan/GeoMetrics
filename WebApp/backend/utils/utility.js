const jwt = require("jsonwebtoken");
const config = require("../config.js");


module.exports = {

    authenticate: (req, res, next) => {
        try {
            const authHeader = req.headers.authorization;

            if (!authHeader) {
                return res.status(401).json({ message: "Authentication required" });
            };

            const decoded = jwt.verify(authHeader, config.sessionSecret);
            req.userId = decoded.userId;
            next();
        } 
        catch (error) {
            //console.log("Auth error:", error);
            return res.status(401).json({ message: "Authentication failed" });
        };
    },

};




