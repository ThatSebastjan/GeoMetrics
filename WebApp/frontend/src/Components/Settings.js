
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
            setProfileImageUrl(`http://${window.location.hostname}:3001${context.user.profileImage.path}`);
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
            <styles.settings.ContainerHero>
                <styles.common.PageTitleHero>Here is where your user profile lives ...</styles.common.PageTitleHero>
                <styles.common.SectionTitleHero>Once you login that is.</styles.common.SectionTitleHero>
                <br/>
                <styles.settings.LoginButton
                    onClick={() => {
                        window.location.href = '/login';
                    }}
                >
                    Login
                </styles.settings.LoginButton>
            </styles.settings.ContainerHero>

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
            <styles.settings.ContainerHero>
                <div style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    width: '100%',
                    maxWidth: '85%',
                    margin: '0 40px',
                    padding: '40px 20px'
                }}>
                    {/* User Info and Buttons on the Left */}
                    <div style={{
                        display: 'flex',
                        flexDirection: 'column',
                        alignItems: 'flex-start',
                        justifyContent: 'center',
                        maxWidth: '80%'
                    }}>
                        {context.user ? (
                            <>
                                <div>
                                    <styles.common.HeroUserName style={{ textAlign: 'left'}}>
                                        {context.user.username}
                                    </styles.common.HeroUserName>
                                    <styles.common.PageTitleHero style={{ textAlign: 'left' }}>
                                        {context.user.email}
                                    </styles.common.PageTitleHero>
                                </div>

                                <div style={{
                                    display: 'flex',
                                    flexDirection: 'column',
                                    width: '100px'
                                }}>
                                    <styles.settings.EditButton
                                        onClick={() => {navigate('/settings/edit-profile');}}
                                        style={{ width: '100%' }}
                                    >
                                        Edit Profile
                                    </styles.settings.EditButton>
                                    <styles.settings.LogoutButton
                                        onClick={() => handleLogout()}
                                        style={{ width: '100%' }}
                                    >
                                        Sign Out
                                    </styles.settings.LogoutButton>
                                </div>
                            </>
                        ) : (
                            <>
                                <div style={{ marginBottom: '20px' }}>
                                    <styles.common.PageTitleHero style={{ textAlign: 'left' }}>
                                        Here is where your user profile lives...
                                    </styles.common.PageTitleHero>
                                    <styles.common.SectionTitleHero style={{ textAlign: 'left' }}>
                                        Once you login that is.
                                    </styles.common.SectionTitleHero>
                                </div>
                                <styles.settings.LoginButton
                                    onClick={() => { window.location.href = '/login'; }}
                                    style={{ width: '200px' }}
                                >
                                    Login
                                </styles.settings.LoginButton>
                            </>
                        )}
                    </div>

                    {/* Profile Image on the Right */}
                    <div style={{
                        display: 'flex',
                        justifyContent: 'center',
                        alignItems: 'center',
                        minWidth: '180px',
                        maxWidth: '0%'
                    }}>
                        <img
                            src={profileImageUrl || '/default-avatar.png'}
                            alt={`${context.user?.username || 'User'}'s profile`}
                            style={{
                                width: '300px',
                                height: '300px',
                                objectFit: 'cover',
                                borderRadius: '50%',
                                boxShadow: '0px 0px 10px rgba(0, 0, 0, 0.25)',
                            }}
                        />
                    </div>
                </div>
            </styles.settings.ContainerHero>

            <styles.settings.TitleSection>
                <styles.common.PageTitle>Preferences</styles.common.PageTitle>
                <styles.common.PageSubtitle>Manage app and account settings</styles.common.PageSubtitle>
            </styles.settings.TitleSection>

            <styles.settings.Section>

                <div style={{
                    height: '400px',
                    border: '2px dashed #ccc',
                    borderRadius: '8px',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    margin: '20px 0',
                    color: '#888'
                }}>Settings Content Will Go Here</div>
            </styles.settings.Section>
        </div>
    );
}

export default Settings;