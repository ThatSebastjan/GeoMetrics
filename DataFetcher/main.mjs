import fs from "node:fs";
import path from "node:path";
import chalk from "chalk";
import { ArcGis } from "./arc_gis_api.mjs";
import config from "./config.mjs";


const arc_gis = new ArcGis(config.api);


const ensure_exists_dir = (path) => {
    !fs.existsSync(path) && fs.mkdirSync(path);
};


const delay = (ms) => new Promise(r => setTimeout(r, ms));



const log_info = (...args) => console.log(chalk.blueBright("[INFO]"), ...args);
const log_succcess = (...args) => console.log(chalk.green("[OK]"), ...args);
const log_error = (...args) => console.log(chalk.red("[ERROR]"), ...args);
const log_warning = (...args) => console.log(chalk.yellowBright("[WARNING]"), ...args);

    
const gen_chunk_bounds = (x_counter, y_counter, bbox, num_x_chunks, num_y_chunks) => {
    const chunk_width = (bbox.xmax - bbox.xmin) / num_x_chunks;
    const chunk_height = (bbox.ymax - bbox.ymin) / num_y_chunks;
    const cx = bbox.xmin + x_counter * chunk_width;
    const cy = bbox.ymin + y_counter * chunk_height;

    return [
        [cx,                cy],
        [cx,                cy + chunk_height],
        [cx + chunk_width,  cy + chunk_height],
        [cx + chunk_width,  cy],
        [cx,                cy],
    ];
};


//Generate child bounds as `count` number of vertical slices
/*
const gen_chunk_subchild_bounds = (x_counter, y_counter, bbox, num_x_chunks, num_y_chunks, count) => {
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
*/


const remove_duplicates = (data_folder_name) => {
    ensure_exists_dir(`./data/${data_folder_name}/unique`);
    log_info("Removing duplicates (overlaps between multiple grid cells)");

    const id_map = {};
    const files = fs.readdirSync(`./data/${data_folder_name}/`).filter(e => path.parse(e).ext == ".json");
    
    for(let i = 0; i < files.length; i++){
        const data = JSON.parse(fs.readFileSync(`./data/${data_folder_name}/${files[i]}`, "utf8"));
        const unique_data = [];

        for(let e_idx = 0; e_idx < data.length; e_idx++){
            const entry = data[e_idx];

            if(!id_map[entry.id]){
                id_map[entry.id] = true;
                unique_data.push(entry);
            };
        };

        fs.writeFileSync(`./data/${data_folder_name}/unique/${files[i]}`, JSON.stringify(unique_data));
        log_info(`Processed ${i + 1} / ${files.length} chunks`);
    };
    
    fs.writeFileSync(`./data/${data_folder_name}/ids.txt`, Object.keys(id_map).join(",\n"));
    log_succcess("Unique id count:", Object.keys(id_map).length);


    //Remove original data and move unique to its place
    files.forEach(f => {
        fs.rmSync(`./data/${data_folder_name}/${f}`);
        fs.renameSync(`./data/${data_folder_name}/unique/${f}`, `./data/${data_folder_name}/${f}`);
    });

    fs.rmdirSync(`./data/${data_folder_name}/unique/`);
};




const land_lot_update = async () => {

    const num_x_chunks = 128;
    const num_y_chunks = 128;

    const land_lot_service = new ArcGis.MapService(arc_gis, "TEMELJNE_VSEBINE", "GH_ZK_KO", config.service_layers.parcele);

    const grab_layer_data = async (x_counter, y_counter) => {

        const test = await land_lot_service.query({
            geometry: {
                rings:[ gen_chunk_bounds(x_counter, y_counter, config.SI_BBOX, num_x_chunks, num_y_chunks) ],
            },
            geometryType: "esriGeometryPolygon",
            outFields: "ST_PARCELE,EID_PARCELA,OBJECTID,KO_ID,POVRSINA,GEOMETRY",
            inSR: ArcGis.WKID.Slovenia,
            outSR: ArcGis.WKID.WGS,
        });

        return test.features;
    };


    ensure_exists_dir("./data/land_lot_data/");
    log_info(`Querying land lot data in ${num_x_chunks * num_y_chunks} chunks`);


    //Grab data in paralel (rows)
    for(let y_counter = 0; y_counter < num_y_chunks; y_counter++){
        
        log_info(`Querying land lot row ${y_counter + 1} / ${num_y_chunks}`);

        const promises = new Array(num_x_chunks).fill().map((e, x_counter) => grab_layer_data(x_counter, y_counter));
        const results = await Promise.all(promises);

        fs.writeFileSync(`./data/land_lot_data/chunk_${y_counter}.json`, JSON.stringify(results.flat()));
        log_succcess(`Grabbed land lot chunk ${y_counter+1}`);
    };


    remove_duplicates("land_lot_data");
};




