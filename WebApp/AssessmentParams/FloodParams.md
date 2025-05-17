# Assigning Scores and Weights for Flood Risk Assessment

To estimate flood risk for a given land area, each relevant parameter is assigned a score based on its impact on flood vulnerability. The parameters are then weighted to reflect their relative importance. Below is a proposed scoring and weighting system.

## Parameter Scores

Each parameter is categorized into levels of risk and assigned a numeric score.

| **Parameter**        | **Category**                      | **Scores** |
|----------------------|-----------------------------------|------------|
| **Flood Type**       | Common / Rare / Rare-catastrophic | 3 / 2 / 1  |
| **Land Use Type**    | Urban / Farmland / Forest         | 5 / 3 / 1  |
| **Slope (Degrees)**  | Flat / Moderate / Steep           | 3 / 2 / 1  |
| **Proximity to Water** | <50m / 50–200m / >200m            | 3 / 2 / 1  |

- **Flood Type**: How frequently floods occur. Common floods increase risk.
- **Land Use**: Determines exposure and vulnerability. Urban areas are most sensitive.
- **Slope**: Flatter land retains water, increasing flood risk.
- **Proximity to Water**: Land near rivers or wetlands is more prone to flooding.

---

## Parameter Weights

Weights reflect how much influence each parameter has on the overall risk score. They should add up to 100%.

| **Parameter**        | **Weight (%)** |
|----------------------|----------------|
| Flood Type           | 40%            |
| Land Use             | 30%            |
| Slope                | 20%            |
| Proximity to Water   | 10%            |

---

## Final Risk Score Formula

Final Risk Score is calculated as:

MAX_POINTS = (3 * 40) + (5 * 30) + (3 * 20) + (3* 10) = 360

PointsSum = (FloodTypeScore * 40) + 
(LandUseScore * 30) + 
(SlopeScore * 20) + 
(ProximityToWaterScore * 10)

FinalScore = PointsSum / MAX_POINTS * 100

---

## Pseudo Code

```pseudocode
FUNCTION calculateFloodRiskScore(floodType, landUseCode, slopeDegrees, proximityToWaterMeters):

    // ----- FLOOD TYPE SCORE -----
    IF floodType == 0:
        floodTypeScore = 3  // Common - most frequent, not necessarily as dangerous, but more likely to occur
    ELSE IF floodType == 1:
        floodTypeScore = 2  // Rare - less frequent, but more dangerous
    ELSE IF floodType == 2:
        floodTypeScore = 1 or 2 not sure  // Rare catastrophic - very rare, but extremely dangerous

    // ----- LAND USE SCORE -----  appologies for the order of the land use codes and scores :^) i reordered it like 5 times :P
    IF landUseCode IN [1211, 1212, 1221, 1222, 1230]:
        landUseScore = 3  // Orchards, olive groves and vineyards
    ELSE IF landUseCode IN [1100, 1160, 1180, 1190, 1240, 1300]: 
        LandUseScore = 4  // Farmland , meadows and pastures
    ELSE IF landUseCode IN [1321, 1410, 1500, 1600, 1800, 2000, 1420]:
        landUseScore = 2  // Transitional or forested
    ELSE IF landUseCode == 3000:
        landUseScore = 5  // Built-up/urban
    ELSE IF landUseCode IN [4100, 4210, 4220, 5000, 6000]:
        landUseScore = 1  // Marsh/wetland
    ELSE:
        landUseScore = 0  // Water (river, lake, etc.)

    // ----- SLOPE SCORE (in degrees) -----
    IF slopeDegrees <= 2:
        slopeScore = 3  // Flat – higher risk of flooding
    ELSE IF slopeDegrees <= 8:
        slopeScore = 2
    ELSE:
        slopeScore = 1  // Steep slope – low flood retention

    // ----- PROXIMITY TO WATER SCORE -----
    IF proximityToWaterMeters <= 50:
        proximityScore = 3  // Very close to large water source --> highest risk
    ELSE IF proximityToWaterMeters <= 200:
        proximityScore = 2
    ELSE:
        proximityScore = 1 

    // ----- FINAL CALCULATION -----
    maxPoints = (3 * 40) + (5 * 30) + (3 * 20) + (3 * 10) = 360
    pointsSum = (floodTypeScore * 40) +
                (landUseScore * 30) +
                (slopeScore * 20) +
                (proximityScore * 10)
    finalScore = (pointsSum / maxPoints) * 100

    RETURN finalScore
    END FUNCTION
    
    ```


