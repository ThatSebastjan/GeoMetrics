//Utility functions needed across multiple files;
//Stored here to prevent code duplication

import * as turf from "@turf/turf";


export const sleep = (ms) => new Promise(r => setTimeout(r, ms));


//Returns a feature collection of points
export const getHighlightPoints = (feature) => {
    const outerRing = turf.lineString(feature.geometry.coordinates[0]);
    return turf.lineChunk(outerRing, 0.001); //Point every 1m
};