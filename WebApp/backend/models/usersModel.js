const mongoose = require('mongoose');
const bcrypt = require('bcrypt');
const Schema = mongoose.Schema;
const multer = require('multer');
const path = require('path');
const fs = require('fs');

const userSchema = new Schema({
    'username': String,
    'password': String,
    'email': String,
    'registrationDate': Date,
    'profileImage': {
        filename: String,
        path: String,
        contentType: String,
        uploadDate: Date
    }
});

// Password hashing middleware
userSchema.pre('save', async function(next) {
    if (this.isModified('password')) {
        this.password = await bcrypt.hash(this.password, 10);
    }
    next();
});

// Password comparison method
userSchema.methods.comparePassword = async function(candidatePassword) {
    return bcrypt.compare(candidatePassword, this.password);
};


var User = mongoose.model('user', userSchema);
module.exports = User;