//Utility functions needed across multiple files;
//Stored here to prevent code duplication

import * as turf from "@turf/turf";


export const sleep = (ms) => new Promise(r => setTimeout(r, ms));


//Returns a feature collection of points
export const getHighlightPoints = (feature) => {
    const outerRing = turf.lineString(feature.geometry.coordinates[0]);
    return turf.lineChunk(outerRing, 0.001); //Point every 1m
};


export const defaultGauges = [
    {
        value: null,
        label: "Flood Risk",
        fillGradient: "#e0f2fe 0deg, #7dd3fc 90deg, #38bdf8 180deg, #0284c7 270deg, #0c4a6e 360deg",
        innerColor: "#f8f9fa",
        valueColor: "#2d3748",
        labelColor: "#4a5568"
    },
    {
        value: null,
        label: "Landslide Risk",
        fillGradient: "#F9F5EB 0deg, #E3D5CA 90deg, #D5A021 180deg, #8B5A2B 270deg, #4A3728 360deg",
        innerColor: "#f8fafc",
        valueColor: "#2d3748",
        labelColor: "#4a5568"
    },
    {
        value: null,
        label: "Earthquake Risk",
        fillGradient: "#2f855a 0deg, #48bb78 144deg, #f6e05e 216deg, #ed8936 270deg, #c53030 360deg",
        innerColor: "#f8f9fa",
        valueColor: "#2d3748",
        labelColor: "#4a5568"
    }
];


export const dist2D = (x1, y1, x2, y2) => {
    const dx = x2 - x1;
    const dy = y2 - y1;
    return Math.sqrt(dx * dx + dy * dy);
};


//Set canvas DPI - fix for blurry canvas on high DPI displays
export const setDPI = (canvas, dpi) => {
    // Set up CSS size.
    canvas.style.width = canvas.style.width || canvas.width + "px";
    canvas.style.height = canvas.style.height || canvas.height + "px";

    // Get size information.
    const scaleFactor = dpi / 96;
    const width = parseFloat(canvas.style.width);
    const height = parseFloat(canvas.style.height);

    // Backup the canvas contents.
    const oldScale = canvas.width / width;
    const backupScale = scaleFactor / oldScale;
    const backup = canvas.cloneNode(false);
    backup.getContext("2d").drawImage(canvas, 0, 0);

    // Resize the canvas.
    const ctx = canvas.getContext("2d");
    canvas.width = Math.ceil(width * scaleFactor);
    canvas.height = Math.ceil(height * scaleFactor);

    // Redraw the canvas image and scale future draws.
    ctx.setTransform(backupScale, 0, 0, backupScale, 0, 0);
    ctx.drawImage(backup, 0, 0);
    ctx.setTransform(scaleFactor, 0, 0, scaleFactor, 0, 0);
};


//Backwards geocoding search
export const coordsToAddress = async (lon, lat) => {
    try {
        const req = await fetch(`https://api.mapbox.com/search/geocode/v6/reverse?longitude=${lon}&latitude=${lat}&country=si&access_token=${process.env.REACT_APP_MAPBOX_TOKEN}`);
        const resp = await req.json();
        return resp.features;
    }
    catch(err){
        console.log(`Error in coordsToAddress:`, err);
    };

    return [];
};



//Get approximate address; might return null
export const getFeatureAddress = async (feature) => {
    const midPoint = turf.centerOfMass(turf.polygon(feature.geometry.coordinates));
    const addrResults = await coordsToAddress(...midPoint.geometry.coordinates);
    const approxAddress = addrResults.find(f => f.properties.feature_type == "address")?.properties.full_address;

    return approxAddress;
};