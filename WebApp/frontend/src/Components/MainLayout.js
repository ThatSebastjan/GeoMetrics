import { Outlet, useNavigate } from 'react-router-dom';
import styles from '../styles';

function MainLayout() {
    const navigate = useNavigate();
    const user = JSON.parse(localStorage.getItem('user') || 'null');


    return (
        <styles.layout.Container>
            <styles.layout.Sidebar>
                <styles.layout.Logo>GeoMetrics</styles.layout.Logo>
                <styles.layout.NavMenu>
                    <styles.layout.NavItem onClick={() => navigate('/assess')}>
                        Assess
                    </styles.layout.NavItem>
                    <styles.layout.NavItem onClick={() => navigate('/smart-select')}>
                        Smart Select
                    </styles.layout.NavItem>
                    <styles.layout.NavItem onClick={() => navigate('/side-by-side')}>
                        Side By Side
                    </styles.layout.NavItem>

                </styles.layout.NavMenu>

                {!user ? (
                    <styles.layout.UserSection>
                        <styles.layout.NavItem onClick={() => navigate('/settings')}>
                            Settings
                        </styles.layout.NavItem>

                        <styles.layout.NavItem onClick={() => navigate('/login')}>
                            Login
                        </styles.layout.NavItem>
                        <styles.layout.NavItem onClick={() => navigate('/register')}>
                            Register
                        </styles.layout.NavItem>
                    </styles.layout.UserSection>
                ) : (
                    <styles.layout.UserSection>
                        <styles.layout.NavItem onClick={() => navigate('/saved-lots')}>
                            Saved Lots
                        </styles.layout.NavItem>
                        <styles.layout.NavItem onClick={() => navigate('/results')}>
                            Results
                        </styles.layout.NavItem>
                        <styles.layout.NavItem onClick={() => navigate('/settings')}>
                            Settings
                        </styles.layout.NavItem>
                    </styles.layout.UserSection>
                )}

            </styles.layout.Sidebar>

            <styles.layout.Content>
                <Outlet />
            </styles.layout.Content>
        </styles.layout.Container>
    );
}

export default MainLayout;