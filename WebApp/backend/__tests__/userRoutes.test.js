const mockingoose = require("mockingoose");
const UserModel = require("../models/usersModel");
const userController = require("../controllers/usersController");
const { authenticate } = require("../utils/utility");



const mockUsers = [
    {
        username: "user#0",
        email: "user0@demo.com",
        password: "$2b$10$MB0o3H8qRl704Bjnxx2rgumuTbsuPh3nsi4JjrfTVgZmIVVa2lOn.", //"test"
        id: 0,
    },
    {
        username: "TestUser",
        email: "test@user.com",
        password: "$2b$10$XRYUWB8b0nuTWEDTMXZFM.dD1TMrrf/94x/QLQuPox.fYjO7DgzOa", //"TestPasswd"
        id: 1,
    },
    {
        username: "admin",
        email: "admin@demo.com",
        password: "$2b$10$/jUwOwbWnf6RuKjEaBsY6.HgOVhxy9OSUc98q0LPWlpV3V2uRJmFS", //"admin"
        id: 2
    },
];




//Reset mocked models after each test
afterEach(() => {
  	mockingoose.resetAll();
});



//Registration of a non-existing user
test("valid user register", done => {

    const saveFn = jest.fn(obj => obj);

    mockingoose(UserModel).toReturn(null, "findOne");
    mockingoose(UserModel).toReturn(saveFn, "save");

    const resJson = jest.fn(() => {});
	const resStatus = jest.fn(() => { return { json: resJson } });

    const body = {
        username: "TestUser",
        email: "test@user.com",
        password: "TestPasswd",
    };


    userController.register({ body }, { status: resStatus }).then(() => {
        expect(resStatus.mock.calls).toHaveLength(1); //expect res.status(201)
        expect(resStatus.mock.calls[0][0]).toBe(201);

		expect(saveFn.mock.calls).toHaveLength(1); //Expect UserModel.save call
		expect(saveFn.mock.calls[0][0].password).not.toBe(body.password); //Check if password is hashed on save

		expect(resJson.mock.calls).toHaveLength(1); //Expect res.json call
		const resp = resJson.mock.calls[0][0];

        expect(resp.message).not.toBeNull(); //Expect .message and .user properties
        expect(resp.user).not.toBeNull();

        expect(resp.user.id).not.toBeNull();
        expect(resp.user.username).toBe(body.username);
        expect(resp.user.email).toBe(body.email);
        expect(resp.user.profileImage).not.toBeNull();
        expect(Date.now() - resp.user.registrationDate).toBeLessThan(5000); //Check if timestamp is valid

        done();
    });
});



//Registration with existing user data
test("existing user register", done => {

    const saveFn = jest.fn(obj => obj);

    mockingoose(UserModel).toReturn({ username: "TestUser", email: "demo@test.com" }, "findOne");
    mockingoose(UserModel).toReturn(saveFn, "save");

    const resJson = jest.fn(() => {});
	const resStatus = jest.fn(() => { return { json: resJson } });

    const body = {
        username: "TestUser",
        email: "test@user.com",
        password: "TestPasswd",
    };


    userController.register({ body }, { status: resStatus }).then(() => {
        expect(resStatus.mock.calls).toHaveLength(1); //expect res.status(409)
        expect(resStatus.mock.calls[0][0]).toBe(409);

		expect(saveFn.mock.calls).toHaveLength(0); //Expect no UserModel.save call

		expect(resJson.mock.calls).toHaveLength(1); //Expect res.json call
        expect(resJson.mock.calls[0][0].message).not.toBeNull(); //Expect .message  property

        done();
    });
});



//Login with valid credentials
test("valid user login", done => {

    const findFn = jest.fn(query => mockUsers.find(u => u.username == query._conditions.username));

    mockingoose(UserModel).toReturn(findFn, "findOne");

    const resJson = jest.fn(() => {});
	const resStatus = jest.fn(() => { return { json: resJson } });

    const body = {
        username: "TestUser",
        password: "TestPasswd",
    };


    userController.login({ body }, { status: resStatus }).then(() => {
        expect(findFn.mock.calls).toHaveLength(1); //expect UserModel.findOne call

        expect(resStatus.mock.calls).toHaveLength(1); //expect res.status(200)
        expect(resStatus.mock.calls[0][0]).toBe(200);

        expect(resJson.mock.calls).toHaveLength(1); //expect res.json call
        const resp = resJson.mock.calls[0][0];

        expect(resp.message).not.toBeNull();
        expect(resp.token).not.toBeNull(); //Check JWT token
        expect(resp.user).not.toBeNull();

        done();
    });
});



