const mqtt = require("mqtt");
const config = require("../config.js");

let mqttClient = null;

function connectMQTT() {
    if (mqttClient && mqttClient.connected) {
        return mqttClient;
    }

    const brokerUrl = `mqtt://${config.mqttBroker.host}:${config.mqttBroker.port}`;
    
    mqttClient = mqtt.connect(brokerUrl, {
        clientId: `geometrics_backend_${Date.now()}`,
        reconnectPeriod: 5000,
        connectTimeout: 10000
    });

    return mqttClient;
}

function publishIncident(incidentData) {
    try {
        const client = connectMQTT();
        
        if (!client || !client.connected) {
            return;
        }

        const message = JSON.stringify({
            id: incidentData.id,
            type: incidentData.type,
            severity: incidentData.severity,
            latitude: incidentData.latitude,
            longitude: incidentData.longitude,
            timestamp: incidentData.timestamp
        });

        client.publish(config.mqttBroker.topic, message, { qos: 1 });
    } catch (error) {
    }
}

function disconnectMQTT() {
    if (mqttClient && mqttClient.connected) {
        mqttClient.end();
        mqttClient = null;
    }
}

module.exports = {
    connectMQTT,
    publishIncident,
    disconnectMQTT
};

