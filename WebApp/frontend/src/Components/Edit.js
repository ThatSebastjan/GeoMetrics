import { useState, useEffect, useContext } from 'react';
import { UserContext } from '../Contexts/UserContext';
import styles from '../styles';
import { useNavigate } from 'react-router-dom';

function Edit() {
    const { user, token, setUserContext } = useContext(UserContext);

    const navigate = useNavigate();
    const [formData, setFormData] = useState({
        username: '',
        email: '',
        currentPassword: '',
        newPassword: ''
    });
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const [success, setSuccess] = useState('');
    const [showPasswordSection, setShowPasswordSection] = useState(false);

    // Load user data on component mount
    useEffect(() => {
        if (!user) {
            navigate('/login');
            return;
        }

        setFormData(prev => ({
            ...prev,
            username: user.username || '',
            email: user.email || ''
        }));
    }, [navigate]);

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: value
        }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setSuccess('');
        setLoading(true);

        try {
            if (!token) {
                throw new Error('Authentication required');
            }

            // Create object with only fields that have values
            const updateData = {};

            // Only add password fields if password section is shown
            if (showPasswordSection) {
                if (!formData.currentPassword) {
                    throw new Error('Current password is required when changing password');
                }
                updateData.currentPassword = formData.currentPassword;
                if (formData.newPassword) {
                    updateData.newPassword = formData.newPassword;
                }
            }

            // Always include basic profile fields
            if (formData.username) updateData.username = formData.username;
            if (formData.email) updateData.email = formData.email;

            const response = await fetch('http://localhost:3001/users/profile/update', {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `${token}`
                },
                body: JSON.stringify(updateData),
            });

            const data = await response.json();

            if (!response.ok) {
                throw new Error(data.message || 'Update failed');
            }

            // Create a properly updated user object using the response data
            const updatedUser = {
                ...user,
                ...data.user || {}, // Merge any user object from response
                username: data.username || formData.username || user.username,
                email: data.email || formData.email || user.email
            };

            // Update context properly with both user and token
            setUserContext({
                user: updatedUser,
                token: token
            });

            setSuccess('Profile updated successfully. Redirecting to Settings ...');

            // Clear password fields
            setFormData(prev => ({
                ...prev,
                currentPassword: '',
                newPassword: ''
            }));

            // Navigate back to settings after a short delay
            setTimeout(() => {
                navigate('/settings');
            }, 1500);
        } catch (error) {
            setError(error.message);
        } finally {
            setLoading(false);
        }
    };

    const togglePasswordSection = () => {
        setShowPasswordSection(!showPasswordSection);
        // Clear password fields when hiding the section
        if (showPasswordSection) {
            setFormData(prev => ({
                ...prev,
                currentPassword: '',
                newPassword: ''
            }));
        }
    };

    return (
        <styles.settings.Section>
            <styles.settings.Header>
                <styles.common.PageTitle>Edit Profile</styles.common.PageTitle>
            </styles.settings.Header>

            {error && <styles.common.Message $type="error">{error}</styles.common.Message>}
            {success && <styles.common.Message $type="success">{success}</styles.common.Message>}

            <form onSubmit={handleSubmit}>
                <styles.settings.FormGroup>
                    <styles.settings.Label htmlFor="username">Username</styles.settings.Label>
                    <styles.settings.Input
                        type="text"
                        id="username"
                        name="username"
                        value={formData.username}
                        onChange={handleChange}
                    />
                </styles.settings.FormGroup>

                <styles.settings.FormGroup>
                    <styles.settings.Label htmlFor="email">Email</styles.settings.Label>
                    <styles.settings.Input
                        type="email"
                        id="email"
                        name="email"
                        value={formData.email}
                        onChange={handleChange}
                    />
                </styles.settings.FormGroup>

                <styles.settings.Button
                    type="button"
                    onClick={togglePasswordSection}
                >
                    {showPasswordSection ? 'Cancel Password Update' : 'Update Password'}
                </styles.settings.Button>

                {showPasswordSection && (
                    <div style={{
                        marginTop: styles.spacing.md,
                        padding: styles.spacing.md,
                        border: `1px solid ${styles.colors.border}`,
                        borderRadius: styles.borderRadius.small
                    }}>
                        <styles.settings.FormGroup>
                            <styles.settings.Label htmlFor="currentPassword">Current Password *</styles.settings.Label>
                            <styles.settings.Input
                                type="password"
                                id="currentPassword"
                                name="currentPassword"
                                value={formData.currentPassword}
                                onChange={handleChange}
                                required
                            />
                        </styles.settings.FormGroup>

                        <styles.settings.FormGroup>
                            <styles.settings.Label htmlFor="newPassword">New Password</styles.settings.Label>
                            <styles.settings.Input
                                type="password"
                                id="newPassword"
                                name="newPassword"
                                value={formData.newPassword}
                                onChange={handleChange}
                            />
                        </styles.settings.FormGroup>
                    </div>
                )}

                <div style={{ display: 'flex', gap: '10px', marginTop: styles.spacing.lg }}>
                    <styles.settings.Button
                        type="submit"
                        disabled={loading}
                    >
                        {loading ? 'Saving...' : 'Save Changes'}
                    </styles.settings.Button>
                    <styles.settings.EditButton onClick={() => navigate('/settings')}>
                        Cancel
                    </styles.settings.EditButton>
                </div>
            </form>
        </styles.settings.Section>
    );
}

export default Edit;