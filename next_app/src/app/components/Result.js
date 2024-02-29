import React from 'react'
import Link from 'next/link';


const Result = ({ result, index, count }) => {
  return (
    <>
        <div className='result-card p-7'>
            <Link href={`/article/${result.docno}`}>
            <div className='result-headline mb-1'>
                {result.headline}
            </div>
            <div className='result-date mb-3'>
                {result.date}
            </div>
            <div className='result-snippet mb-2'>
                {result.snippet}
            </div>
            </Link>
        </div>
    </>
  )
}

export default Result;


