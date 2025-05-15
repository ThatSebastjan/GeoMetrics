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
    const [profileImage, setProfileImage] = useState(null);
    const [imagePreview, setImagePreview] = useState(null);
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const [success, setSuccess] = useState('');
    const [showPasswordSection, setShowPasswordSection] = useState(false);

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

        if (user.profileImage && user.profileImage.path) {
            setImagePreview(`http://localhost:3001${user.profileImage.path}`);
        }
    }, [navigate, user]);

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: value
        }));
    };

    const handleImageChange = (e) => {
        const file = e.target.files[0];
        if (file) {
            setProfileImage(file);
            setImagePreview(URL.createObjectURL(file));
        }
    };

    const handleImageUpload = async () => {
        if (!profileImage) return null;

        try {
            const formData = new FormData();
            formData.append('profileImage', profileImage);

            const response = await fetch('http://localhost:3001/users/profile/image', {
                method: 'POST',
                headers: { 'Authorization': token },
                body: formData
            });

            if (!response.ok) throw new Error('Failed to upload image');
            return await response.json();
        } catch (error) {
            console.error('Error uploading image:', error);
            setError(error.message);
            return null;
        }
    };

    const handleDeleteImage = async () => {
        try {
            setLoading(true);
            const response = await fetch('http://localhost:3001/users/profile/image', {
                method: 'DELETE',
                headers: { 'Authorization': token }
            });

            if (!response.ok) throw new Error('Failed to delete image');
            const data = await response.json();

            setUserContext({ user: data.user, token });
            setImagePreview(null);
            setProfileImage(null);
            setSuccess('Profile image deleted successfully');
        } catch (error) {
            console.error('Error deleting image:', error);
            setError(error.message);
        } finally {
            setLoading(false);
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setSuccess('');
        setLoading(true);

        try {
            if (!token) throw new Error('Authentication required');

            let imageUploadResult = null;
            if (profileImage) {
                imageUploadResult = await handleImageUpload();
                if (!imageUploadResult) throw new Error('Image upload failed');
            }

            const updateData = {};

            if (showPasswordSection) {
                if (!formData.currentPassword) {
                    throw new Error('Current password is required when changing password');
                }
                updateData.currentPassword = formData.currentPassword;
                if (formData.newPassword) updateData.newPassword = formData.newPassword;
            }

            if (formData.username) updateData.username = formData.username;
            if (formData.email) updateData.email = formData.email;

            let updatedUser = user;

            if (Object.keys(updateData).length > 0) {
                const response = await fetch('http://localhost:3001/users/profile/update', {
                    method: 'PUT',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': token
                    },
                    body: JSON.stringify(updateData)
                });

                const data = await response.json();
                if (!response.ok) throw new Error(data.message || 'Update failed');

                updatedUser = {
                    ...user,
                    ...data.user || {},
                    username: data.username || formData.username || user.username,
                    email: data.email || formData.email || user.email
                };
            }

            if (imageUploadResult && imageUploadResult.user) {
                updatedUser = {
                    ...updatedUser,
                    profileImage: imageUploadResult.user.profileImage
                };
            }

            setUserContext({ user: updatedUser, token });
            setSuccess('Profile updated successfully. Redirecting to Settings ...');
            setFormData(prev => ({
                ...prev,
                currentPassword: '',
                newPassword: ''
            }));

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
        if (showPasswordSection) {
            setFormData(prev => ({
                ...prev,
                currentPassword: '',
                newPassword: ''
            }));
        }
    };

    // Define a common button style for all main buttons
    const buttonStyle = {
        minWidth: '140px',
        padding: '0.4rem 1.2rem',
        transition: 'background 0.2s, box-shadow 0.2s'
    };

    return (
        <styles.settings.Section style={{
            minHeight: 'calc(100vh - 100px)',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            padding: '2rem 1rem'
        }}>
            <styles.settings.Header style={{ maxWidth: '500px', width: '100%', textAlign: 'center' }}>
                <styles.common.PageTitle>Edit Profile</styles.common.PageTitle>
            </styles.settings.Header>

            {error && <styles.common.Message $type="error" style={{ maxWidth: '500px', width: '100%' }}>{error}</styles.common.Message>}
            {success && <styles.common.Message $type="success" style={{ maxWidth: '500px', width: '100%' }}>{success}</styles.common.Message>}

            <form onSubmit={handleSubmit} style={{
                width: '100%',
                maxWidth: '500px',
                display: 'flex',
                flexDirection: 'column',
                gap: '1.5rem',
                marginTop: '1.5rem',
                background: '#fff',
                borderRadius: '16px',
                boxShadow: '0 4px 24px rgba(0,0,0,0.08)',
                padding: '2rem'
            }}>
                <styles.settings.FormGroup style={{ alignItems: 'center', maxWidth: '350px', margin: '0 auto', width: '100%' }}>
                    <styles.settings.Label style={{ marginBottom: '0.5rem' }}>Profile Picture</styles.settings.Label>
                    {imagePreview && (
                        <img
                            src={imagePreview}
                            alt="Profile"
                            style={{
                                width: '140px',
                                height: '140px',
                                borderRadius: '50%',
                                objectFit: 'cover',
                                border: `3px solid ${styles.colors.primary}`,
                                margin: '1.5rem 0 1rem 0',
                                boxShadow: '0 2px 8px rgba(0,0,0,0.06)'
                            }}
                        />
                    )}
                    <div style={{ display: 'flex', gap: '10px', justifyContent: 'center', width: '100%', marginTop: '0.5rem' }}>
                        <styles.settings.Button
                            type="button"
                            onClick={() => document.getElementById('imageInput').click()}
                            style={{
                                ...buttonStyle,
                            }}
                            onMouseOver={e => e.currentTarget.style.backgroundColor = styles.colors.primaryLight}
                            onMouseOut={e => e.currentTarget.style.backgroundColor = ''}
                        >
                            Choose Image
                            <input
                                id="imageInput"
                                type="file"
                                accept="image/*"
                                onChange={handleImageChange}
                                style={{ display: 'none' }}
                            />
                        </styles.settings.Button>
                        {imagePreview && (
                            <styles.settings.Button
                                type="button"
                                onClick={handleDeleteImage}
                                disabled={loading}
                                style={{
                                    ...buttonStyle,
                                    backgroundColor: styles.colors.danger
                                }}
                                onMouseOver={e => e.currentTarget.style.backgroundColor = '#ff4d4f'}
                                onMouseOut={e => e.currentTarget.style.backgroundColor = styles.colors.danger}
                            >
                                Delete
                            </styles.settings.Button>
                        )}
                    </div>
                </styles.settings.FormGroup>

                <styles.settings.FormGroup style={{ maxWidth: '350px', margin: '0 auto', width: '100%' }}>
                    <styles.settings.Label htmlFor="username">Username</styles.settings.Label>
                    <styles.settings.Input
                        id="username"
                        name="username"
                        type="text"
                        value={formData.username}
                        onChange={handleChange}
                    />
                </styles.settings.FormGroup>

                <styles.settings.FormGroup style={{ maxWidth: '350px', margin: '0 auto', width: '100%' }}>
                    <styles.settings.Label htmlFor="email">Email</styles.settings.Label>
                    <styles.settings.Input
                        id="email"
                        name="email"
                        type="email"
                        value={formData.email}
                        onChange={handleChange}
                    />
                </styles.settings.FormGroup>

                <div style={{ display: 'flex', justifyContent: 'center', width: '100%' }}>
                    <styles.settings.Button
                        type="button"
                        onClick={togglePasswordSection}
                        style={{
                            ...buttonStyle,
                            backgroundColor: styles.colors.secondary,
                            margin: '0.5rem 0'
                        }}
                    >
                        {showPasswordSection ? 'Cancel Password Update' : 'Update Password'}
                    </styles.settings.Button>
                </div>

                {showPasswordSection && (
                    <div style={{
                        backgroundColor: '#f9f9f9',
                        border: `1px solid ${styles.colors.border}`,
                        borderRadius: '8px',
                        padding: '1rem',
                        display: 'flex',
                        flexDirection: 'column',
                        gap: '1rem'
                    }}>
                        <styles.settings.FormGroup>
                            <styles.settings.Label htmlFor="currentPassword">Current Password *</styles.settings.Label>
                            <styles.settings.Input
                                id="currentPassword"
                                name="currentPassword"
                                type="password"
                                value={formData.currentPassword}
                                onChange={handleChange}
                                required
                            />
                        </styles.settings.FormGroup>

                        <styles.settings.FormGroup>
                            <styles.settings.Label htmlFor="newPassword">New Password</styles.settings.Label>
                            <styles.settings.Input
                                id="newPassword"
                                name="newPassword"
                                type="password"
                                value={formData.newPassword}
                                onChange={handleChange}
                            />
                        </styles.settings.FormGroup>
                    </div>
                )}

                <div style={{ display: 'flex', gap: '1rem', justifyContent: 'center' }}>
                    <styles.settings.Button
                        type="submit"
                        disabled={loading}
                        style={buttonStyle}
                    >
                        {loading ? 'Saving...' : 'Save Changes'}
                    </styles.settings.Button>
                    <styles.settings.EditButton
                        type="button"
                        onClick={() => navigate('/settings')}
                        style={buttonStyle}
                    >
                        Cancel
                    </styles.settings.EditButton>
                </div>
            </form>
        </styles.settings.Section>
    );
}

export default Edit;
