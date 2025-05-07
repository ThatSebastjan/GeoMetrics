/*

    Implementation of ArcGis API in node.js
    Author: Rok

*/

import fetch from "node-fetch";



export class Loggable {

    static DEBUG = 3;
    static WARNING = 2;
    static ERROR = 1;
    static NONE = 0;


    constructor(log_level, tag="LOG"){
        this.log_level = log_level;
        this.tag = tag;
    };


    log(level, ...args){
        if(level >= this.log_level){
            console.log(`[${this.tag}]`, ...args);
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


        try {
            const url = `${this.parent_instance.base_url}${this.service_url}/query`;

            const data = new FormData();
            for(let prop in default_props){ data.append(prop, default_props[prop]); };

            const req = await fetch(url, {
                method: "POST",
                body: data
            });

            return await req.json();
        }
        catch(err){
            this.log(Loggable.DEBUG, "query failed with error:", err);
            return null;
        };
    };

};