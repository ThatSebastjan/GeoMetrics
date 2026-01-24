const fs = require("node:fs");
const child_process = require("node:child_process");
const turf = require("@turf/turf");
const { createCanvas, Image } = require("canvas");

const TILE_SIZE = 256; //tile width and height

const DETECT_SCRIPT_PATH = "../../URVRV";


function tile2long(x, zoom) {
	return (x/Math.pow(2,zoom)*360-180);
};


function tile2lat(y, zoom) {
	var n=Math.PI-2*Math.PI*y/Math.pow(2,zoom);
	return (180/Math.PI*Math.atan(0.5*(Math.exp(n)-Math.exp(-n))));
};


function lon2tile(lon, zoom) {
    return (Math.floor((lon+180)/360*Math.pow(2,zoom)));
};


function lat2tile(lat, zoom)  {
    return (Math.floor((1-Math.log(Math.tan(lat*Math.PI/180) + 1/Math.cos(lat*Math.PI/180))/Math.PI)/2 *Math.pow(2,zoom)));
};



function point_to_tile(p, zoom){
    const [lon, lat] = p.geometry.coordinates
    return [lon2tile(lon, zoom), lat2tile(lat, zoom)];
};


function pos_to_pixel(lon, lat, zoom, tile_size) {
    const lat_rad = lat * Math.PI / 180;
    const n = Math.pow(2, zoom);
    const map_size = tile_size * n;

    const worldX = (lon + 180) / 360 * map_size;
    const worldY = (1 - Math.log(Math.tan(lat_rad) + 1/Math.cos(lat_rad)) / Math.PI) / 2 * map_size;

    const tileX = Math.floor(worldX / tile_size);
    const tileY = Math.floor(worldY / tile_size);
    const pixelX = worldX % tile_size;
    const pixelY = worldY % tile_size;

    return { tileX, tileY, pixelX, pixelY, worldX, worldY };
};


function load_img(url){
    return new Promise(async (resolve, reject) => {

        const req = await fetch(url);
        const img_buf = Buffer.from(await req.arrayBuffer());

        const img = new Image();
        img.onload = () => resolve(img);
        img.onerror = err => reject(err);
        img.src = img_buf;
    });
};


//NOTE: dst_radius is in km
async function build_image_on_loc(lon, lat, zoom, dst_radius, out_path){

    const center_point = turf.point([lon, lat]);
    const center_tile = point_to_tile(center_point, zoom);
    console.log(`${center_tile}`);

    //console.log(pos_to_pixel(lon, lat, zoom, 256));

    let min_x = center_tile[0];
    let min_y = center_tile[1];
    let max_x = min_x;
    let max_y = min_y;


    const pixel_offsets = [];


    for(let b = 0; b < 4; b++){
        const bearing = b * 90;
        const dst_point = turf.destination(center_point, dst_radius, bearing);

        const dst_tile = point_to_tile(dst_point, zoom);

        min_x = Math.min(min_x, dst_tile[0]);
        max_x = Math.max(max_x, dst_tile[0]);
        min_y = Math.min(min_y, dst_tile[1]);
        max_y = Math.max(max_y, dst_tile[1]);

        const coords = dst_point.geometry.coordinates;

        pixel_offsets.push({
            tile: dst_tile,
            pos: pos_to_pixel(coords[0], coords[1], zoom, TILE_SIZE),
        });

        //console.log(`${bearing} -> ${dst_tile}`);
    };


    const num_x = max_x - min_x + 1;
    const num_y = max_y - min_y + 1;

    console.log(min_x, min_y, max_x, max_y);
    console.log(num_x, num_y);


    const canvas = createCanvas(num_x * TILE_SIZE, num_y * TILE_SIZE);
    const ctx = canvas.getContext("2d");
    
    for(let y_off = 0; y_off < num_y; y_off++){
        const y_tile = min_y + y_off;

        for(let x_off = 0; x_off < num_x; x_off++){
            const x_tile = min_x + x_off;
            const img = await load_img(`https://tile.openstreetmap.org/${zoom}/${x_tile}/${y_tile}.png`);

            ctx.drawImage(img, x_off * TILE_SIZE, y_off * TILE_SIZE, TILE_SIZE, TILE_SIZE);
        };
    };


    const center_loc = pos_to_pixel(lon, lat, zoom, TILE_SIZE);
    const center_x_off = (center_tile[0] - min_x) * TILE_SIZE;
    const center_y_off = (center_tile[1] - min_y) * TILE_SIZE;

    let bbox = {
        x_min: center_x_off + center_loc.pixelX,
        y_min: center_y_off + center_loc.pixelY,
        x_max: center_x_off + center_loc.pixelX,
        y_max: center_y_off + center_loc.pixelY,
    };


    /*
    ctx.fillStyle = "red";

    ctx.beginPath();
    ctx.arc(center_x_off + center_loc.pixelX, center_y_off + center_loc.pixelY, 5, 0, 2*Math.PI);
    ctx.fill();
    */

    for(let i = 0; i < pixel_offsets.length; i++){
        const po = pixel_offsets[i];

        const p_x_off = (po.tile[0] - min_x) * TILE_SIZE;
        const p_y_off = (po.tile[1] - min_y) * TILE_SIZE;

        bbox.x_min = Math.min(bbox.x_min, p_x_off + po.pos.pixelX);
        bbox.y_min = Math.min(bbox.y_min, p_y_off + po.pos.pixelY);
        bbox.x_max = Math.max(bbox.x_max, p_x_off + po.pos.pixelX);
        bbox.y_max = Math.max(bbox.y_max, p_y_off + po.pos.pixelY);
    };

    const bbox_width = Math.floor(bbox.x_max - bbox.x_min);
    const bbox_height = Math.floor(bbox.y_max - bbox.y_min);

    console.log(bbox_width, bbox_height)


    const cropped_canvas = createCanvas(bbox_width, bbox_height);
    const c_ctx = cropped_canvas.getContext("2d");

    c_ctx.drawImage(canvas, bbox.x_min, bbox.y_min, bbox_width, bbox_height, 0, 0, bbox_width, bbox_height);

    const cropped_img = cropped_canvas.toBuffer();
    fs.writeFileSync(out_path, cropped_img);


    
    ctx.strokeStyle = "blue";
    ctx.strokeRect(bbox.x_min - 1, bbox.y_min - 1, bbox_width + 2, bbox_height + 2);

    const res_img = canvas.toBuffer();
    fs.writeFileSync("./_full_img.png", res_img);
    

    return [bbox_width, bbox_height];
};



