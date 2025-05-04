module.exports = {
	
	
	//HTTP server config
	port: 3001,
	
	
	//DB config
	dbURL: "mongodb://127.0.0.1/TEST",
	
	
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