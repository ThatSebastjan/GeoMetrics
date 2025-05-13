
import { useState, useEffect, useContext} from "react";
import styles from "../styles";
import {useNavigate} from "react-router-dom";
import {UserContext} from "../Contexts/UserContext";

function Settings() {
    const navigate = useNavigate();
    const context = useContext(UserContext);
    const [profileImageUrl, setProfileImageUrl] = useState(null);

    // Set profile image URL when user is available
    useEffect(() => {
        if (context.user && context.user.profileImage && context.user.profileImage.path) {
            setProfileImageUrl(`http://localhost:3001${context.user.profileImage.path}`);
        }
    }, [context.user]);

    const handleLogout = () => {
        // Clear localStorage
        localStorage.removeItem('token');
        localStorage.removeItem('user');

        // Reset the user context
        context.clearUserContext()

        // Navigate to home page
        navigate('/');
    };


    if (!context.user) return (
        <>
            <styles.settings.Section>
                <styles.common.PageTitle>Here is where your user profile lives ...</styles.common.PageTitle>
                <styles.common.SectionTitle>Once you login that is.</styles.common.SectionTitle>
                <br/>
                <styles.settings.LoginButton
                    onClick={() => {
                        window.location.href = '/login';
                    }}
                >
                    Login
                </styles.settings.LoginButton>
            </styles.settings.Section>

            <styles.settings.Section>
                <styles.common.SectionTitle>App Settings</styles.common.SectionTitle>
                <styles.settings.List>
                    <styles.settings.Button
                        onClick={() => alert('Dark mode setting would be toggled here')}
                    >
                        Toggle Dark Mode
                    </styles.settings.Button>
                    <styles.settings.Button
                        onClick={() => alert('Notifications would be disabled here')}
                    >
                        Disable Notifications
                    </styles.settings.Button>
                    <styles.settings.Button
                        onClick={() => alert('Language would be changed here')}
                    >
                        Change Language
                    </styles.settings.Button>
                </styles.settings.List>

            </styles.settings.Section>
        </>
    );

    return (
        <div>
            {/* User info at the top */}
            <styles.settings.Section>
                {/* Profile Image */}
                <div style={{
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    marginBottom: '20px'
                }}>
                    <img
                        src={profileImageUrl || '/default-avatar.png'}
                        alt={`${context.user.username}'s profile`}
                        style={{
                            width: '150px',
                            height: '150px',
                            objectFit: 'cover',
                            borderRadius: '50%',
                            border: `3px solid ${styles.colors.primary}`,
                            marginBottom: '20px'
                        }}
                    />
                    <styles.common.HeroTitle>{context.user.username}</styles.common.HeroTitle>
                    <styles.common.SectionTitle>{context.user.email}</styles.common.SectionTitle>
                </div>

                <styles.settings.Button
                    onClick={() => handleLogout()}
                >
                    Logout
                </styles.settings.Button>
                <styles.settings.EditButton
                    onClick={() => {navigate('/settings/edit-profile');}}>
                    Edit Profile
                </styles.settings.EditButton>
            </styles.settings.Section>

            <styles.settings.Section>
                <styles.common.PageTitle>Settings</styles.common.PageTitle>
            </styles.settings.Section>
        </div>
    );
}

export default Settings;