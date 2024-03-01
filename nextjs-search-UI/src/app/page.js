"use client"
import React from "react";
import Search from "./components/Search";
import Result from "./components/Result";
import Link from "next/link";
import Image from "next/image";

export default function Home() {
  const [results, setResults] = React.useState([]);
  const [noResult, setNoResults] = React.useState(false);
  const [elapsedTime, setElapsedTime] = React.useState("");
  const [prevQuery, setPrevQuery] = React.useState(null);
  const [rendered, setRendered] = React.useState(false);

  React.useEffect(() => { 
    const prevQuery = JSON.parse(sessionStorage.getItem('prevQuery'))
    const prevElapsedTime = JSON.parse(sessionStorage.getItem('prevElapsedTime'))
    const prevResults = JSON.parse(sessionStorage.getItem('prevResults'))
    if (prevResults && prevQuery) {
      setResults(prevResults)
      setPrevQuery(prevQuery)
      setElapsedTime(prevElapsedTime)
      setRendered(true)
    }
  }, [])

  const handleSearch = (query, results) => {
    if (results.length === 0 && query !== "") {
      setNoResults(true);
      setResults([]);
    } else {
      setNoResults(false);
      setResults(results);
    }
  }

  return (
    <div className={`px-16 ${!rendered && "fadeInUp"}`}>
      <div className="flex flex-col justify-center items-center">
        <div className="flex flex-col justify-center items-center mt-40 mb-10">
          <div style={{width:'44rem'}}>
            <div className="search-title mb-2">
            CBC News Search Engine
            </div>
            <div className="flex mb-10" style={{color:'#3d4452', width:'100%', marginTop:'-10px'}}>
        
                This is a traditional search engine built to fetch results from a dataset of CBC news articles. 
                You can search for any topic and the engine will return up to the top 10 best results. 
                You can also click on a result to view the full article. 
                Give it a try! 🚀
            </div>
            <Search prevQuery={prevQuery} onSubmit={handleSearch} setElpasedTime={setElapsedTime} />
            <div className="flex justify-center mt-6">
              <div className="mr-2">
                <Link href="https://github.com/lmtroper/search-engine" target="_blank">
                <button className="github-btn px-5 py-3 shadow-md">Source Code</button>
                </Link>
              </div>
              <div className="ml-2">
                <Link href="/about">
                <button className="how-btn px-5 py-3 shadow-md">
                  How it works?
                </button>
                </Link>
              </div>
            </div>
          </div>
        </div>
        <div style={{width:'80rem'}} className="mt-5 mb-10 flex flex-col justify-center">
          {results.length > 0 ?
          <>
            <div className="mb-5 text-gray-500">
              {results.length >= 10 
              ? `Top ${results.length} results` : 
              `${results.length} matching results`} ({elapsedTime} seconds)
            </div>
            {results.map((result, index) => (
              <>
              <Result 
                count={index} 
                key={result.docno} 
                result={result}
                />
              </>
            ))}
          </> :
          <div className="flex justify-center mt-10 how-description">
            {noResult ? "No results found. Try entering a different query!" : ""}
          </div>
          }
        </div>
      </div>
    </div>
  );
}
