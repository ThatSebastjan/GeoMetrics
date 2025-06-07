module.exports = {
	
	
	//HTTP server config
	port: 3001,
	
	
	//DB config
	dbURL: "mongodb://mongodb:27017/GeoMetricsDB",
	
	
	//Session storage config
	sessionSecret: "a very secret string",
	
	
	//CORS config
	allowedOrigins: [
		"http://localhost:3000",
		"http://localhost:3001",
		"http://89.168.88.70:3000",
		"http://89.168.88.70:3001",
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