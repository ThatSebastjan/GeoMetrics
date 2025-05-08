import fs from "node:fs";


const id_map = {};


const parcel_data = fs.readdirSync("./parcel_data/").filter(e => e.includes(".json"));

for(let i = 0; i < parcel_data.length; i++){
    const data = JSON.parse(fs.readFileSync(`./parcel_data/${parcel_data[i]}`, "utf8"));

    const new_data = data.filter(e => !id_map[e.id]);
    new_data.forEach(e => id_map[e.id] = true);
    fs.writeFileSync(`./parcel_data/unique/${parcel_data[i]}`, JSON.stringify(new_data));

    console.log("Processed:", i);
};

console.log("Total count:", Object.keys(id_map).length);

fs.writeFileSync("./parcel_data/unique/ids.txt", Object.keys(id_map).join(",\n"));