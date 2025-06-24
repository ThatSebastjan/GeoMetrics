import { useState, useEffect, useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import styles from '../styles';
import Minimap from './Minimap';
import { UserContext } from "../Contexts/UserContext";

function SavedLots() {
    const [savedLots, setSavedLots] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const navigate = useNavigate();
    const context = useContext(UserContext);

    // Mock data for saved lots
    useEffect(() => {

        const fetchSavedData = async () => {

            const req = await fetch(`http://${window.location.hostname}:3001/users/getSavedLots`, {
                headers: { "Authorization": context.token },
            });

            const resp = await req.json();

            const data = resp.map((r, i) => {
                return {
                    id: r.OBJECTID,
                    title: r.name,
                    address: r.address,
                    coordinates: { lng: r.coordinates[0], lat: r.coordinates[1] },
                };
            });

            setSavedLots(data);
            setIsLoading(false);
        };

        fetchSavedData();
    }, []);

    const handleViewOnMap = (lot) => {
        // Navigate to assess page with the lot coordinates
        navigate(`/assess/basic?lng=${lot.coordinates.lng}&lat=${lot.coordinates.lat}&id=${lot.id}`);
    };

    const handleCompare = (lot) => {
        // Add to compare queue
        navigate(`/side-by-side?lng=${lot.coordinates.lng}&lat=${lot.coordinates.lat}&id=${lot.id}`);
    };

    const handleUnsave = async (lotId) => {
        
        const req = await fetch(`http://${window.location.hostname}:3001/users/savedLots`, {
            method: "DELETE",
            headers: {
                "Authorization": context.token,
                "Content-Type": "application/json",
            },
            body: JSON.stringify({
                OBJECTID: lotId
            }),
        });

        const resp = await req.json();

        const data = resp.map((r, i) => {
            return {
                id: r.OBJECTID,
                title: r.name,
                address: r.address,
                coordinates: { lng: r.coordinates[0], lat: r.coordinates[1] },
            };
        });

        setSavedLots(data);
    };

    if (isLoading) {
        return <styles.common.Loading>Loading saved locations...</styles.common.Loading>;
    }

    return (
        <>
            <styles.settings.ContainerHero>
                <styles.common.HeroTitle>Saved Locations</styles.common.HeroTitle>
                <p>View and manage your saved property lots.</p>
            </styles.settings.ContainerHero>

            <styles.settings.Container>
                {savedLots.length === 0 ? (
                    <styles.common.Card>
                        <p>You don't have any saved lots yet. Browse the map and save lots to see them here.</p>
                    </styles.common.Card>
                ) : (
                    <styles.saved.LotsGrid>
                        {savedLots.map(lot => (
                            <styles.saved.LotCard key={lot.id}>
                                <styles.saved.MapContainer>

                                    <Minimap
                                        lat={lot.coordinates.lat}
                                        lng={lot.coordinates.lng}
                                        zoom={16}
                                    />

                                </styles.saved.MapContainer>

                                <styles.saved.LotInfo>
                                    <div>
                                        <styles.saved.LotTitle>{lot.title}</styles.saved.LotTitle>
                                        <styles.saved.LotAddress>{lot.address}</styles.saved.LotAddress>
                                    </div>

                                    <styles.saved.ButtonGroup>
                                        <styles.common.Button onClick={() => handleViewOnMap(lot)}>
                                            View on Map
                                        </styles.common.Button>
                                        <styles.common.Button onClick={() => handleCompare(lot)}>
                                            Add to Compare
                                        </styles.common.Button>
                                        <styles.common.Button $secondary onClick={() => handleUnsave(lot.id)}>
                                            Unsave
                                        </styles.common.Button>
                                    </styles.saved.ButtonGroup>
                                </styles.saved.LotInfo>
                            </styles.saved.LotCard>
                        ))}
                    </styles.saved.LotsGrid>
                )}
            </styles.settings.Container>
        </>
    );
}

export default SavedLots;