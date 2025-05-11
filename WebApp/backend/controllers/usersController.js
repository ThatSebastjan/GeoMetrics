const UserModel = require('../models/usersModel.js');
const jwt = require('jsonwebtoken');
const config = require('../config.js');
const path = require('path');
const fs = require('fs');

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
}