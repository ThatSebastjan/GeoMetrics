import { useState, useEffect } from 'react';
import styles from '../styles';

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
        <div>
            <styles.common.PageTitle>PLACEHOLDER ASSESS</styles.common.PageTitle>
        </div>
    );
}

export default Assess;