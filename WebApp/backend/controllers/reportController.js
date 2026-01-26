const ReportModel = require("../models/disasterReportModel.js");
const blockchainContext = require("../utils/blockchainContext.js");
const mqttService = require("../utils/mqttService.js");

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

            try {
                const blockchainData = JSON.stringify({
                    type: "incident_report",
                    id: obj.id || report._id.toString(),
                    disasterType: obj.type,
                    severity: obj.severity,
                    latitude: obj.latitude,
                    longitude: obj.longitude,
                    timestamp: obj.timestamp,
                    mongoId: report._id.toString()
                });

                blockchainContext.addPendingData(blockchainData);
            } catch (blockchainErr) {
            }

            try {
                mqttService.publishIncident({
                    id: obj.id || report._id.toString(),
                    type: obj.type,
                    severity: obj.severity,
                    latitude: obj.latitude,
                    longitude: obj.longitude,
                    timestamp: obj.timestamp
                });
            } catch (mqttErr) {
            }

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
    },

    queryFromBlockchain: async(req, res) => {
        try {
            const blockchain = blockchainContext.getBlockchain();
            
            const incidents = [];
            if (Array.isArray(blockchain)) {
                blockchain.forEach((block) => {
                    try {
                        if (block.data) {
                            const incidentData = JSON.parse(block.data);
                            if (incidentData.type === "incident_report") {
                                incidents.push({
                                    id: incidentData.id,
                                    type: incidentData.disasterType,
                                    severity: incidentData.severity,
                                    latitude: incidentData.latitude,
                                    longitude: incidentData.longitude,
                                    timestamp: incidentData.timestamp,
                                    mongoId: incidentData.mongoId
                                });
                            }
                        }
                    } catch (e) {
                    }
                });
            }

            return res.json(incidents);
        }
        catch(err){
            console.log("Error in queryFromBlockchain:", err);
            return res.status(500).json({ message: "Internal server error..." });
        };
    }
};