//Login with invalid credentials
test("invalid user login", done => {

    const findFn = jest.fn(query => mockUsers.find(u => u.username == query._conditions.username));

    mockingoose(UserModel).toReturn(findFn, "findOne");

    const resJson = jest.fn(() => {});
	const resStatus = jest.fn(() => { return { json: resJson } });

    const body = {
        username: "NonExistentUser",
        password: "something123",
    };


    userController.login({ body }, { status: resStatus }).then(() => {
        expect(findFn.mock.calls).toHaveLength(1); //expect UserModel.findOne call

        expect(resStatus.mock.calls).toHaveLength(1); //expect res.status(401)
        expect(resStatus.mock.calls[0][0]).toBe(401);

        expect(resJson.mock.calls).toHaveLength(1); //expect res.json call
        expect(resJson.mock.calls[0][0].message).not.toBeNull();

        done();
    });
});



test("getProfile route", done => {

    const findFn = jest.fn(query => mockUsers.find(u => u.id == query._conditions._id));

    mockingoose(UserModel).toReturn(findFn, "findOne");

    const resJson = jest.fn(() => {});
	const resStatus = jest.fn(() => { return { json: resJson } });


    userController.getProfile({ params: {}, userId: 1 }, { status: resStatus }).then(() => {
        expect(findFn.mock.calls).toHaveLength(1); //expect UserModel.findOne call

        expect(resStatus.mock.calls).toHaveLength(1); //expect res.status(200)
        expect(resStatus.mock.calls[0][0]).toBe(200);

        expect(resJson.mock.calls).toHaveLength(1); //expect res.json call
        expect(resJson.mock.calls[0][0].user).not.toBeNull(); //Check for .user object property

        done();
    });
});



test("valid profile update", done => {

    const findOneFn = jest.fn(query => {
        if(query._conditions.username){
            return null;
        };

        if(query._conditions._id){
            return mockUsers.find(u => u.id == query._conditions._id);
        };
    });

    const saveFn = jest.fn(obj => obj);

    mockingoose(UserModel).toReturn(findOneFn, "findOne");
    mockingoose(UserModel).toReturn(saveFn, "save");

    const resJson = jest.fn(() => {});
	const resStatus = jest.fn(() => { return { json: resJson } });

    const req = {
        userId: 1,
        body: {
            currentPassword: "TestPasswd",
            newPassword: "NewPasswd",
            username: "NewUsername",
            email: "new@demo.com",
        }
    };


    userController.updateProfile(req, { status: resStatus }).then(() => {
        expect(resStatus.mock.calls).toHaveLength(1); //expect res.status(200)
        expect(resStatus.mock.calls[0][0]).toBe(200);

        expect(resJson.mock.calls).toHaveLength(1); //expect res.json call
        const resp = resJson.mock.calls[0][0];

        expect(resp.message).not.toBeNull();
        expect(resp.user).not.toBeNull();

        expect(resp.user.username).toBe(req.body.username); //Check if user info was updated
        expect(resp.user.email).toBe(req.body.email);

        expect(saveFn.mock.calls).toHaveLength(1); //expect UserModel.save call

        done();
    });
});