const flood_areas_update = async () => {

    ensure_exists_dir("./data/flood_data/");


    const query_layer_data = (service) => {
        return service.query({
            geometry: {
                rings:[ gen_chunk_bounds(0, 0, config.SI_BBOX, 1, 1) ],
            },
            geometryType: "esriGeometryPolygon",
            outFields: "OBJECTID",
            inSR: ArcGis.WKID.Slovenia,
            outSR: ArcGis.WKID.WGS,
        });
    };


    let flood_idx = 0; //For fixing indexes (same indexes between different flood types)

    const re_index = (list) => {
        list.forEach(e => {
            e.id = flood_idx;
            e.properties.OBJECTID = flood_idx++;
        });
    };
    


    //Rare flood areas -> Num: ~1700
    log_info("Querying rare flood areas");

    const rare_floods_service = new ArcGis.MapService(arc_gis, "DRSV", "Zun", config.service_layers.obmocje_redkih_poplav);
    const rare_floods = await query_layer_data(rare_floods_service);
    re_index(rare_floods.features);
    
    fs.writeFileSync("./data/flood_data/rare_floods.json", JSON.stringify(rare_floods.features));
    log_succcess(`Rare flood area count: ${rare_floods.features.length}`);


    //Common flood areas -> Num: ~400
    log_info("Querying common flood areas");

    const common_floods_service = new ArcGis.MapService(arc_gis, "DRSV", "Zun", config.service_layers.obmocje_pogostih_poplav);
    const common_floods = await query_layer_data(common_floods_service);
    re_index(common_floods.features);
    
    fs.writeFileSync("./data/flood_data/common_floods.json", JSON.stringify(common_floods.features));
    log_succcess(`Common flood area count: ${common_floods.features.length}`);


    //Very rare, catastrophic flood areas -> Num: ~2200
    log_info("Querying very rare, catastrophic flood areas");

    const rc_floods_service = new ArcGis.MapService(arc_gis, "DRSV", "Zun", config.service_layers.obmocje_zelo_redkih_katastrofalnih_poplav);
    const rc_floods = await query_layer_data(rc_floods_service);
    re_index(rc_floods.features);
    
    fs.writeFileSync("./data/flood_data/rare_catastrophic_floods.json", JSON.stringify(rc_floods.features));
    log_succcess(`Very rare, catastrophic flood area count: ${rc_floods.features.length}`);
};



const land_slides_update = async () => {

    log_warning("NOTE: Land slide data query is slow...");


    const num_x_chunks = 128;
    const num_y_chunks = 128;

    const land_slide_service = new ArcGis.MapService(arc_gis, "DRSV", "Ogrozena_obmocja", config.service_layers.plazljiva_obmocja);

    const grab_layer_data = async (x_counter, y_counter) => {

        const test = await land_slide_service.query({
            geometry: {
                rings:[ gen_chunk_bounds(x_counter, y_counter, config.SI_BBOX, num_x_chunks, num_y_chunks) ],
            },
            geometryType: "esriGeometryPolygon",
            outFields: "OBJECTID,OPIS_GRID",
            inSR: ArcGis.WKID.Slovenia,
            outSR: ArcGis.WKID.WGS,
        });

        return test.features;
    };


    ensure_exists_dir("./data/land_slide_data/");
    log_info(`Querying land slide data in ${num_x_chunks * num_y_chunks} chunks`);


    //Grab data in paralel (rows)
    for(let y_counter = 0; y_counter < num_y_chunks; y_counter++){

        try {
        
            log_info(`Querying land slide row ${y_counter + 1} / ${num_y_chunks}`);

            const promises = new Array(num_x_chunks).fill().map((e, x_counter) => grab_layer_data(x_counter, y_counter));
            const results = await Promise.all(promises);

            fs.writeFileSync(`./data/land_slide_data/chunk_${y_counter}.json`, JSON.stringify(results.flat()));
            log_succcess(`Grabbed land slide data chunk ${y_counter + 1}`);
        }
        catch(err){
            if(err.code == "ETIMEDOUT"){
                log_error(`Query for row ${y_counter + 1} timed out. Retrying`);
                y_counter--;

                await delay(5000);
            };
        };
    };


    remove_duplicates("land_slide_data");


    //Seperate different types based on description
    const files = fs.readdirSync("./data/land_slide_data/").filter(e => e.includes("chunk") && path.parse(e).ext == ".json").map(e => `./data/land_slide_data/${e}`);

    let land_slide_idx = 0;
    const type_map = {};

    const LAND_SLIDE_TYPE_MAP = {
        "zanemarljiva_verjetnost_pojavljanja_plazov": 0,
        "zelo_majhna_verjetnost_pojavljanja_plazov": 1,
        "majhna_verjetnost_pojavljanja_plazov": 2,
        "srednja_verjetnost_pojavljanja_plazov": 3,
        "velika_verjetnost_pojavljanja_plazov": 4,
        "zelo_velika_verjetnost_pojavljanja_plazov": 5,
    };

    for(let i = 0; i < files.length; i++){
        const data = JSON.parse(fs.readFileSync(files[i], "utf8"));

        data.forEach(e => {
            const type = e.properties.OPIS_GRID;

            if(!type_map[type]){
                type_map[type] = [];
            };

            e.id = land_slide_idx; //Re-index
            e.properties.OBJECTID = land_slide_idx++;

            type_map[type].push(e);
        });

        log_info(`Differentiating: ${i + 1} / ${files.length}`);
    };

    Object.keys(type_map).forEach(key => {
        const normalized = key.split(" ").join("_").toLowerCase();
        log_info(`Saving ${normalized}`);

        type_map[key].forEach(el => {
            delete el.properties.OPIS_GRID;
            el.properties.LandSlideType = LAND_SLIDE_TYPE_MAP[normalized];
        });

        fs.writeFileSync(`./data/land_slide_data/${normalized}.json`, JSON.stringify(type_map[key]));
    });

    //Remove chunk files
    files.forEach(f => fs.rmSync(f));
};



