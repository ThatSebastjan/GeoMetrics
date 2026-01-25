let blockchain = [];
let pendingData = [];

module.exports = {
    addPendingData: (data) => {
        pendingData.push({
            data: data
        });
    },

    getPendingData: () => {
        return pendingData;
    },

    clearPendingData: () => {
        pendingData = [];
    },

    getBlockchain: () => {
        return blockchain;
    },

    setBlockchain: (newBlockchain) => {
        blockchain = newBlockchain;
    },

    clearBlockchain: () => {
        blockchain = [];
    },

    clearAll: () => {
        blockchain = [];
        pendingData = [];
    }
};

