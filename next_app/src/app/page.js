"use client"
import React from "react";
import Search from "./components/Search";
import Result from "./components/Result";
import Image from "next/image";

export default function Home() {
  const [results, setResults] = React.useState([]);
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

  return (
    <div className={`px-16 ${!rendered && "fadeInUp"}`}>
      <div>
        <div className="flex flex-col justify-center items-center mt-40 mb-10">
          <div className="search-title">
            CBC News Search Engine
          </div>
          <div className="mb-10" style={{color:'#3d4452', maxWidth:'40rem', marginTop:'-10px'}}>
            <br />
            This is a traditional search engine built to fetch results from a dataset of CBC news articles. 
            You can search for any topic and the engine will return up to the top 10 best results. You can also click on a result to view the full article. Give it a try! 🚀
          </div>
          <Search prevQuery={prevQuery} onSubmit={setResults} setElpasedTime={setElapsedTime} />
          <div className="flex mt-6">
            <div className="mr-2">
              <button className="github-btn px-5 py-3">Source Code</button>
            </div>
            <div className="ml-2">
              <button className="how-btn px-5 py-3">
                How it works?
              </button>
            </div>
          </div>
        </div>
        <div className="px-10 py-5">
          {results.length > 0 && 
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
          </>
          }
        </div>
      </div>
    </div>
  );
}
