const mockingoose = require("mockingoose");
const EarthquakeModel = require("../models/earthquakeModel");
const LandLotModel = require("../models/landLotModel");

const KoModel = require("../models/koModel.js");
mockingoose(KoModel).toReturn([{ id: 2121, name: "Test KO" }], "find"); //Mock global init routine that would fail otherwise


//Required after KoModel mock as it is called in a global initializer and would cause errors otherwise
const mapController = require("../controllers/mapController");



//Reset mocked models; KoModel mock is only needed once on init so it can be reset
afterEach(() => {
  	mockingoose.resetAll();
});



//Sample lot bounds used in tests
const mockLotBounds = [[ [14.409875,46.139479], [14.409623,46.139597], [14.409328,46.139271], [14.409578,46.139154], [14.409875,46.139479] ]];

const mockEarthQuake = {
	geometry: { coordinates: [14.409875, 46.139479] },
	properties: { magnitude: 6.9, depth: 0 }
};

const mockLandLots = [{
	properties: { ST_PARCELE: 123456, KO_ID: 2121 },
	geometry: { coordinates:  mockLotBounds }
},
{
	properties: { ST_PARCELE: 666, KO_ID: 324 },
	geometry: { coordinates:  mockLotBounds }
}];



test("mapQuery route test", (done) => {

	mockingoose(LandLotModel).toReturn(mockLandLots, "find");

	const resJson = jest.fn(() => {});
	const resStatus = jest.fn(() => { return { json: () => {} } });

	const params = {
		bbox_data: [0, 0, 0, 0,  14.411956, 46.138442, 14.407711, 46.140335].join(","),
	};

	
	mapController.mapQuery({ params }, { status: resStatus, json: resJson }).then(() => {
		expect(resStatus.mock.calls).toHaveLength(0); //Expect no res.status calls

		expect(resJson.mock.calls).toHaveLength(1); //Expect res.json call
		const data = resJson.mock.calls[0][0].data;

		expect(data).not.toBeNull();
		expect(data.length).toBe(mockLandLots.length); //Returns all ignoring the view bbox as filtering is done via intersect query which we mock

		data.forEach((el, idx) => {
			expect(el.properties.ST_PARCELE == mockLandLots[idx].properties.ST_PARCELE);
			expect(el.properties.KO_ID == mockLandLots[idx].properties.KO_ID);
		});

		done();
	});
});



test("mapFind route test", (done) => {

	mockingoose(LandLotModel).toReturn(mockLandLots, "find");

	const resJson = jest.fn(() => {});
	const resStatus = jest.fn(() => { return { json: () => {} } });

	const params = {
		land_lot_id: 123456,
		ko_id: 2121,
	};

	
	mapController.mapFind({ params }, { status: resStatus, json: resJson }).then(() => {
		expect(resStatus.mock.calls).toHaveLength(0); //Expect no res.status calls

		expect(resJson.mock.calls).toHaveLength(1); //Expect res.json call
		const data = resJson.mock.calls[0][0];

		expect(data).not.toBeNull();
		expect(data.length).toBe(1);
		expect(data[0].ko_id).toBe(2121);
		expect(data[0].ko_name).toBe("Test KO");
		expect(data[0].bbox).not.toBeNull();

		done();
	});
});



test("queryEarthquakes route test", (done) => {
	
	mockingoose(EarthquakeModel).toReturn([mockEarthQuake], "find");

	const resJson = jest.fn(() => {});
	const resStatus = jest.fn(() => { return { json: () => {} } });

	
	mapController.queryEarthquakes({}, { status: resStatus, json: resJson }).then(() => {
		expect(resStatus.mock.calls).toHaveLength(0); //Expect no res.status calls

		expect(resJson.mock.calls).toHaveLength(1); //Expect res.json call
		const data = resJson.mock.calls[0][0];

		expect(data).not.toBeNull();
		expect(data.length).toBe(1);

		done();
	});
});



test("assessArea route test", (done) => {

	mockingoose(EarthquakeModel).toReturn([mockEarthQuake], "find");

	const resJson = jest.fn(() => {});
	const resStatus = jest.fn(() => { return { json: () => {} } });
	
	const body = {
		bounds: mockLotBounds
	};


	mapController.assessArea({ body }, { status: resStatus, json: resJson }).then(() => {
		expect(resStatus.mock.calls).toHaveLength(0); //Expect no res.status calls

		expect(resJson.mock.calls).toHaveLength(1); //Expect res.json call
		const data = resJson.mock.calls[0][0];

		//Test if risk values are as expected
		expect(data.results).not.toBeNull();
		expect(data.results.floodRisk).toBeCloseTo(88.764);
		expect(data.results.landSlideRisk).toBe(0);
		expect(data.results.earthQuakeRisk).toBeCloseTo(47.553);

		//Test only if description is provided
		expect(data.details).not.toBeNull();
		expect(data.details.flood).not.toBeNull();
		expect(data.details.landSlide).not.toBeNull();
		expect(data.details.earthQuake).not.toBeNull();

		done();
	});
});



test("smartSelect route test", (done) => {

	mockingoose(LandLotModel).toReturn(mockLandLots, "find");
	mockingoose(EarthquakeModel).toReturn([mockEarthQuake], "find");

	const failingJsonFn = jest.fn(() => {});
	const failingStatusFn = jest.fn(() => { return { json: failingJsonFn } });

	const invalidBody = { 
		bounds: [], 
		filter: 123
	};
	

	//First test with invalid params
	mapController.smartSelect({ body: invalidBody }, { status: failingStatusFn }).then(() => {
		expect(failingStatusFn.mock.calls).toHaveLength(1); //Expect res.status call
		expect(failingStatusFn.mock.calls[0][0]).toBe(500);

		expect(failingJsonFn.mock.calls).toHaveLength(1); //Expect res.status call
		expect(failingJsonFn.mock.calls[0][0].message).not.toBe(null); //Response object must have a message property


		//Test succeeding call
		const jsonFn = jest.fn(() => {});

		const body = {
			bounds: [14.411956, 46.138442, 14.407711, 46.140335],
			filter: { 
				mode: 0,
				top: 5
			}
		};

		mapController.smartSelect({ body }, { json: jsonFn }).then(() => {

			expect(jsonFn.mock.calls).toHaveLength(1); //Expect res.json call

			//Check response object data
			const data = jsonFn.mock.calls[0][0];
			expect(data.length).toBe(1); //One lot in response (top 5% of 2 lots -> can only be 1 lot)

			const resp = data[0];
			expect(resp).not.toBeNull();
			expect(resp.result).not.toBeNull();
			expect(resp.lot).not.toBeNull();
			expect(resp.details).not.toBeNull();

			//Test if risk values are as expected
			expect(resp.result.floodRisk).toBeCloseTo(88.764);
			expect(resp.result.landSlideRisk).toBe(0);
			expect(resp.result.earthQuakeRisk).toBeCloseTo(47.553);

			//Test only if description is provided
			expect(resp.details).not.toBeNull();
			expect(resp.details.flood).not.toBeNull();
			expect(resp.details.landSlide).not.toBeNull();
			expect(resp.details.earthQuake).not.toBeNull();

			//Test lot properties
			expect(resp.lot.geometry).not.toBeNull();
			expect(resp.lot.properties).not.toBeNull();

			done();
		});
	});
}, 10000);