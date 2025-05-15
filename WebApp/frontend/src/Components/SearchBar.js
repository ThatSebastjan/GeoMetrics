import { useState } from 'react';
import styles from '../styles';
import icons, { CancelIcon, SearchIcon } from './Icons';

function SearchBar({ placeholder = "Search...", onSearch }) {
    const [query, setQuery] = useState('');
    const [isFocused, setIsFocused] = useState(false);
    const [searchResults, setSearchResults] = useState([]);

    const handleSubmit = async (e) => {
        e.preventDefault();

        const value = query.trim();

        if(value.length == 0){
            return;
        };

        const results = await proximitySearchByAddress(value);
        setSearchResults(results);
    };

    const handleChange = (e) => {
        setQuery(e.target.value);
    };

    const handleClear = () => {
        setQuery('');
        onSearch(null);
        setSearchResults([]);
    };

    const handleResultClick = (result) => {
        setQuery(result.properties.full_address);
        onSearch(result);
    };


    //Uses MapBox foward Geocoding to find nearby locations that match the specified address (limited to Slovenia)
    //Returns an array of 5 nearest matches as GeoJson features
    const proximitySearchByAddress = async (address) => {
        try {
            const req = await fetch(`https://api.mapbox.com/search/geocode/v6/forward?q=${address}&proximity=ip&country=si&access_token=${process.env.REACT_APP_MAPBOX_TOKEN}`);
            const resp = await req.json();
            return resp.features;
        }
        catch(err){
            console.log(`Error in proximitySearchByAddress:`, err);
        };
    
        return [];
    };


    return (
        <styles.search.SearchBarContainer>
            <form onSubmit={handleSubmit}>
                <styles.search.SearchInputWrapper $hasResults={isFocused && (searchResults.length != 0)}>
                    <styles.search.SearchIconWrapper>
                        <SearchIcon />
                    </styles.search.SearchIconWrapper>
                    <styles.search.SearchInput
                        type="text"
                        value={query}
                        onChange={handleChange}
                        placeholder={placeholder}
                        onFocus={() => setIsFocused(true)}
                        onBlur={() => setIsFocused(false)}
                    />
                    {(query || (searchResults.length > 0)) && (
                        <styles.search.ClearButtonContainer>
                            <styles.search.ClearButton
                                type="button"
                                onClick={handleClear}
                                aria-label="Clear search"
                            >
                                <styles.layout.IconWrapper><icons.CancelIcon /></styles.layout.IconWrapper>
                            </styles.search.ClearButton>
                        </styles.search.ClearButtonContainer>
                    )}
                </styles.search.SearchInputWrapper>
            </form>

            <styles.search.SearchResultsWrapper $numResults={isFocused ? searchResults.length : 0}>
                {searchResults.map(s => <styles.search.SearchResult onClick={() => handleResultClick(s)}>{s.properties.full_address}</styles.search.SearchResult>)}
            </styles.search.SearchResultsWrapper>
            
        </styles.search.SearchBarContainer>
    );
}

export default SearchBar;