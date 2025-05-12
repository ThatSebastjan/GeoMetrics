import { Outlet } from 'react-router-dom';
import Sidebar from './Sidebar';
import styles from '../styles';

function MainLayout() {
    return (
        <styles.layout.Container>
            <Sidebar />
            <styles.layout.Content>
                <Outlet />
            </styles.layout.Content>
        </styles.layout.Container>
    );
}

export default MainLayout;