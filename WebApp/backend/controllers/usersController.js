const UserModel = require('../models/usersModel.js');
const jwt = require('jsonwebtoken');
const config = require('../config.js');
const path = require('path');
const fs = require('fs');
const SavedLotModel = require("../models/savedLotModel.js");
const LandLotModel = require("../models/landLotModel.js");
const ReportModel = require("../models/reportModel.js");
const WaterBodyModel = require("../models/waterBodyModel.js");
const FireStationModel = require("../models/fireStationModel.js");
const turf = require("@turf/turf");

const { getElevation } = require("../utils/elevation.js");
const { getFloodDetails, getLandSlideDetails, getEarthQuakeDetails } = require("../utils/riskDescriptions.js");


const DEFAULT_PROFILE_IMAGE = {
    filename: 'default-avatar.png',
    path: '/default-avatar.png',
};

/**
 * User login function
 * 1. Finds user by email or username
 * 2. Verifies password using the comparePassword method
 * 3. Creates a JWT token for authentication
 */
exports.login = async (req, res) => {
    try {
        const { username, email, password } = req.body;

        // Check if either username or email is provided
        if ((!username && !email) || !password) {
            return res.status(400).json({
                message: 'Username/email and password are required'
            });
        }

        // Find user by username or email
        const searchQuery = username ? { username } : { email };
        const user = await UserModel.findOne(searchQuery);

        // If no user found or password doesn't match
        if (!user) {
            return res.status(401).json({ message: 'Invalid credentials' });
        }

        // Verify password using the method defined in the model
        const isPasswordValid = await user.comparePassword(password);
        if (!isPasswordValid) {
            return res.status(401).json({ message: 'Invalid credentials' });
        }

        // Create JWT token with user information
        const token = jwt.sign(
            { userId: user._id, username: user.username },
            config.sessionSecret,
            { expiresIn: '1h' } // Token expires in 1 hours
        );

        // Return success with token and user info (excluding password)
        res.status(200).json({
            message: 'Login successful',
            token,
            user: {
                id: user._id,
                username: user.username,
                email: user.email,
                registrationDate: user.registrationDate,
                profileImage: user.profileImage
            }
        });
    } catch (error) {
        console.error('Login error:', error);
        res.status(500).json({ message: 'Server error during login' });
    }
};

/**
 * User registration function
 * 1. Checks if user already exists
 * 2. Creates new user record
 * 3. Password is automatically hashed via the middleware
 */
exports.register = async (req, res) => {
    try {
        const { username, email, password } = req.body;

        // Validate required fields
        if (!username || !email || !password) {
            return res.status(400).json({
                message: 'Username, email and password are required'
            });
        }

        // Check if user already exists
        const existingUser = await UserModel.findOne({
            $or: [{ username }, { email }]
        });

        if (existingUser) {
            return res.status(409).json({
                message: 'User with this username or email already exists'
            });
        }

        // Create new user with default profile image
        const newUser = new UserModel({
            username,
            email,
            password,
            registrationDate: new Date(),
            profileImage: DEFAULT_PROFILE_IMAGE
        });

        // Save user (password will be hashed by pre-save middleware)
        await newUser.save();

        res.status(201).json({
            message: 'User registered successfully',
            user: {
                id: newUser._id,
                username: newUser.username,
                email: newUser.email,
                registrationDate: newUser.registrationDate,
                profileImage: newUser.profileImage
            }
        });
    } catch (error) {
        console.error('Registration error:', error);
        res.status(500).json({ message: 'Server error during registration' });
    }
};

// Add a function to get user profile
exports.getProfile = async (req, res) => {
    try {
        // This would typically be protected by authentication middleware
        const userId = req.params.id || req.userId; // from auth middleware
        const user = await UserModel.findById(userId).select('-password');

        if (!user) {
            return res.status(404).json({ message: 'User not found' });
        }

        res.status(200).json({ user });
    } catch (error) {
        console.error('Get profile error:', error);
        res.status(500).json({ message: 'Server error retrieving profile' });
    }
};

