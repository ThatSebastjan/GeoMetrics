let blockchain = [];
let pendingData = []; // [{data: "str data"}, ...]


/*
//Generate test data
setInterval(() => {
    pendingData.push({
        data: `Test blockchain entry#${Date.now().toString(16)}`,
    });

    console.log(`Inserted data; queue size: ${pendingData.length}`);
}, 500);
*/



module.exports = {

    /*
        Endpoints accesses by the blockchain master procecss (rank 0)
    */

    poll: async (req, res) => {
        res.status(200).json(pendingData);
        pendingData = [];
    },


    sync: async(req, res) => {

        if(!Array.isArray(req.body)){
            console.log("Received invalid blockchain sync data!");
            return res.status(500).end("Invalid data!");
        };

        blockchain = req.body;
        console.log(`Received blockchain with length ${blockchain.length}`);
       
        res.status(200).end();
    },



    /*
        Endpoints for inserting new data into the blockchain processing queue / retrieving blockchain data
    */

    query: async(req, res) => {
        return res.status(200).json(blockchain);
    },


    insert: async(req, res) => {
        if(!req.body || !req.body.data){
            return res.status(500).json({ message: "Invalid data!" });
        };

        pendingData.push({
            data: req.body.data,
        });

        res.status(200).json({ message: "ok" });
    }

    
};