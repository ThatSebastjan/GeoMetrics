import { useState, useEffect } from 'react';
import styles from '../styles';
import Map from '../Components/Map.js';

function Assess() {
    const [user, setUser] = useState(null);

    useEffect(() => {
        // Get user data from localStorage
        const userData = localStorage.getItem('user');
        if (userData) {
            setUser(JSON.parse(userData));
        }
    }, []);

    // if (!user) {
    //     return <styles.common.LoadingIndicator>Loading...</styles.common.LoadingIndicator>;
    // }

    return (

        <Map />

    );
}

export default Assess;