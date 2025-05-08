import fs from "node:fs";
import { ArcGis } from "./arc_gis_api.mjs";
import config from "./config.mjs";


const arc_gis = new ArcGis(config.api);



const SI_BBOX = {
    xmin: 372865.61688217556,
    ymin: 32561.166641428135,
    xmax: 621704.3425568037, //x grows west
    ymax: 194254.8275896134, //y grows toward north
};



const SI_BBOX_WGS = { //EPSG:3794 -> EPSG:4326 WGS 84
    xmin: 13.3754608,
    ymin: 45.42143,
    xmax: 16.5966968,
    ymax: 46.8766684
};


const max_items = 2730;



const num_x_chunks = 128;
const num_y_chunks = 128;

    
const gen_chunk_bounds = (x_counter, y_counter, bbox) => {
    const bbox_width = bbox.xmax - bbox.xmin;
    const bbox_height = bbox.ymax - bbox.ymin;
    const chunk_width = bbox_width / num_x_chunks;
    const chunk_height = bbox_height / num_y_chunks;

    const cx = bbox.xmin + x_counter * chunk_width;
    const cy = bbox.ymin + y_counter * chunk_height;

    return [
        [cx, cy],
        [cx, cy + chunk_height],
        [cx + chunk_width, cy + chunk_height],
        [cx + chunk_width, cy],
        [cx, cy],
    ];
};


//Generate child bounds as `count` number of vertical slices
const gen_chunk_subchild_bounds = (x_counter, y_counter, bbox, count) => {
    const bbox_width = bbox.xmax - bbox.xmin;
    const bbox_height = bbox.ymax - bbox.ymin;
    const chunk_width = bbox_width / num_x_chunks;
    const chunk_height = bbox_height / num_y_chunks;

    const cx = bbox.xmin + x_counter * chunk_width;
    const cy = bbox.ymin + y_counter * chunk_height;


    const child_width = chunk_width / count;

    return new Array(count).fill(0).map((e, i) => {
        const child_x = cx + child_width * i;
        return [
            [child_x, cy],
            [child_x, cy + chunk_height],
            [child_x + child_width, cy + chunk_height],
            [child_x + child_width, cy],
            [child_x, cy],
        ];
    });
};




const main = async (args) => {

    //Check status
    if(await arc_gis.check_status() == false){
        return console.log("Ping error");
    };

    console.log("Ping ok");


    const parcel_service = new ArcGis.MapService(arc_gis, "TEMELJNE_VSEBINE", "GH_ZK_KO", 1460);


    
    //Counts saved for 128x128 grid chunks
    const counts_log = fs.readFileSync("./counts.txt", "utf8").split("\n");
    const count_map = {};
    let max_pop = 0;

    counts_log.forEach(e => {
        const [x, y, count] = e.split(":").map(r => parseInt(r));
        count_map[`${x}:${y}`] = count;

        max_pop = Math.max(max_pop, count);
    });

    console.log(max_pop)
    
    /*


    const list = [];
    //const max_pop = 255;

    for(let y_counter = 0; y_counter < num_y_chunks; y_counter++){
        for(let x_counter = 0; x_counter < num_x_chunks; x_counter++){

            const chunk_pop = count_map[`${x_counter}:${y_counter}`];
            //const chunk_pop = 1;
            if(chunk_pop == 0){
                continue;
            };

            const bounds = gen_chunk_bounds(x_counter, y_counter, SI_BBOX_WGS);

            const tmp = {
                "type": "Feature",
                "id": x_counter + y_counter * num_x_chunks,
                "geometry": {
                    "type": "Polygon",
                    "coordinates": [ bounds ],
                },
                "properties": { 
                    x_counter, y_counter,
                    population: chunk_pop,
                    fill: `rgb(${((chunk_pop / max_pop) * 255).toFixed(2)}, 0, 0)`,
                },
            };

            list.push(tmp);
        };
    };


    fs.writeFileSync("tmp.json", JSON.stringify(list, null, 4));
    
    return;

*/



    const grab_layer_data = async (x_counter, y_counter) => {

        const pop = count_map[`${x_counter}:${y_counter}`];

        if(pop == 0){
            return [];
        };


        //Normal query
        if(pop < max_items){
            const test = await parcel_service.query({
                geometry: {
                    rings:[ gen_chunk_bounds(x_counter, y_counter, SI_BBOX) ],
                },
                geometryType: "esriGeometryPolygon",
                outFields: "ST_PARCELE,EID_PARCELA,OBJECTID,POVRSINA,GEOMETRY",
                inSR: ArcGis.WKID.Slovenia,
                outSR: ArcGis.WKID.WGS,
            });

            return test.features;
        };

        //Split into 8 smaller chunks
        const sub_chunks = gen_chunk_subchild_bounds(x_counter, y_counter, SI_BBOX, 8);
        const res = [];

        for(let i = 0; i < sub_chunks.length; i++){

            const sub_res = await parcel_service.query({
                geometry: {
                    rings:[ sub_chunks[i] ],
                },
                geometryType: "esriGeometryPolygon",
                outFields: "ST_PARCELE,EID_PARCELA,OBJECTID,POVRSINA,GEOMETRY",
                inSR: ArcGis.WKID.Slovenia,
                outSR: ArcGis.WKID.WGS,
            });

            const unique = sub_res.features.filter((f, i) => res.findIndex(e => e.id == f.id) == -1);
            res.push(...unique);

            console.log(`Queried sub-chunk ${x_counter}:${y_counter} ${i}/8 -> ${unique.length}`);
        };

        return res;
    };



    //Grab data
    for(let y_counter = 0; y_counter < num_y_chunks; y_counter++){
        const promises = new Array(num_x_chunks).fill().map((e, x_counter) => grab_layer_data(x_counter, y_counter));
        const results = await Promise.all(promises);

        fs.writeFileSync(`./parcel_data/chunk_${y_counter}.json`, JSON.stringify(results.flat()));
        console.log(`Grabbed chunk ${y_counter}`);
    };




    //Query chunk counts for optimization
    /*
    for(let y_counter = 0; y_counter < num_y_chunks; y_counter++){

        const promises = [];

        for(let x_counter = 0; x_counter < num_x_chunks; x_counter++){

            promises.push(new Promise(async res => {

                const test = await parcel_service.query({
                    geometry: {
                        rings:[ gen_chunk_bounds(x_counter, y_counter, SI_BBOX) ],
                    },
                    geometryType: "esriGeometryPolygon",
                    //outFields: "ST_PARCELE,EID_PARCELA,OBJECTID,POVRSINA,GEOMETRY",
                    inSR: ArcGis.WKID.Slovenia,
                    outSR: ArcGis.WKID.WGS,
                    returnCountOnly: true,
                });

                res(`${x_counter}:${y_counter}:${test.count}`);

            }));
        };

        const results = await Promise.all(promises);

        results.forEach(el => {
            fs.appendFileSync("./counts.txt", el + "\n");
        });

        console.log("Completed batch:", y_counter);
    };
    */


/*
    const test = await parcel_service.query({
        geometry: {
            rings:[ gen_chunk_bounds(8, 7, SI_BBOX) ],
        },
        geometryType: "esriGeometryPolygon",
        outFields: "ST_PARCELE,EID_PARCELA,OBJECTID,POVRSINA,GEOMETRY",
        inSR: ArcGis.WKID.Slovenia,
        outSR: ArcGis.WKID.WGS,
    });

    console.log(test.features.length);
    */
};


main(process.argv.slice(2));