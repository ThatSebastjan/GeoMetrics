const ReportModel = require("../models/disasterReportModel.js");


module.exports = {

    report: async (req, res) => {
        if(!req.body){
            return res.status(500).json({ message: "Invalid data" });
        };

        const obj = req.body;
        const props = ["type", "severity", "latitude", "longitude", "timestamp"];

        if(props.every(p => obj.hasOwnProperty(p)) == false){
            return res.status(500).json({ message: "Missing required properties" });
        };


        try {
            const report = new ReportModel({
                type: "Feature",
                geometry: {
                    type: "Point",
                    coordinates: [obj.longitude, obj.latitude],
                },
                properties: {
                    type: obj.type,
                    severity: obj.severity,
                },
            });

            await report.save();
            return res.status(200).end();
        }
        catch(err){
            console.log("Error in report:", err);
            return res.status(500).json({ message: "Failed to insert report" });
        };
    },


    query: async(req, res) => {
        try {
            const list = await ReportModel.find();
            return res.json(list);
        }
        catch(err){
            console.log("Error in query:", err);
            return res.status(500).json({ message: "Internal server error..." });
        };
    }

};