/**
 * Update user profile function
 * 1. Validates current password
 * 2. Updates the user profile with new data if password is valid
 * 3. Returns the updated user information
 */
exports.updateProfile = async (req, res) => {
    try {
        // Get user ID from authenticated request
        const userId = req.userId;

        // Get the current user with password
        const user = await UserModel.findById(userId);
        if (!user) {
            return res.status(404).json({ message: 'User not found' });
        }

        // Check if there's a password change attempt
        const isPasswordChangeAttempt = req.body.hasOwnProperty('newPassword');

        // Only require current password if changing password
        if (isPasswordChangeAttempt) {
            const { currentPassword } = req.body;
            if (!currentPassword) {
                return res.status(400).json({ message: 'Current password is required when changing password' });
            }

            // Verify the current password
            const isPasswordValid = await user.comparePassword(currentPassword);
            if (!isPasswordValid) {
                return res.status(401).json({ message: 'Current password is incorrect' });
            }

            // Update password
            user.password = req.body.newPassword;
        }

        // Update user fields directly on the user document
        if (req.body.username) user.username = req.body.username;
        if (req.body.email) user.email = req.body.email;

        // Check for uniqueness if username or email is updated
        if (req.body.username || req.body.email) {
            const query = { _id: { $ne: userId } };

            if (req.body.username) query.username = req.body.username;
            if (req.body.email) query.email = req.body.email;

            const existingUser = await UserModel.findOne(query);

            if (existingUser) {
                return res.status(409).json({
                    message: existingUser.username === req.body.username
                        ? 'Username already taken'
                        : 'Email already in use'
                });
            }
        }

        // Save the user document to trigger password hashing middleware
        await user.save();

        // Return user without password
        const userObject = user.toObject();
        delete userObject.password;

        res.status(200).json({
            message: 'Profile updated successfully',
            user: userObject
        });

    } catch (error) {
        console.error('Update profile error:', error);
        res.status(500).json({ message: 'Server error updating profile' });
    }
};

// profile picture logic
exports.uploadProfileImage = async (req, res) => {
    try {
        // req.file is added by multer middleware
        if (!req.file) {
            return res.status(400).json({ message: 'No image file provided' });
        }

        const userId = req.userId;
        const user = await UserModel.findById(userId);

        if (!user) {
            return res.status(404).json({ message: 'User not found' });
        }

        // If user already has a profile image, delete the old one
        if (user.profileImage && user.profileImage.path) {
            const oldImagePath = path.join(__dirname, '..', user.profileImage.path);
            if (fs.existsSync(oldImagePath)) {
                fs.unlinkSync(oldImagePath);
            }
        }

        // Update user with new profile image information
        user.profileImage = {
            filename: req.file.filename,
            path: `/uploads/profiles/${req.file.filename}`,
            uploadDate: new Date()
        };

        await user.save();

        // Return the updated user without password
        const userObject = user.toObject();
        delete userObject.password;

        res.status(200).json({
            message: 'Profile image uploaded successfully',
            user: userObject
        });
    } catch (error) {
        console.error('Profile image upload error:', error);
        res.status(500).json({ message: 'Server error uploading profile image' });
    }
};

// Delete profile image
exports.deleteProfileImage = async (req, res) => {
    try {
        const userId = req.userId;
        const user = await UserModel.findById(userId);

        if (!user || !user.profileImage) {
            return res.status(404).json({ message: 'User or profile image not found' });
        }

        // Delete the file from storage
        const imagePath = path.join(__dirname, '..', user.profileImage.path);
        if (fs.existsSync(imagePath)) {
            fs.unlinkSync(imagePath);
        }

        // Remove profile image data from user
        user.profileImage = null;
        await user.save();

        const userObject = user.toObject();
        delete userObject.password;

        res.status(200).json({
            message: 'Profile image deleted successfully',
            user: userObject
        });
    } catch (error) {
        console.error('Delete profile image error:', error);
        res.status(500).json({ message: 'Server error deleting profile image' });
    }
};

