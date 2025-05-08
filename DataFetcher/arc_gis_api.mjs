/*

    Implementation of ArcGis API in node.js
    Author: Rok

*/

import fetch from "node-fetch";
import chalk from "chalk";



export class Loggable {

    static DEBUG = 3;
    static WARNING = 2;
    static ERROR = 1;
    static NONE = 0;

    static LEVEL_COLORS = [
        () => {},
        chalk.red,
        chalk.yellow,
        chalk.blue,
    ];


    constructor(log_level, tag="LOG"){
        this.log_level = log_level;
        this.tag = tag;
    };


    log(level, ...args){
        if(level >= this.log_level){
            console.log(Loggable.LEVEL_COLORS[level](`[${this.tag}]`), ...args);
        };
    };
};



/*

Request URL: https://<host>/<context>/rest/services/<folder>/<serviceName>/<serviceType>/<serviceLayer>/<operation>


Constructor parameters:

    endpoint: {
        scheme: String,
        host: String,
        context: String,
    }
  
*/


/*
    ArcGis API class
*/
export class ArcGis extends Loggable {


    //Well known ID's used for projection
    static WKID = {
        WGS: "WGS84",
        Slovenia: "3794", //Slovenia_1996_Slovene_National_Grid
    };


    constructor(endpoint, log_level = Loggable.DEBUG){
        super("ArcGis", log_level);

        this.endpoint = endpoint;
        this.base_url = `${this.endpoint.scheme}://${this.endpoint.host}/${this.endpoint.context}`;
    };


    //Check if endpoint is valid
    async check_status(){
        try {
            const req = await fetch(`${this.base_url}/rest/info/healthCheck?f=json`);
            const resp = await req.json();

            return resp.success;
        }
        catch(err){
            this.log(Loggable.DEBUG, "Status check failed with error:", err)
            return false;
        };
    };


    static get MapService(){
        return MapService;
    };

};



/* 
    ArcGis MapService
*/
class MapService extends Loggable {

    constructor(parent_instance, folder, service_name, service_layer, log_level = Loggable.DEBUG){
        super(log_level, "MapService");

        this.parent_instance = parent_instance;
        this.service_url = `/rest/services/${folder}/${service_name}/MapServer/${service_layer}`;

        this.info = null;
    };


    //Get MapService info
    async get_info(){
        try {
            const req = await fetch(`${this.parent_instance.base_url}${this.service_url}/?f=json`);
            return (this.info = await req.json());
        }
        catch(err){
            this.log(Loggable.DEBUG, "get_info failed with error:", err);
            return null;
        };
    };


    //MapService query
    async query(props){

        const default_props = {
            "where": "1=1",
            "timeRelation": "esriTimeRelationOverlaps",
            "geometry": "",
            "geometryType": "",
            "inSR": "",
            "spatialRel": "esriSpatialRelIntersects",
            "distance": "",
            "units": "esriSRUnit_Meter",
            "outFields": "",
            "returnGeometry": "true",
            "returnTrueCurves": "false",
            "outSR": "",
            "returnIdsOnly": "false",
            "returnCountOnly": "false",
            "returnZ": "false",
            "returnM": "false",
            "returnDistinctValues": "false",
            "resultOffset": 0,
            "resultRecordCount": "",
            "returnExtentOnly": "false",
            "sqlFormat": "none",
            "featureEncoding": "esriDefault",
            "f": "geojson"
        };

        for(let key in props){
            const val = props[key];
            default_props[key] = (typeof(val) == "object") ? JSON.stringify(val) : val;
        };


        const feature_list = [];
        let query_offset = 0;
        let query_chunked = false;

        try {

            //Query all data
            while(true){

                const url = `${this.parent_instance.base_url}${this.service_url}/query`;

                default_props.resultOffset = query_offset;

                const data = new FormData();
                for(let prop in default_props){ data.append(prop, default_props[prop]); };

                const req = await fetch(url, {
                    method: "POST",
                    body: data
                });

                const resp = await req.json();

                if(resp.exceededTransferLimit && (query_offset == 0)){
                    query_chunked = true;
                }
                else if(!resp.exceededTransferLimit && !query_chunked){
                    return resp; //Single request
                };


                //Need an id property for filtering out unique features
                if(resp.features.length != 0){
                    const p1 = resp.features[0];

                    if(!p1.hasOwnProperty("id") && !(p1.properties && p1.properties.hasOwnProperty("OBJECTID"))){
                        throw new Error("Feature must have id or properties.OBJECTID property!");
                    };
                };

                const unique = resp.features.filter((e) => feature_list.findIndex(f => f.id == e.id) == -1); //Sometimes one feature is present in 2 offset requests...
                feature_list.push(...unique);

                //Last request - no more data
                if(query_chunked && !resp.exceededTransferLimit){
                    resp.features = feature_list;
                    return resp;
                };

                //Increment query offset
                query_offset += resp.features.length;
                this.log(Loggable.DEBUG, "query_offset:", query_offset);
            };
        }
        catch(err){
            this.log(Loggable.DEBUG, "query failed with error:", err);
            return null;
        };
    };

};