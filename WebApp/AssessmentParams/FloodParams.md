# Assigning Scores and Weights for Flood Risk Assessment

To estimate flood risk for a given land area, each relevant parameter is assigned a score based on its impact on flood vulnerability. The parameters are then weighted to reflect their relative importance. Below is a proposed scoring and weighting system.

## Parameter Scores

Each parameter is categorized into levels of risk and assigned a numeric score.

| **Parameter**        | **Category**                      | **Scores** |
|----------------------|-----------------------------------|------------|
| **Flood Type**       | Common / Rare / Rare-catastrophic | 1 / 2 / 3  |
| **Land Use Type**    | Urban / Farmland / Forest         | 1 / 3 / 5  |
| **Slope (Degrees)**  | Flat / Moderate / Steep           | 1 / 2 / 3  |
| **Proximity to Water** | <50m / 50–200m / >200m            | 1 / 2 / 3  |

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


