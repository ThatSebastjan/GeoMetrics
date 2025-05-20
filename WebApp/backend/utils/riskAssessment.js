/**
 * Calculates flood risk score for a land plot.
 * @param {number} floodType - 0: Common, 1: Rare, 2: Rare-catastrophic
 * @param {number} landUseCode - Land use code (e.g., 3000 for urban)
 * @param {number} slopeDegrees - Slope in degrees
 * @param {number} proximityToWaterMeters - Distance to water in meters
 * @returns {number} Final risk score (0-100)
 */
function calculateFloodRiskScore(floodType, landUseCode, slopeDegrees, proximityToWaterMeters) {
    // FLOOD TYPE SCORE
    let floodTypeScore = 0;
    if (floodType === 0) floodTypeScore = 3;
    else if (floodType === 1) floodTypeScore = 2;
    else if (floodType === 2) floodTypeScore = 1; // or 2, but using 1 for highest risk

    // LAND USE SCORE
    let landUseScore = 0;
    if ([1211, 1212, 1221, 1222, 1230].includes(landUseCode)) landUseScore = 3;
    else if ([1100, 1160, 1180, 1190, 1240, 1300].includes(landUseCode)) landUseScore = 4;
    else if ([1321, 1410, 1500, 1600, 1800, 2000, 1420].includes(landUseCode)) landUseScore = 2;
    else if (landUseCode === 3000) landUseScore = 5;
    else if ([4100, 4210, 4220, 5000, 6000].includes(landUseCode)) landUseScore = 1;
    else landUseScore = 0;

    // SLOPE SCORE
    let slopeScore = 0;
    if (slopeDegrees <= 2) slopeScore = 3;
    else if (slopeDegrees <= 8) slopeScore = 2;
    else slopeScore = 1;

    // PROXIMITY TO WATER SCORE
    let proximityScore = 0;
    if (proximityToWaterMeters <= 50) proximityScore = 3;
    else if (proximityToWaterMeters <= 200) proximityScore = 2;
    else proximityScore = 1;

    // FINAL CALCULATION
    const maxPoints = (3 * 40) + (5 * 30) + (3 * 20) + (3 * 10); // 360
    const pointsSum = (floodTypeScore * 40) +
                      (landUseScore * 30) +
                      (slopeScore * 20) +
                      (proximityScore * 10);
    const finalScore = (pointsSum / maxPoints) * 100;

    return finalScore;
}

/**
 * Calculates landslide risk score for a land plot.
 * @param {number} landslideType - 0: Insignificant, 1: Very low, ..., 5: Very high
 * @param {number} slopeDegrees - Slope in degrees
 * @param {number} landUseCode - Land use code (e.g., 3000 for urban)
 * @param {number} proximityToWaterMeters - Distance to water in meters
 * @returns {number} Final risk score (0-100)
 */
function calculateLandslideRiskScore(landslideType, slopeDegrees, landUseCode, proximityToWaterMeters) {
    // LANDSLIDE TYPE SCORE
    let landslideTypeScore = Math.max(0, Math.min(5, landslideType));

    // SLOPE SCORE
    let slopeScore = 1;
    if (slopeDegrees <= 5) slopeScore = 1;
    else if (slopeDegrees <= 15) slopeScore = 2;
    else if (slopeDegrees <= 25) slopeScore = 3;
    else if (slopeDegrees <= 35) slopeScore = 4;
    else slopeScore = 5;

    // LAND USE SCORE
    let landUseScore = 1;
    if ([3000].includes(landUseCode)) landUseScore = 5; // Urban
    else if ([1100, 1160, 1300, 1180, 1190, 1211, 1221, 1222].includes(landUseCode)) landUseScore = 4; // Agricultural
    else if ([2000, 1800, 1500].includes(landUseCode)) landUseScore = 2; // Forest
    else if ([4210, 4100, 4220, 1410].includes(landUseCode)) landUseScore = 3; // Transitional/wetlands

    // PROXIMITY TO WATER SCORE
    let proximityScore = 1;
    if (proximityToWaterMeters <= 50) proximityScore = 3;
    else if (proximityToWaterMeters <= 200) proximityScore = 2;

    // FINAL CALCULATION
    const maxPoints = (5 * 40) + (5 * 30) + (5 * 25) + (3 * 5); // 490
    const pointsSum = (landslideTypeScore * 40) +
                      (slopeScore * 30) +
                      (landUseScore * 25) +
                      (proximityScore * 5);
    const finalScore = (pointsSum / maxPoints) * 100;

    return finalScore;
}

module.exports = {
    calculateFloodRiskScore,
    calculateLandslideRiskScore,
};