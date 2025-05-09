export default {


    //https://<host>/<context>/rest/services/...
    api: {
        scheme: "https",
        host: "geohub.gov.si", //hostname only - without protocol
        context: "ags"
    },


    //geohub layer ids
    service_layers: {

        // TEMELJNE_VSEBINE/GH_ZK_KO
        parcele: 1460,

        // DRSV/Zun
        obmocje_pogostih_poplav: 4016,
        obmocje_redkih_poplav: 4017,
        obmocje_zelo_redkih_katastrofalnih_poplav: 4018,

        // DRSV/Ogrozena_obmocja
        plazljiva_obmocja: 4402
    },



    //Bounding boxes
    SI_BBOX: {
        xmin: 372865.61688217556,
        ymin: 32561.166641428135,
        xmax: 621704.3425568037, //x grows west
        ymax: 194254.8275896134, //y grows toward north
    },
    
    SI_BBOX_WGS: { //EPSG:3794 -> EPSG:4326 WGS 84
        xmin: 13.3754608,
        ymin: 45.42143,
        xmax: 16.5966968,
        ymax: 46.8766684
    },

};