const land_use_update = async () => {

    const num_x_chunks = 128;
    const num_y_chunks = 128;

    const gerk_service = new ArcGis.MapService(arc_gis, "TEMELJNE_VSEBINE", "GH_MKGP_GERK_RABA", config.service_layers.raba);

    const grab_layer_data = async (x_counter, y_counter) => {

        const test = await gerk_service.query({
            geometry: {
                rings:[ gen_chunk_bounds(x_counter, y_counter, config.SI_BBOX, num_x_chunks, num_y_chunks) ],
            },
            geometryType: "esriGeometryPolygon",
            outFields: "OBJECTID,RABA_PID,RABA_ID",
            inSR: ArcGis.WKID.Slovenia,
            outSR: ArcGis.WKID.WGS,
        });

        return test.features;
    };


    ensure_exists_dir("./data/land_use_data/");
    log_info(`Querying land use data in ${num_x_chunks * num_y_chunks} chunks`);


    //Grab data in paralel (rows)
    for(let y_counter = 0; y_counter < num_y_chunks; y_counter++){

        try {
            log_info(`Querying land use row ${y_counter + 1} / ${num_y_chunks}`);

            const promises = new Array(num_x_chunks).fill().map((e, x_counter) => grab_layer_data(x_counter, y_counter));
            const results = await Promise.all(promises);

            fs.writeFileSync(`./data/land_use_data/chunk_${y_counter}.json`, JSON.stringify(results.flat()));
            log_succcess(`Grabbed land use chunk ${y_counter+1}`);

        }
        catch(err){
            log_error(`Query for row ${y_counter + 1} failed. Retrying`);
            y_counter--;
            await delay(5000);
        };
    };


    remove_duplicates("land_use_data");
};





const main = async (args) => {

    if(args.length != 1){
        console.log("Invalid usage!");
        console.log("Usage: main.mjs <land_lots | floods | land_slides | land_use>")
    };


    //Check API status
    if(await arc_gis.check_status() == false){
        return log_error("API status error");
    };

    log_succcess("API status ok");


    const update_start = Date.now();

    switch(args[0]){

        case "land_lots":
            await land_lot_update();
        break;

        case "floods":
            await flood_areas_update();
        break;

        case "land_slides":
            await land_slides_update();
        break;

        case "land_use":
            await land_use_update();
        break;

        default:
            return console.log(`Invalid argument: ${args[0]}`);
    };


    const elapsed_time = Math.floor((Date.now() - update_start) / 1000);
    log_info(`Done, update took: ${elapsed_time}s`);
};


main(process.argv.slice(2));



//Generate tmp.json with GeoJSON for chunk visualization

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

        const bounds = gen_chunk_bounds(x_counter, y_counter, config.SI_BBOX_WGS);

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
*/