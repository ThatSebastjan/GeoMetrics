// frontend/src/Components/Edit.js
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

        // Set image preview if user has a profile image
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
                headers: {
                    'Authorization': token
                },
                body: formData
            });

            if (!response.ok) {
                throw new Error('Failed to upload image');
            }

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
                headers: {
                    'Authorization': token
                }
            });

            if (!response.ok) {
                throw new Error('Failed to delete image');
            }

            const data = await response.json();

            // Update context with new user data
            setUserContext({
                user: data.user,
                token
            });

            // Clear image preview
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
            if (!token) {
                throw new Error('Authentication required');
            }

            // Upload image first if there is one
            let imageUploadResult = null;
            if (profileImage) {
                imageUploadResult = await handleImageUpload();
                if (!imageUploadResult) {
                    throw new Error('Image upload failed');
                }
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

            // Only proceed with profile update if there are fields to update
            let updatedUser = user;

            if (Object.keys(updateData).length > 0) {
                const response = await fetch('http://localhost:3001/users/profile/update', {
                    method: 'PUT',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': token
                    },
                    body: JSON.stringify(updateData),
                });

                const data = await response.json();

                if (!response.ok) {
                    throw new Error(data.message || 'Update failed');
                }

                // Use data from profile update
                updatedUser = {
                    ...user,
                    ...data.user || {},
                    username: data.username || formData.username || user.username,
                    email: data.email || formData.email || user.email
                };
            }

            // If image was uploaded, use that user data
            if (imageUploadResult && imageUploadResult.user) {
                updatedUser = {
                    ...updatedUser,
                    profileImage: imageUploadResult.user.profileImage
                };
            }

            // Update context
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
                {/* Profile Image Section */}
                <styles.settings.FormGroup>
                    <styles.settings.Label>Profile Picture</styles.settings.Label>
                    <div style={{
                        display: 'flex',
                        flexDirection: 'column',
                        alignItems: 'center',
                        marginBottom: '20px'
                    }}>
                        {imagePreview && (
                            <div style={{ marginBottom: '10px' }}>
                                <img
                                    src={imagePreview}
                                    alt="Profile Preview"
                                    style={{
                                        width: '150px',
                                        height: '150px',
                                        objectFit: 'cover',
                                        borderRadius: '50%',
                                        border: `3px solid ${styles.colors.primary}`
                                    }}
                                />
                            </div>
                        )}
                        <div style={{ display: 'flex', gap: '10px', marginTop: '10px' }}>
                            <label style={{
                                padding: '8px 15px',
                                backgroundColor: styles.colors.primary,
                                color: 'white',
                                borderRadius: '4px',
                                cursor: 'pointer',
                                display: 'inline-block'
                            }}>
                                Choose Image
                                <input
                                    type="file"
                                    accept="image/*"
                                    onChange={handleImageChange}
                                    style={{ display: 'none' }}
                                />
                            </label>
                            {imagePreview && (
                                <styles.settings.Button
                                    type="button"
                                    onClick={handleDeleteImage}
                                    disabled={loading}
                                    style={{ backgroundColor: styles.colors.danger }}
                                >
                                    Delete Image
                                </styles.settings.Button>
                            )}
                        </div>
                    </div>
                </styles.settings.FormGroup>

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