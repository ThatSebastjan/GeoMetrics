const blockchainContext = require("../utils/blockchainContext.js");

module.exports = {
    poll: async (req, res) => {
        const pendingData = blockchainContext.getPendingData();
        blockchainContext.clearPendingData();
        res.status(200).json(pendingData);
    },

    sync: async(req, res) => {
        if(!Array.isArray(req.body)){
            console.log("Received invalid blockchain sync data!");
            return res.status(500).end("Invalid data!");
        };

        blockchainContext.setBlockchain(req.body);
        console.log(`Received blockchain with length ${req.body.length}`);
       
        res.status(200).end();
    },

    query: async(req, res) => {
        return res.status(200).json(blockchainContext.getBlockchain());
    },

    insert: async(req, res) => {
        if(!req.body || !req.body.data){
            return res.status(500).json({ message: "Invalid data!" });
        };

        blockchainContext.addPendingData(req.body.data);

        res.status(200).json({ message: "ok" });
    },

    clear: async(req, res) => {
        const blockchain = blockchainContext.getBlockchain();
        const pendingData = blockchainContext.getPendingData();
        const beforeBlockchainLength = blockchain.length;
        const beforePendingLength = pendingData.length;
        
        blockchainContext.clearAll();
        
        console.log(`Blockchain cleared: Removed ${beforeBlockchainLength} blockchain entries and ${beforePendingLength} pending entries`);
        
        return res.status(200).json({ 
            message: "Blockchain cleared successfully",
            removed: {
                blockchain: beforeBlockchainLength,
                pending: beforePendingLength
            }
        });
    }
};