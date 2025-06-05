# Assigning Scores and Weights for Landslide Risk Assessment

This document outlines how to estimate landslide risk using a simple point-based scoring system based on hazard probability, slope, land use, and optionally proximity to water.

## Parameter Scores

| **Parameter**          | **Category**                                   | **Scores** |
|------------------------|-------------------------------------------------------|-----------------------|
| **Landslide Severity** | Insignificant / Very low / Low / Medium / High / Very high | 0 / 1 / 2 / 3 / 4 / 5  |
| **Slope (Degrees)**    | Flat / Moderate / Steep / Very steep                  | 1 / 2 / 4 / 5          |
| **Land Use Type**      | Forest / Farmland / Urban / Barren land              | 1 / 3 / 4 / 5          |
| **Proximity to Water** | >200m / 50–200m / <50m (optional factor)             | 1 / 2 / 3              |

- **Landslide Severity**: Based on LAND_SLIDE_TYPE_MAP, this is the core hazard score.
- **Slope**: Steepness increases gravitational stress on soil.
- **Land Use**: Forested areas are more stable; urbanization or deforestation weakens soil.
- **Proximity to Water**: Optional modifier — closer proximity may raise risk due to soil saturation.

---

## Parameter Weights

| **Parameter**      | **Weight (%)** |
|--------------------|----------------|
| Landslide Severity | 40%            |
| Slope              | 30%            |
| Land Use           | 25%            |
| Proximity to Water | 5% *(optional)* |

> Note: If we exclude proximity to water, its 5% needs to be redistributed to other parameters.

---

## 🧮 Final Risk Score Formula

MAX_POINTS =
(5 * 40) + (5 * 30) + (5 * 25) + (3 * 5) = 490

PointsSum =
(LandslideTypeScore * 40) +
(SlopeScore * 30) +
(LandUseScore * 25) +
(ProximityToWaterScore * 5)


FinalScore = (PointsSum / MAX_POINTS) * 100

---

## Pseudo Code

```pseudocode

FUNCTION calculateLandslideRiskScore(landslideType, slopeDegrees, landUseCode, proximityToWaterMeters):

    // ----- LANDSLIDE TYPE SCORE -----
    IF landslideType == 0:
        landslideTypeScore = 0  // Insignificant probability
    ELSE IF landslideType == 1:
        landslideTypeScore = 1  // Very low probability
    ELSE IF landslideType == 2:
        landslideTypeScore = 2  // Low probability
    ELSE IF landslideType == 3:
        landslideTypeScore = 3  // Medium probability
    ELSE IF landslideType == 4:
        landslideTypeScore = 4  // High probability
    ELSE IF landslideType == 5:
        landslideTypeScore = 5  // Very high probability

    // ----- SLOPE SCORE (in degrees) -----
    IF slopeDegrees <= 5:
        slopeScore = 1  // Gentle incline  = low risk
    ELSE IF slopeDegrees <= 15:
        slopeScore = 2
    ELSE IF slopeDegrees <= 25:
        slopeScore = 3
    ELSE IF slopeDegrees <= 35:
        slopeScore = 4
    ELSE:
        slopeScore = 5  // Very steep incline = high risk

    // ----- LAND USE SCORE -----
    IF landUseCode IN [3000]:  // Urban
        landUseScore = 5
    ELSE IF landUseCode IN [1100, 1160, 1300, 1180, 1190, 1211, 1221, 1222]:
        landUseScore = 4  // Exposed agricultural
    ELSE IF landUseCode IN [2000, 1800, 1500]:
        landUseScore = 2  // Forest stabilizes slope
    ELSE IF landUseCode IN [4210, 4100, 4220, 1410]:
        landUseScore = 3  // Transitional or wetlands
    ELSE:
        landUseScore = 1

    // ----- PROXIMITY TO WATER SCORE -----
    IF proximityToWaterMeters <= 50:
        proximityScore = 3
    ELSE IF proximityToWaterMeters <= 200:
        proximityScore = 2
    ELSE:
        proximityScore = 1

    // ----- FINAL CALCULATION -----
    maxPoints = (5 * 40) + (5 * 30) + (5 * 25) + (3 * 5) = 490
    pointsSum = (landslideTypeScore * 40) +
                (slopeScore * 30) +
                (landUseScore * 25) +
                (proximityScore * 5)
    finalScore = (pointsSum / maxPoints) * 100

    RETURN finalScore

```