function process_image(path){

    try {
        const output = child_process.execSync(`python ${DETECT_SCRIPT_PATH}/detect_water.py --model ${DETECT_SCRIPT_PATH}/checkpoints/best_model.pth --image ${path} --mask_only 1 --output ./results`);
        return true;
    }
    catch(err){
        console.log("error:", err);
        console.log(err.stdout.toString("ascii"));
    };

    return false;
};





async function find_distance(img_path, width, height, dst_radius){
    const img_data_buf = fs.readFileSync("./results/pos_mask.png");

    const img = new Image();

    const wait = new Promise(resolve => {
        img.onload = () => resolve(img);
        img.onerror = err => reject(err);
        img.src = img_data_buf;
    });

    await wait;
    
    const canvas = createCanvas(width, height);
    const ctx = canvas.getContext("2d");

    ctx.drawImage(img, 0, 0);

    const img_data = ctx.getImageData(0, 0, width, height);
    const pixels = img_data.data;

    let center_x = Math.floor(width / 2);
    let center_y = Math.floor(height / 2);
    let best_dst = Infinity;
    let best_x = -1;
    let best_y = -1;

    const dst = (x, y) => {
        const dx = center_x - x;
        const dy = center_y - y;
        return Math.sqrt(dx * dx + dy * dy);
    };

    for(let y = 0; y < img_data.height; y++){
        for(let x = 0; x < img_data.width; x++){
            const px_idx = (y * img_data.width + x) * 4;

            if(pixels[px_idx] < 250){
                continue; //Only check r component others should be the same
            };

            const p_dst = dst(x, y);

            if(p_dst < best_dst){
                best_dst = p_dst;
                best_x = x;
                best_y = y;
            };
        };
    };

    if(best_dst == Infinity){
        return -1;
    };

    const factor_x = (dst_radius * 2) / width;
    const factor_y = (dst_radius * 2) / height;

    console.log("best:", best_x, best_y);

    const dst_x = Math.abs(center_x - best_x) * factor_x;
    const dst_y = Math.abs(center_y - best_y) * factor_y;

    const dst_len = Math.sqrt(dst_x * dst_x + dst_y * dst_y);
    console.log("len:", dst_len);

    return dst_len;
};



module.exports = {

    detect: async (req, res) => {

        if(!req.body || !req.body.longitude || !req.body.latitude){
            return res.status(500).json({ message: "Invalid data!" });
        };

        if(typeof(req.body.longitude) != "number" || typeof(req.body.latitude) != "number"){
            return res.status(500).json({ message: "Invalid data types!" });
        };

        try {

            const [width, height] = await build_image_on_loc(req.body.longitude, req.body.latitude, 15, 0.5, "./tmp/pos.png");

            if(process_image("./tmp/pos.png", width, height) == false){
                console.log("Failed to process image!");
                return;
            };

            const dst = await find_distance("./results/pos_mask.png", width, height, 0.5);

            return res.json({ distance: dst });
        }
        catch(err){
            console.log("Error", err);
            return res.status(500).json({ message: "Internal server error!" });
        };
    }
};
