const createError 	= require("http-errors");
const express 		= require("express");
const path 			= require("path");
const cookieParser 	= require("cookie-parser");
const logger 		= require("morgan");
const mongoose 		= require("mongoose");
const cors 			= require("cors");
const session 		= require("express-session");
const MongoStore 	= require("connect-mongo");
const isDocker      = require("is-docker"); 


const config = require("./config.js");


const app = express();



//DB connection
const DB_URL = isDocker() ? config.dockerDbURL : config.dbURL;

mongoose.connect(DB_URL);
mongoose.Promise = global.Promise;
mongoose.connection.on("error", console.error.bind(console, "MongoDB connection error:"));



//CORS
app.use(cors({
    credentials: true,
    origin: (origin, callback) => {
        // Allow requests with no origin (mobile apps, curl)
        if (!origin) return callback(null, true);
        if (config.allowedOrigins.indexOf(origin) === -1) {
            const msg = "The CORS policy does not allow access from the specified Origin.";
            return callback(new Error(msg), false);
        }
        return callback(null, true);
    }
}));


app.use(logger("dev"));
app.use(express.json());
app.use(express.urlencoded({ extended: false }));
app.use(cookieParser());
app.use(express.static(path.join(__dirname, "public")));



//Session storage
app.use(session({
    secret: config.sessionSecret,
    resave: true,
    saveUninitialized: false,
    store: MongoStore.create({ mongoUrl: DB_URL })
}));



//Routers
config.routes.forEach(r => {
	app.use(r.route, require(path.resolve(`./routes/${r.file}`)));
});




//Error handler
app.use((err, req, res, next) => {
    
	//Log error
	console.log("Error:", err);

    //Return error
    res.status(err.status || 500);
    res.json(err);
});


module.exports = app;