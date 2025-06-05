import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import styles from '../styles';
import Minimap from './Minimap';
import * as turf from "@turf/turf";

function SavedLots() {
    const [savedLots, setSavedLots] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const navigate = useNavigate();

    // Mock data for saved lots
    useEffect(() => {
        const mockSavedLots = [
            {
                id: 1,
                title: "Riverside Property",
                address: "123 River Road, Ljubljana",
                coordinates: { lat: 46.0569, lng: 14.5058 },
            },
            {
                id: 2,
                title: "Hillside Vineyard",
                address: "456 Vine Hill, Maribor",
                coordinates: { lat: 46.5547, lng: 15.6467 },
            },
            {
                id: 3,
                title: "Mountain Cabin",
                address: "789 Alpine Way, Bled",
                coordinates: { lat: 46.3688, lng: 14.1144 },
            },
            {
                id: 4,
                title: "Coastal Plot",
                address: "101 Sea View, Piran",
                coordinates: { lat: 45.5289, lng: 13.5682 },
            }
        ];


            setSavedLots(mockSavedLots);
            setIsLoading(false);

    }, []);

    const handleViewOnMap = (lot) => {
        // Navigate to assess page with the lot coordinates
        navigate(`/assess?lat=${lot.coordinates.lat}&lng=${lot.coordinates.lng}&id=${lot.id}`);
    };

    const handleCompare = (lot) => {
        // Add to compare queue
        navigate(`/side-by-side?lot=${lot.id}`);
    };

    const handleUnsave = (lotId) => {
        // Remove lot from saved lots
        setSavedLots(savedLots.filter(lot => lot.id !== lotId));
        // In a real app, you would also make an API call to remove from the server
    };

    if (isLoading) {
        return <styles.common.Loading>Loading saved lots...</styles.common.Loading>;
    }

    return (
        <>
            <styles.settings.ContainerHero>
                <styles.common.HeroTitle>Saved Lots</styles.common.HeroTitle>
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