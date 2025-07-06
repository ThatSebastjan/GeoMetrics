import React from 'react';

function WorkInProgress() {
    return (
        <div style={{
            height: '100vh',
            padding: '20px',
            background: 'repeating-linear-gradient(45deg, black, black 25px, yellow 25px, yellow 50px)',
            boxSizing: 'border-box'
        }}>
            <div style={{
                backgroundColor: 'white',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                height: '100%',
                width: '100%',
            }}>

                <h1 style={{
                    fontFamily: 'monospace',
                    fontSize: '2rem',
                    color: 'black',
                    textAlign: 'center'
                }}>
                    This feature is not ready yet.
                </h1>
                <p style={{
                    fontFamily: 'monospace',
                    fontSize: '1.25rem',
                }}>
                    We are working on it! :)
                </p>
            </div>
        </div>
    );
}

export default WorkInProgress;