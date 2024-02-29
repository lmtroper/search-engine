package com.search.api.controller;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import component.Retrieval.*;
import component.SearchResultWrapper.*;
import component.DocumentData.*;
import component.GetDoc.*;

@RestController
public class SearchController {
    private final Retrieval searchEngine;
    private final GetDoc documentRetriever;

    public SearchController() throws ClassNotFoundException {
        this.searchEngine = new Retrieval(new File("/Users/larissatroper/Documents/cbc-articles"));
        this.documentRetriever = new GetDoc();
    }

	
	@CrossOrigin(origins = "http://localhost:3000/")
	@GetMapping("/")
	public String home() {
		return "Hello World2";
	}
	
	@CrossOrigin(origins = "http://localhost:3000/")
	@GetMapping("/search")
	public SearchResultWrapper search(@RequestParam(value="query", defaultValue="") String query) throws ClassNotFoundException, FileNotFoundException, IOException {
		SearchResultWrapper output = searchEngine.querySearch(query);
		return output;
	}
	
	@CrossOrigin(origins = "http://localhost:3000/")
	@GetMapping("/article")
	public DocumentData getArticle(@RequestParam(value="docno", required=true, defaultValue="") String docno) throws ClassNotFoundException, FileNotFoundException, IOException {
		DocumentData article = documentRetriever.getDataByDocno(new File("/Users/larissatroper/Documents/cbc-articles"), docno);
		return article;
	}
	
}