exports.checkSession = async function (req, res) {
    try {
        // Fetch the user from the database using the ID from the token
        const user = await UserModel.findById(req.userId);
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
};


exports.saveLot = async (req, res) => {
    const b = req.body;

    if(!b.name || !b.OBJECTID || !b.address || !b.coordinates || (b.coordinates.length != 2)){
        return res.status(500).json({ message: "Invalid data!" });
    };

    if((b.name.length > 512) || (b.address.length > 512)){
        return res.status(500).json({ message: "Field too long!" });
    };


    try {
        const existing = await SavedLotModel.exists({ owner: req.userId, OBJECTID: b.OBJECTID });

        if(existing != null){
            return res.status(500).json({ message: "Save with this lot already exists!" });
        };

        const obj = new SavedLotModel({
            name: b.name,
            address: b.address,
            OBJECTID: b.OBJECTID,
            coordinates: b.coordinates,
            owner: req.userId,
        });

        await obj.save();

        return res.json({ message: "ok" });
    }
    catch(err){
        console.log("Error in usersController.saveLot:", err);
        return res.status(500).json({ message: "Error saving" });
    };
};


exports.getSavedLots = async (req, res) => {
    try {
        const savedLots = await SavedLotModel.find({ owner: req.userId });
        return res.json(savedLots);
    }
    catch(err){
        console.log("Erorr in usersController.getSavedLots:", err);
        return res.status(500).json([]);
    };
};


exports.deleteSavedLot = async (req, res) => {

    if(!req.body.OBJECTID){
        return res.status(500).json({ message: "Invalid data!" });
    };

    try {
        await SavedLotModel.deleteOne({ OBJECTID: req.body.OBJECTID, owner: req.userId });

        const list = await SavedLotModel.find({ owner: req.userId });
        return res.json(list);
    }
    catch(err){
        console.log("Erorr in usersController.deleteSavedLot:", err);
        return res.status(500).json([]);
    };
};



//Save assessment report
exports.saveReport = async (req, res) => {
    if(!req.body.name || !req.body.address || !Number.isInteger(req.body.lotId) || !req.body.results){
        return res.status(500).json({ message: "Missing / invalid parameters!" });
    };

    const { lotId, results } = req.body;

    if(!results.hasOwnProperty("floodRisk") || !results.hasOwnProperty("landSlideRisk") || !results.hasOwnProperty("earthQuakeRisk")){
        return res.status(500).json({ message: "Invalid parameters!" });
    };


    try {

        //Does the user already have a saved report for this land lot?
        const existing = await ReportModel.findOne({ owner: req.userId, lotId: lotId });

        if(existing != null){
            return res.status(500).json({ message: `Saved report for this land lot already exists under the name "${existing.title}"` });
        };


        const landLot = await LandLotModel.findOne({ id: lotId });

        if(landLot == null){
            return res.status(500).json({ message: "Invalid lotId!" });
        };

        const lotCenter = turf.centerOfMass(turf.polygon(landLot.geometry.coordinates));


        const reportObj = new ReportModel({
            lotId: lotId,
            title: req.body.name,
            address: req.body.address,
            coordinates: lotCenter.geometry.coordinates,
            summary: "TODO: AI generate summary based on assessment results!",
            results: results,
            owner: req.userId,
        });

        await reportObj.save();
        return res.json({ id: reportObj._id });
    }
    catch(err){
        console.log("Error in saveReport:", err);
        return res.status(500).json({ message: "Internal server error" });
    };
};



exports.getReports = async (req, res) => {
    try {
        const reports = await ReportModel.find({ owner: req.userId });
        return res.json(reports);
    }
    catch(err){
        console.log("Erorr in usersController.getReports:", err);
        return res.status(500).json([]);
    };
};



exports.getReportDetails = async (req, res) => {

    if(!req.params.id){
        return res.status(500).json({ message: "Invalid params" });
    };


    const getNearestFeature = async (point, model, maxDst) => {
        try {
            const nearObjs = await model.find({
                geometry: {
                    $near: {
                        $geometry: point.geometry,
                        $maxDistance: maxDst,
                        $minDistance: 0,
                    }
                }
            });
            
            const objDsts = nearObjs.map(e => {
                const objCenter = (e.geometry.type == "Point") ? turf.point(e.geometry.coordinates) : turf.centerOfMass(turf.polygon(e.geometry.coordinates));
                return { distance: turf.distance(point, objCenter), obj: e };
            }).sort((a, b) => a.distance - b.distance);

            return objDsts[0]; //Nearest object or null
        }
        catch(err){
            console.log("Error in getNearestFeature:", err);
            return null;
        };
    };


    try {

        //NOTE: ownership is not checked here as we allow users to share report URLs 
        const report = await ReportModel.findOne({ _id: req.params.id });

        if(report == null){
            return res.status(500).json({ message: "Invalid report ID" });
        };

        const landLot = await LandLotModel.findOne({ id: report.lotId });

        if(landLot == null){
            return res.status(500).json({ message: "Invalid lotId!" });
        };

        const lotPoly = turf.polygon(landLot.geometry.coordinates);
        const lotArea = turf.area(lotPoly); //In square meters
        const lotCenter = turf.centerOfMass(lotPoly);

        const nearestWB = await getNearestFeature(lotCenter, WaterBodyModel, 5000);
        const nearestFS = await getNearestFeature(lotCenter, FireStationModel, 100000);

        const lotElevation = getElevation(lotCenter.geometry.coordinates[0], lotCenter.geometry.coordinates[1]);

        return res.json({
            id: report._id,
            lotId: landLot.id,
            title: report.title,
            address: report.address,
            date: report.date,
            scores: {
                floodRisk: +report.results.floodRisk.toFixed(2),
                landslideRisk: +report.results.landSlideRisk.toFixed(2),
                earthquakeRisk: +report.results.earthQuakeRisk.toFixed(2)
            },
            coordinates: { lng: report.coordinates[0], lat: report.coordinates[1] },
            summary: report.summary,
            
            details: {
                flood: getFloodDetails(report.results.floodRisk),
                landslide: getLandSlideDetails(report.results.landSlideRisk),
                earthquake: getEarthQuakeDetails(report.results.earthQuakeRisk),
            },
            
            propertyDetails: {
                size: `${Math.round(lotArea)} m²`,
                elevation: `${Math.round(lotElevation)}m above sea level`,
                proximityToWater: (nearestWB != null) ? `${Math.round(nearestWB.distance * 1000)}m to nearest body of water` : "More than 5km to nearest body of water",
                proximityToFirstResponders: (nearestFS != null) ? `${nearestFS.distance.toFixed(2)}km from nearest Fire station` : "More than 100km away from nearest Fire station"
            }
        });
    }
    catch(err){
        console.log("Erorr in usersController.getReportDetails:", err);
        return res.status(500).json({ message: "Internal server error" });
    };
};



exports.deleteReport = async (req, res) => {

    if(!req.params.id){
        return res.status(500).json({ message: "Invalid parameters!" });
    };

    try {
        const report = await ReportModel.findOne({ _id: req.params.id, owner: req.userId });

        if(report == null){
            return res.status(500).json({ message: "Invalid report id!" });
        };

        await ReportModel.deleteOne({ _id: report._id, owner: report.owner });

        return res.json({ message: "OK" });
    }
    catch(err){
        console.log("Erorr in usersController.deleteReport:", err);
        return res.status(500).json({ message: "Internal server error!" });
    };
};