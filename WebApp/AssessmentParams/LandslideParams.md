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