import { useRef, useEffect } from 'react';
import mapboxgl from 'mapbox-gl';
import 'mapbox-gl/dist/mapbox-gl.css';
import styles from "../styles";

mapboxgl.accessToken = process.env.REACT_APP_MAPBOX_TOKEN;

const Map = () => {
    const mapContainer = useRef(null);
    const map = useRef(null);

    useEffect(() => {
        if (map.current) return; // initialize map only once
        map.current = new mapboxgl.Map({
            container: mapContainer.current,
            style: 'mapbox://styles/thatsebastjan/cmal96wtw013501qyglis664t',
            center: [14.9955, 46.1512], // Slovenia center coordinates
            zoom: 8
        });
    }, []);

    return <styles.map.MapContainer ref={mapContainer} />;
};

export default Map;