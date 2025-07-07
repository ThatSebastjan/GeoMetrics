const fs = require("fs");

const buf = fs.readFileSync(`${__dirname}/height_map.bin`);



const SI_BBOX_WGS = { //EPSG:3794 -> EPSG:4326 WGS 84
	xmin: 13.3754608,
	ymin: 45.42143,
	xmax: 16.5966968,
	ymax: 46.8766684
};

const BBOX_WIDTH = SI_BBOX_WGS.xmax - SI_BBOX_WGS.xmin;
const BBOX_HEIGHT = SI_BBOX_WGS.ymax - SI_BBOX_WGS.ymin;

const DATA_WIDTH = 2187;
const DATA_HEIGHT = 1407;


const getElevationXY = (x, y) => {
	
	if((x < 0) || (x >= DATA_WIDTH) || (y < 0) || (y >= DATA_HEIGHT)){
		return null;
	};
	
	const px_idx = (y * DATA_WIDTH + x) * 3;
	const R = buf.readUInt8(px_idx);
	const G = buf.readUInt8(px_idx + 1);
	const B = buf.readUInt8(px_idx + 2);
	return -10000 + ((R * 256 * 256 + G * 256 + B) * 0.1);
};


const getElevation = (lon, lat) => {
	
	//Is in data bounds?
	if((lon < SI_BBOX_WGS.xmin) || (lon > SI_BBOX_WGS.xmax) || (lat < SI_BBOX_WGS.ymin) || (lat > SI_BBOX_WGS.ymax)){
		return null;
	};
	
	const px = Math.round(((lon - SI_BBOX_WGS.xmin) / BBOX_WIDTH) * DATA_WIDTH);
	const py = Math.round((1 - (lat - SI_BBOX_WGS.ymin) / BBOX_HEIGHT) * DATA_HEIGHT); //latitude grows up, while pixel y grows down
	
	return getElevationXY(px, py);
};


module.exports = {
    getElevation: getElevation,
}