//Test profile update with already existing data for another user
test("existing user profile update", done => {

    const findOneFn = jest.fn(query => {
        if(query._conditions.username){
            return mockUsers[0]; //Pretend user with same info already exists
        };

        if(query._conditions._id){
            return mockUsers.find(u => u.id == query._conditions._id);
        };
    });

    const saveFn = jest.fn(obj => obj);

    mockingoose(UserModel).toReturn(findOneFn, "findOne");
    mockingoose(UserModel).toReturn(saveFn, "save");

    const resJson = jest.fn(() => {});
	const resStatus = jest.fn(() => { return { json: resJson } });

    const req = {
        userId: 1,
        body: {
            currentPassword: "TestPasswd",
            newPassword: "NewPasswd",
            username: "NewUsername",
            email: "new@demo.com",
        }
    };


    userController.updateProfile(req, { status: resStatus }).then(() => {
        expect(resStatus.mock.calls).toHaveLength(1); //expect res.status(409)
        expect(resStatus.mock.calls[0][0]).toBe(409);

        expect(saveFn.mock.calls).toHaveLength(0); //expect no UserModel.save call

        expect(resJson.mock.calls).toHaveLength(1); //expect res.json call
        expect(resJson.mock.calls[0][0].message).not.toBeNull();
    
        done();
    });
});



//Test profile update with invalid password
test("invalid credentials profile update", done => {

    const findOneFn = jest.fn(query => {
        if(query._conditions.username){
            return null;
        };

        if(query._conditions._id){
            return mockUsers.find(u => u.id == query._conditions._id);
        };
    });

    const saveFn = jest.fn(obj => obj);

    mockingoose(UserModel).toReturn(findOneFn, "findOne");
    mockingoose(UserModel).toReturn(saveFn, "save");

    const resJson = jest.fn(() => {});
	const resStatus = jest.fn(() => { return { json: resJson } });

    const req = {
        userId: 1,
        body: {
            currentPassword: "InvalidPassword1234",
            newPassword: "NewPasswd",
            username: "NewUsername",
            email: "new@demo.com",
        }
    };


    userController.updateProfile(req, { status: resStatus }).then(() => {
        expect(resStatus.mock.calls).toHaveLength(1); //expect res.status(401)
        expect(resStatus.mock.calls[0][0]).toBe(401);

        expect(saveFn.mock.calls).toHaveLength(0); //expect no UserModel.save call

        expect(resJson.mock.calls).toHaveLength(1); //expect res.json call
        expect(resJson.mock.calls[0][0].message).not.toBeNull();
    
        done();
    });
});



//Test authentication middleware
test("user authentication middleware - valid token", done => {

    const findFn = jest.fn(query => mockUsers.find(u => u.username == query._conditions.username));

    mockingoose(UserModel).toReturn(findFn, "findOne");

    const loginResJson = jest.fn(() => {});
	const loginResStatus = jest.fn(() => { return { json: loginResJson } });

    const body = {
        username: "TestUser",
        password: "TestPasswd",
    };


    //Perform user login; login is tested seperately, this is here just to get the JWT token
    userController.login({ body }, { status: loginResStatus }).then(() => {
        const resp = loginResJson.mock.calls[0][0];
        expect(resp.token).not.toBeNull();


        const req = {
            headers:{
                authorization: resp.token
            }
        };

        const resJson = jest.fn(() => {});
	    const resStatus = jest.fn(() => { return { json: resJson } });
        const nextFn = jest.fn(() => {});

        authenticate(req, { status: resStatus }, nextFn);

        expect(resStatus.mock.calls).toHaveLength(0); //expect no res.status or res.json calls
        expect(resJson.mock.calls).toHaveLength(0);

        expect(nextFn.mock.calls).toHaveLength(1); //expect next() handler call

        done();
    });
   
});



//Test authentication middleware with invalid token
test("user authentication middleware - invalid token", done => {

    const req = {
        headers:{
            authorization: "Invalid JWT"
        }
    };

    const resJson = jest.fn(() => {});
    const resStatus = jest.fn(() => { return { json: resJson } });
    const nextFn = jest.fn(() => {});

    authenticate(req, { status: resStatus }, nextFn);

    expect(resStatus.mock.calls).toHaveLength(1); //expect no res.status(401)
    expect(resStatus.mock.calls[0][0]).toBe(401);

    expect(resJson.mock.calls).toHaveLength(1);
    expect(resJson.mock.calls[0][0].message).not.toBeNull(); //expect .message property

    expect(nextFn.mock.calls).toHaveLength(0); //expect no next() handler call

    done();
});
