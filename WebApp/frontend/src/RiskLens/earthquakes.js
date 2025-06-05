import * as turf from "@turf/turf";


const formatDate = (date) => date.toString().split(" ").slice(0, -5).join(" "); //TODO: should probably use a proper library...


//Earthquake heatmap functionality
const addEarthquakeHeatmap = (map, earthquakePointsData, mapboxgl) => {
    
    map.addSource("earthquake_heatmap", { type: "geojson", data: earthquakePointsData });

    map.addLayer({
        id: "earthquake_heatmap",
        type: "heatmap",
        source: "earthquake_heatmap",
        maxzoom: 15,
        layout: {
            visibility: "none",
        },
        paint: {
            // Increase the heatmap weight based on frequency and property magnitude
            "heatmap-weight": [
                "interpolate",
                ["linear"],
                [
                    "*",                     //weight = magnitude * magnitude * (10.0001 - min(sqrt(depth), 100)) 
                    ["^", ["get", "magnitude"], ["get", "magnitude"]], 
                    [
                        "-", 
                        10.001,
                        [
                            "min", 
                            ["sqrt", ["get", "depth"]], 
                            100
                        ]
                    ]
                ],
                
                0, 0,
                500, 0.6,
                1000, 1
            ],

            "heatmap-intensity": [
                "interpolate",
                ["linear"],
                ["zoom"],
                0, 5,
                15, 20
            ],

            "heatmap-color": [
                "interpolate",
                ["linear"],
                ["heatmap-density"],
                0, "rgba(255, 255, 255, 0)",
                0.01, "rgba(255, 255, 255, 0.3)",

                0.2, "hsl(16, 81.70%, 90%)",
                0.4, "hsl(16, 81.70%, 80%)",
                0.6, "hsl(16, 81.70%, 70%)",
                0.8, "hsl(16, 81.70%, 60%)",
                1, "hsl(16, 81.70%, 50%)"
            ],

            "heatmap-radius": [
                "interpolate",
                ["exponential", 2],
                ["zoom"],
                0, 5,
                6, 10,
                8, 20,
                10, 60,
                11, 80,
                12, 140,
                13, 260,
                14, 500,
                15, 980,
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



    //Earthquake points for detail view
    map.addLayer({
        id: "earthquake_points",
        type: "circle",
        source: "earthquake_heatmap", //same source as heatmap
        minzoom: 10,
        layout: {
            visibility: "none",
        },
        paint: {
            "circle-radius": [
                "interpolate",
                ["linear"],
                ["zoom"],
                12,
                ["interpolate", ["linear"], ["get", "magnitude"], 1, 1, 6, 4],
                16,
                ["interpolate", ["linear"], ["get", "magnitude"], 1, 5, 6, 50]
            ],

            "circle-color": [
                "interpolate",
                ["linear"],
                ["get", "magnitude"],
                0, "rgba(255, 255, 255, 0)",
                0.5, "rgba(255, 255, 255, 0.3)",
                1, "hsl(16, 81.70%, 90%)",
                2, "hsl(16, 81.70%, 80%)",
                3, "hsl(16, 81.70%, 70%)",
                4, "hsl(16, 81.70%, 60%)",
                5, "hsl(16, 81.70%, 50%)"
            ],

            "circle-stroke-color": "white",
            "circle-stroke-width": 1,

            "circle-opacity": ["interpolate", ["linear"], ["zoom"], 12, 0, 13, 1]
        }
    });


    //Add earthquake point select
    const eqPopup = new mapboxgl.Popup({
        closeButton: false,
        closeOnClick: false,
        maxWidth: "none",
    });



    let lastFeatureSum = "";

    map.on("mousemove", (e) => {

        //Filter out points within 20px of mouse cursor
        const radius_px = 20;

        const mouseLonLat = map.unproject({x: e.point.x, y: e.point.y});
        const radLonLat = map.unproject({x: e.point.x + radius_px, y: e.point.y});

        const mousePoint = turf.point(mouseLonLat.toArray());
        const radPoint = turf.point(radLonLat.toArray());
        const filterDistance = turf.distance(mousePoint, radPoint); //in km


        //This queries all earthquakes by heatmap radius for some reason so we filter them out by cursor distance...
        const nearPoints = map.queryRenderedFeatures([
            [e.point.x - 5, e.point.y - 5],
            [e.point.x + 5, e.point.y + 5]
        ], 
        {
            target: {layerId: "earthquake_points"}
        })
        .filter(e => {
            const p = turf.point(e.geometry.coordinates);
            const dst = turf.distance(p, mousePoint);
            return dst < filterDistance;
        });


        const featureSum = nearPoints.map(p => p.id).join(",");

        if(featureSum !== lastFeatureSum){
            lastFeatureSum = featureSum;
            eqPopup.remove();
        }
        else {
            return; //No change in near features
        }

        if(nearPoints.length === 0){
            return;
        };


        //Show a popup with all neaerby earthquakes at a midpoint
        let mid_lon = 0;
        let mid_lat = 0;
        
        nearPoints.forEach(p => {
            mid_lon += p.geometry.coordinates[0];
            mid_lat += p.geometry.coordinates[1];
        });

        mid_lon /= nearPoints.length;
        mid_lat /= nearPoints.length;

        //Yea we have to generate html here...
        const pointsHTML = nearPoints
            .reverse()
            .map(p => 
                `<tr>
                    <td>${p.properties.magnitude}</td>
                    <td>${formatDate(new Date(p.properties.timestamp))}</td>
                </tr>`)
            .join("");

        const popupHTML = 
        `<strong>Nearby earthquakes (${nearPoints.length})</strong>
        <br>
        <table class="eq-table">
            <tr>
                <th>Magnitude</th>
                <th>Timestamp</th>
            </tr>
            ${pointsHTML}
        </table>`

        eqPopup.setLngLat([mid_lon, mid_lat]).setHTML(popupHTML).addTo(map);
    });
};


export default addEarthquakeHeatmap;