import { useRef, useEffect } from 'react';
import mapboxgl from 'mapbox-gl';
import 'mapbox-gl/dist/mapbox-gl.css';
import styles from "../styles";

mapboxgl.accessToken = process.env.REACT_APP_MAPBOX_TOKEN;

const Minimap = ({ lat, lng, zoom }) => {
    const mapContainer = useRef(null);
    const map = useRef(null);

    useEffect(() => {
        if (map.current) return;
        map.current = new mapboxgl.Map({
            container: mapContainer.current,
            style: 'mapbox://styles/thatsebastjan/cmal96wtw013501qyglis664t',
            center: [lng, lat],
            zoom: zoom,
            interactive: false,
        });

        new mapboxgl.Marker()
            .setLngLat([lng, lat])
            .addTo(map.current);
    }, [lat, lng, zoom]);

    return <styles.map.MapContainer ref={mapContainer} />;
};

export default Minimap;