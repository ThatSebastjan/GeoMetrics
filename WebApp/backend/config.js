module.exports = {
	
	
	//HTTP server config
	port: 3001,
	
	
	//DB config
	dbURL: "mongodb://mongodb/TEST",
	
	
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
			file: "index.js",
			route: "/",
		},
		{
			file: "users.js",
			route: "/users",
		}
	]
};