//Flood heatmap functionality
const addFloodHeatmap = (map, floodPointsData) => {
    map.addSource("flood_heatmap", { type: "geojson", data: floodPointsData });

    map.addLayer({
        id: "flood_heatmap",
        type: "heatmap",
        source: "flood_heatmap",
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
                1, 0.6
            ],
    
            "heatmap-color": [
                "interpolate",
                ["exponential", 0.2],
                ["heatmap-density"],

                0, "rgba(100, 162, 255, 0)",
                0.2, "rgb(100, 162, 255)",
                0.5, "rgb(66, 142, 255)",
                0.8, "rgb(28, 119, 255)",
                0.9, "rgb(52, 62, 255)",
                2, "rgb(183, 28, 255)",
            ],

            "heatmap-radius": [
                "interpolate",
                ["exponential", 2], //Exponential 2 makes it stable at all zoom levels :D yey
                ["zoom"],
                0, 0.1,
                6, 2,
                8, 5,
                10, 20,
                11, 40,
                12, 80,
                13, 160,
                14, 320,
                15, 640,
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


export default addFloodHeatmap;