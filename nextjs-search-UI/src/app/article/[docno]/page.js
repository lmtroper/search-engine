"use client"
import React from 'react'
import { useRouter } from 'next/navigation';
import { articleFetch } from '../../api';
import Link from 'next/link';
import Image from 'next/image';

export default function Page({ params }) {
    const router = useRouter();
    const { docno } = params;

    const [article, setArticle] = React.useState(null);

    const parser = new DOMParser();
  
    React.useEffect(() => {
      if (docno) {
        const fetchArticle = async () => {
            const article = await articleFetch(docno);
            const content = article.content;

            const xmlDoc = parser.parseFromString(content, 'text/xml');
            console.log("content")
            console.log(content)
            // Extract data from XML
            const docDocno = xmlDoc.querySelector('DOCNO').textContent.trim();
            const date = xmlDoc.querySelector('DATE P').textContent.trim();
            const headline = Array.from(xmlDoc.querySelectorAll('HEADLINE P')).map(p => p.textContent);
            const textParagraphs = xmlDoc.querySelectorAll('TEXT P');
            const text = Array.from(textParagraphs).map(p => p.textContent.trim());
            
            const parsedArticle = {
                docno: docDocno,
                date: date,
                headline: headline,
                text: text
            }

          setArticle(parsedArticle);
        };
        fetchArticle();
      }
    }, [docno]);
  
    if (!article) {
      return <div>Loading...</div>;
    }  

    return (
      <div className='w-full overflow-clip px-16'>
          <div className='w-fit my-10'>
            <Link href="/" className='flex items-center'>
              <Image src="/back-arrow.svg" alt="Back" width={25} height={25} />
              <div className="pl-4" style={{fontSize:'14pt', color:"#484848"}}>
                  Back
              </div>
            </Link>
          </div>
          <div>
            <div className='article-headline mb-1'>
              <h1>{article.headline}</h1>
            </div>
            <div className='article-date mb-8'>
              <p>{article.date}</p>
            </div> 
            <div className='article-body p-4 mb-10 bg-white'>
              {article.text.map((blurb, index) => (
                <>
                  <p className="mb-4" key={index}>{blurb}</p>
                </>
              ))}
              </div>
          </div>
      </div>
  );
};

