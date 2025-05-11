import React from 'react';

function ProfileImage({ user, size = 'medium', onClick = null }) {
    // Determine image URL
    const getImageUrl = () => {
        if (user && user.profileImage && user.profileImage.path) {
            return `http://localhost:3001${user.profileImage.path}`;
        }
        return '/default-avatar.png'; // Fallback image
    };

    // Determine dimensions based on size
    const getDimensions = () => {
        switch(size) {
            case 'small':
                return { width: '40px', height: '40px' };
            case 'large':
                return { width: '200px', height: '200px' };
            case 'medium':
            default:
                return { width: '100px', height: '100px' };
        }
    };

    const dimensions = getDimensions();

    return (
        <img
            src={getImageUrl()}
            alt={user ? `${user.username}'s profile` : 'Profile'}
            style={{
                ...dimensions,
                objectFit: 'cover',
                borderRadius: '50%',
                border: '2px solid #ddd',
                cursor: onClick ? 'pointer' : 'default'
            }}
            onClick={onClick}
            onError={(e) => {
                e.target.onerror = null;
                e.target.src = '/default-avatar.png';
            }}
        />
    );
}

export default ProfileImage;