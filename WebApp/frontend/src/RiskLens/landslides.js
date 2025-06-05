
//Landslide heatmap functionality
const addLandslideHeatmap = (map, landslidePointsData) => {

    //Theese two variables are for easy configuration in case of landslide heatmap data swap (the current dataset is kinda large ~22mb)
    const mult = 1; //1;
    const max_score = 144; //144;

    map.addSource("landslide_heatmap", { type: "geojson", data: landslidePointsData });

    map.addLayer({
        id: "landslide_heatmap",
        type: "heatmap",
        source: "landslide_heatmap",
        minzoom: 7,
        maxzoom: 15,
        layout: {
            visibility: "none",
        },
        paint: {
            "heatmap-weight": [
                "interpolate",
                ["linear"],
                ["get", "score"],
                0, 0,
                max_score, 1
            ],
    
            "heatmap-color": [
                "interpolate",
                ["linear"],
                ["heatmap-density"],

                0, "rgba(255, 255, 255, 0)",
                0.1, "rgba(112, 168, 2, 0.2)",
                0.2, "rgba(112, 168, 2, 0.5)",
                0.3, "rgba(112, 168, 2, 0.6)",
                0.35, "rgba(112, 168, 2, 0.65)",
                0.4, "#4c7300", //Zelo majhna stopnja verjetnosti pojavljanja
                0.65, "#98e600", //Majhna stopnja verjetnosti pojavljanja
                0.85, "#ffff00", //Srednja stopnja verjetnosti pojavljanja
                0.95, "#ffaa00", //Velika stopnja verjetnosti pojavljanja
                1, "#ff0000", //Zelo velika stopnja verjetnosti pojavljanja
            ],

            "heatmap-radius": [
                "interpolate",
                ["exponential", 2],
                ["zoom"],
                0, 0,
                7, 1.15 * mult,
                8, 2.32 * mult,
                9, 4.5 * mult,
                10, 9 * mult,
                11, 18 * mult,
                12, 36 * mult,
                13, 72 * mult,
                14, 144 * mult,
                15, 288 * mult,
            ],

            "heatmap-opacity": [
                "interpolate",
                ["linear"],
                ["zoom"],
                8, 1,
                10, 0.9,
                14, 0.8,
                15, 0
            ]
        }
    });
};

export default addLandslideHeatmap;