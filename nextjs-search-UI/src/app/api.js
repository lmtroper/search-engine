const BASEURL = 'http://localhost:8080'
const ENDPOINT = `/search?query=`

export const searchAPI = async (query) => {
    const searchQuery = `${BASEURL}/search?query=${query}`

    return await fetch(searchQuery)
    .then(response => response.json())
    .catch(error => console.log(error));
}

export const articleFetch = async (docno) => {
    const articleQuery = `${BASEURL}/article?docno=${docno}`
    console.log(docno);
    return await fetch(articleQuery)
    .then(response => response.json())
    .catch(error => console.log(error));
}

