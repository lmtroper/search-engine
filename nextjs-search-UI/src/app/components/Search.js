"use client"
import React from 'react'
import { searchAPI } from '../api';

const Search = ({ prevQuery, onSubmit, setElpasedTime }) => {
  const [query, setQuery] = React.useState("");

  React.useEffect(() => {
    setQuery(prevQuery);
  }, [prevQuery]); 

  const handleChange = (e) => {
    setQuery(e.target.value);
  };

  const handleKeyPress = async (e) => {
    console.log('key press', e.key)
    if (e.key === 'Enter') {
      await handleSearch();
    }
  };

  const handleSearch = async (e) => {
    const queryResults = await searchAPI(query)
    onSubmit(query, queryResults.results)
    const time = queryResults.elapsedTime / 1000
    setElpasedTime(time.toFixed(2))
    sessionStorage.setItem('prevQuery',JSON.stringify(query))
    sessionStorage.setItem('prevResults', JSON.stringify(queryResults.results))
    sessionStorage.setItem('prevElapsedTime', JSON.stringify(time.toFixed(2)))
  }

  return (
    <div class="input-container">
      <div className="w-11/12">
        <input type="text" onKeyPress={handleKeyPress} name="text" value={query} class="w-full input" onChange={handleChange} placeholder="Enter your query..." />
      </div>
      <div className="search-button">
        <button onClick={handleSearch}>
          <svg viewBox="0 0 512 512" class="searchIcon"><path d="M416 208c0 45.9-14.9 88.3-40 122.7L502.6 457.4c12.5 12.5 12.5 32.8 0 45.3s-32.8 12.5-45.3 0L330.7 376c-34.4 25.2-76.8 40-122.7 40C93.1 416 0 322.9 0 208S93.1 0 208 0S416 93.1 416 208zM208 352a144 144 0 1 0 0-288 144 144 0 1 0 0 288z"></path></svg>
        </button>
      </div>
    </div>
  )
}

export default Search
