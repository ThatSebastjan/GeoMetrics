import { ArcGis } from "./arc_gis_api.mjs";
import config from "./config.mjs";


const arc_gis = new ArcGis(config.api);



const main = async (args) => {

    //Check status
    if(await arc_gis.check_status() == false){
        return console.log("Ping error");
    };

    console.log("Ping ok");


    const parcel_service = new ArcGis.MapService(arc_gis, "TEMELJNE_VSEBINE", "GH_ZK_KO", 1460);

    const test = await parcel_service.query({
        geometry: {
            rings:[
                [
                    [360031, 31262],
                    [623695, 31262],
                    [623695, 193755],
                    [360031, 193755],
                    [360031, 31262]
                ]
            ]
        },
        geometryType: "esriGeometryPolygon",
        outFields: "ST_PARCELE,EID_PARCELA,OBJECTID,POVRSINA,GEOMETRY",
        outSR: ArcGis.WKID.WGS,
    });

    console.log(test);

};


main(process.argv.slice(2));