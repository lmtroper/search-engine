"use client"
import React from "react";
import Image from "next/image";
import Link from "next/link";

export default function About() {
  const [defaultMode, setDefaultMode] = React.useState(1);

  const toggleMode = (mode) => {
    if (mode !== defaultMode) {
      setDefaultMode(mode);
    }
  };

  console.log(defaultMode)
  return (
    <div className='px-16'>
      <Link href="/">
        <div className="flex mt-6 items-center">
          <Image src="/back-arrow.svg" alt="Back" width={25} height={25} />
          <div className="pl-4" style={{fontSize:'14pt', color:"#484848"}}>
              Back
          </div>
        </div>
      </Link>
      <div className="fadeInUp">
        <div className="flex flex-col justify-center items-center mt-20 mb-10">
          <div className="search-title mb-4">
            How it Works?
          </div>
          <ToggleSwitch toggleMode={toggleMode} defaultMode={defaultMode} />
          {defaultMode === 1 && 
          <>
            <div className="w-1/2 mb-10 how-description">
              An index engine was built to process the scraped articles and extract relevant textual content. 
              It tokenizes the text and constructs a lexicon. 
              The engine then builds the inverted index, mapping terms to document identifiers and facilitating 
              efficient query processing. 
            </div>
            <div>
              <Image src="/index_engine.svg" alt="Search Engine" width={1000} height={1000} />
            </div>
          </>}
          {defaultMode === 2 && 
          <>
            <div className="w-1/2 mb-10 how-description">
            Given a user query, the engine consults the inverted index to retrieve the list of candidate documents that contain some or all of the query terms.
            <br /><br />
            The search engine then uses the&nbsp; 
            <Link className="text-blue-500" href="https://en.wikipedia.org/wiki/Okapi_BM25#:~:text=6%20External%20links-,The%20ranking%20function,slightly%20different%20components%20and%20parameters" target="_blank">
              BM25 scoring algorithm
            </Link>, a popular ranking function used in information retrieval, to assign a relevance score to each document based on the frequency of query terms within the document and other factors such as document length and term frequency within the document collection.
            </div>
            <div>
              <Image src="/bm25.svg" alt="Search Engine" width={600} height={600} />
            </div>
          </>}
        </div>
      </div>
    </div>
  );
}

const ToggleSwitch = ({ toggleMode, defaultMode }) => {
  return (
    <div className="mb-10">
      <button className={`toggle-btn`}
        onClick={()=>{toggleMode(1)}} 
        style={{borderRadius: "10px 0px 0px 10px"}}
        >
          <div class={`left-toggle p-5 ${defaultMode === 1 ? 'active' : ''}`}>
          🔨 Search Engine Build
          </div>
      </button>
      <button className={`toggle-btn`}
        onClick={()=>{toggleMode(2)}}
        style={{borderRadius: "0px 10px 10px 0px"}}
        >
          <div class={`right-toggle p-5 ${defaultMode === 2 ? 'active' : ''}`}>
           🔍 Retrieval Process
          </div>
      </button>
    </div>
  );
}
