//This file contains risk descriptions based on level for each type of risk.
//Yes, I AI generated the descriptions :c

const floodDescriptions = [
    {
        "from": 0,
        "to": 5,
        "description": "Very Low Risk - Flooding is unlikely to occur in this area."
    },
    {
        "from": 5,
        "to": 15,
        "description": "Low Risk - Flooding is possible but unlikely, with minimal impact on properties and infrastructure."
    },
    {
        "from": 15,
        "to": 25,
        "description": "Moderate Risk - Flooding is possible and may impact properties and infrastructure, with some disruption to daily life."
    },
    {
        "from": 25,
        "to": 35,
        "description": "High Risk - Flooding is likely to occur, with significant impact on properties and infrastructure, and potential disruption to daily life."
    },
    {
        "from": 35,
        "to": 45,
        "description": "Very High Risk - Flooding is highly likely to occur, with severe impact on properties and infrastructure, and significant disruption to daily life."
    },
    {
        "from": 45,
        "to": 55,
        "description": "Extremely High Risk - Flooding is almost certain to occur, with catastrophic impact on properties and infrastructure, and severe disruption to daily life."
    },
    {
        "from": 55,
        "to": 65,
        "description": "Catastrophic Risk - Flooding is highly probable and will have a devastating impact on properties, infrastructure, and daily life."
    },
    {
        "from": 65,
        "to": 75,
        "description": "Severe Risk - Flooding is almost inevitable, with a high likelihood of catastrophic damage to properties, infrastructure, and daily life."
    },
    {
        "from": 75,
        "to": 85,
        "description": "Extremely Severe Risk - Flooding is imminent, with a high likelihood of widespread devastation to properties, infrastructure, and daily life."
    },
    {
        "from": 85,
        "to": 95,
        "description": "Catastrophic Risk (High Probability) - Flooding is highly likely to occur, with a high probability of catastrophic damage to properties, infrastructure, and daily life."
    },
    {
        "from": 95,
        "to": 100,
        "description": "Certain Catastrophe - Flooding is almost certain to occur, with a high likelihood of widespread devastation to properties, infrastructure, and daily life."
    }
];


const landSlideDescriptions = [
    {
        "from": 0,
        "to": 5,
        "description": "Stable Terrain - The area is considered geologically stable, with minimal risk of landslides."
    },
    {
        "from": 5,
        "to": 15,
        "description": "Steep Slope Risk - The area has steep slopes, increasing the risk of landslides, but they are still unlikely to occur."
    },
    {
        "from": 15,
        "to": 25,
        "description": "Slope Instability Risk - The area's slopes are unstable, and landslides are possible, but not likely to occur frequently."
    },
    {
        "from": 25,
        "to": 35,
        "description": "Landslide Prone Area - The area is prone to landslides, which can occur frequently, causing damage to properties and infrastructure."
    },
    {
        "from": 35,
        "to": 45,
        "description": "High Slope Instability Risk - The area's slopes are highly unstable, increasing the likelihood of landslides, which can be catastrophic."
    },
    {
        "from": 45,
        "to": 55,
        "description": "Extreme Slope Instability Risk - Landslides are highly probable, and the area is at high risk of widespread devastation."
    },
    {
        "from": 55,
        "to": 65,
        "description": "Catastrophic Landslide Risk - The area is highly susceptible to catastrophic landslides, which can cause widespread destruction."
    },
    {
        "from": 65,
        "to": 75,
        "description": "Imminent Landslide Risk - Landslides are imminent, and the area is at high risk of catastrophic damage."
    },
    {
        "from": 75,
        "to": 85,
        "description": "Widespread Landslide Risk - The area is highly prone to widespread landslides, which can cause catastrophic damage to properties and infrastructure."
    },
    {
        "from": 85,
        "to": 95,
        "description": "Severe Landslide Threat - Landslides are highly likely to occur, and the area is at severe risk of widespread devastation."
    },
    {
        "from": 95,
        "to": 100,
        "description": "Catastrophic Landslide Event - Landslides are almost certain to occur, and the area is at high risk of a catastrophic landslide event."
    }
];


const earthQuakeDescriptions = [
    {
        "from": 0,
        "to": 5,
        "description": "Seismic Stability - The area is considered seismically stable, with minimal risk of earthquakes."
    },
    {
        "from": 5,
        "to": 15,
        "description": "Low Seismic Activity - The area has low seismic activity, but earthquakes can still occur, causing minor damage."
    },
    {
        "from": 15,
        "to": 25,
        "description": "Moderate Seismic Risk - The area has moderate seismic activity, and earthquakes can cause moderate damage to properties and infrastructure."
    },
    {
        "from": 25,
        "to": 35,
        "description": "Elevated Seismic Risk - The area is at elevated risk of earthquakes, which can cause significant damage to properties and infrastructure."
    },
    {
        "from": 35,
        "to": 45,
        "description": "High Seismic Hazard - The area is at high risk of strong earthquakes, which can cause widespread damage and disruption."
    },
    {
        "from": 45,
        "to": 55,
        "description": "Very High Seismic Hazard - The area is at very high risk of destructive earthquakes, which can cause catastrophic damage to properties and infrastructure."
    },
    {
        "from": 55,
        "to": 65,
        "description": "Extreme Seismic Hazard - The area is at extreme risk of massive earthquakes, which can cause widespread devastation and loss of life."
    },
    {
        "from": 65,
        "to": 75,
        "description": "Catastrophic Seismic Risk - The area is at high risk of a catastrophic earthquake event, which can cause widespread destruction and loss of life."
    },
    {
        "from": 75,
        "to": 85,
        "description": "Severe Seismic Threat - The area is at severe risk of a devastating earthquake, which can cause widespread damage and disruption."
    },
    {
        "from": 85,
        "to": 95,
        "description": "Imminent Seismic Disaster - The area is at high risk of a massive earthquake, which can cause catastrophic damage and loss of life."
    },
    {
        "from": 95,
        "to": 100,
        "description": "Catastrophic Seismic Event - The area is at extremely high risk of a catastrophic earthquake event, which can cause widespread devastation and loss of life."
    }
];


const getDescription = (percentage, list) => {
    percentage = Math.min(percentage, 99); //Upper bound is exclusive so 100 would fall out of bounds
    return list.find(el => (el.from <= percentage) && (el.to > percentage))?.description || "Something went wrong...";
};


module.exports = {
    getFloodDetails: (percentage) => getDescription(percentage, floodDescriptions),
    getLandSlideDetails: (percentage) => getDescription(percentage, landSlideDescriptions),
    getEarthQuakeDetails: (percentage) => getDescription(percentage, earthQuakeDescriptions),
};