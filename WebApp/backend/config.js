module.exports = {
	
	
	//HTTP server config
	port: 3001,
	
	
	//DB config
	dbURL: "mongodb://localhost:27017/GeoMetricsDB",
	
	
	//Session storage config
	sessionSecret: "a very secret string",
	
	
	//CORS config
	allowedOrigins: [
		"http://localhost:3000",
		"http://localhost:3001"
	],
	
	
	//Routers and API paths
	routes: [
		{
			file: "users.js",
			route: "/users",
		},
		{
			file: "map.js",
			route: "/map",
		},
		{
			file: "manegement.js",
			route: "/manegement",
		},
	]
};