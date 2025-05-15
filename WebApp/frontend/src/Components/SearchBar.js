import { useState } from 'react';
import styles from '../styles';
import icons, { CancelIcon, SearchIcon } from './Icons';

function SearchBar({ placeholder = "Search...", onSearch }) {
    const [query, setQuery] = useState('');

    const handleSubmit = (e) => {
        e.preventDefault();
        if (query.trim()) {
            onSearch(query.trim());
        }
    };

    const handleChange = (e) => {
        setQuery(e.target.value);
    };

    const handleClear = () => {
        setQuery('');
        onSearch('');
    };

    return (
        <styles.search.SearchBarContainer>
            <form onSubmit={handleSubmit}>
                <styles.search.SearchInputWrapper>
                    <styles.search.SearchIconWrapper>
                        <SearchIcon />
                    </styles.search.SearchIconWrapper>
                    <styles.search.SearchInput
                        type="text"
                        value={query}
                        onChange={handleChange}
                        placeholder={placeholder}
                    />
                    {query && (
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
        </styles.search.SearchBarContainer>
    );
}

export default SearchBar;