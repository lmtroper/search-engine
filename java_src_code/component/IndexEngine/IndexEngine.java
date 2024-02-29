/* Name: Larissa Troper
 * MSCI 541 Homework 2 - Program 1: IndexEngine Updated
 * Date Written: 10 October 2023
 * Description: This program will parse a gzipped file of documents and create a directory system
 * of each individual document. The directory system is divided using the date in the DOCNO for 
 * each document.
 * Input: The file path of the gzipped file and the filepath for where the root document directory
 * is to be created.
 * Output: A directory system of the documents in the gzipped file, a file mapping internal id to
 * DOCNO, a file mapping internal id to document length, a serialized lexicon object, a serialized 
 * reverse lexicon mapping, and a serialzied inverted index object.
*/

package component.IndexEngine;

import component.Stemmer.PorterStemmer;

import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Set;
import java.io.ObjectOutputStream;
import java.io.FileOutputStream;

public class IndexEngine {

	public static void main(String[] args) {
		if( args.length < 2) {
			System.out.println("\nThe IndexEngine program requires two parameters: <path/read/latimes.gz> <path/write/filename> <stem>");
			
			System.out.println("\nThe first parameter is the file path to the gzipped file of LA Times documents. The second parameter is the"
					+ " path to a new root directory for storing each document and its metadata. There is a third optional argument 'stem' which"
					+ "when included will use the Porter Stemmer on the document terms.\n");
			System.exit(1);
		}
		
		if (!args[0].contains(".gz")){
			System.out.println("\nERROR: This program is specifically designed to parse a gzipped document . Please ensure"
					+ " the file path you are passing leads to the gzipped file.\n");
			System.exit(2);
		}
		
		if (!new File(args[0]).exists()) {
			System.out.println("\nERROR: Could not find " + args[0] + ". Please provide the correct file path to "
					+ " the gzipped file.\n");
			System.exit(3);
		}
		
		
		boolean stem = false;
		if(args.length == 3 && args[2].toLowerCase().equals("stem")) {
			stem = true;
			args[1] = args[1]+"-stemmed";
		}
		
		File rootDir = new File(args[1]);
		
		if (rootDir.exists()) {
			System.out.println("\nERROR: " + rootDir + " is a directory that already exists. Please provide a new directory.\n");
			System.exit(4);
		}
		
		rootDir.mkdirs();
		
		parseGzipToDocFiles(args[0], rootDir, stem);
		
	}
	
	/*
	 * The method parses through a gzipped file of documents. The content and metadata of each 
	 * document is parsed and stored into a unique directory based on the DOCNO. 
	 * 
	 * An additional file is written containing each document's docno to be used for indexing.
	 * 
	 * @param readFile A string of the file path to the gzipped file of documents.
	 * @param rootDir  A file to store the directories of parsed documents.
	 */
	public static void parseGzipToDocFiles(String readFile, File rootDir, boolean stem) {
		/* 
		 * Implemented reading of gzipped file using code from 
		 * https://stackoverflow.com/questions/1080381/gzipinputstream-reading-line-by-line
		*/
		try {
			// INPUT FILE READER
			GZIPInputStream gzip = new GZIPInputStream(new FileInputStream(readFile));
			BufferedReader br = new BufferedReader(new InputStreamReader(gzip));
			
			// INDEX WRITER
			BufferedWriter bwIndex = new BufferedWriter(new FileWriter(rootDir+"/index.txt"));
			
			// DOC LENGTHS WRITER
			BufferedWriter bwDocLength = new BufferedWriter(new FileWriter(rootDir+"/doc-lengths.txt"));
			
			// DOC WRITER
			BufferedWriter bwDoc = null;
			
			// Important variables for metadata processing
			String docno = "";
			String date = "";
			File docDirectory = null;
			int internalId = 0;
				
			Map<String, Integer> lexicon = new HashMap<>();
			Map<Integer, String> lexiconReverse = new HashMap<>();
			
			ArrayList<ArrayList<Integer>> invertedIndex = new ArrayList<ArrayList<Integer>>();
			
			String line;
			int num_docs = 0;
			
			while ((line = br.readLine()) != null) {

				// Create directory for document
				if (line.equals("<DOC>")) {	
					num_docs += 1;
					String docTag = line; // Store open DOC tag
					line = br.readLine(); // Read DOCNO tag to get date for directory
					
					int docnoStart = line.indexOf("LA");
					int docnoEnd = line.indexOf("</DOCNO>");
					
					docno = line.substring(docnoStart, docnoEnd).replaceAll("\\s+", ""); // remove white space
					
					String month = docno.substring(2,4);
					String day = docno.substring(4,6);
					String year = docno.substring(6,8);
					
					date = dateFormatter(month, day, year);
					
					docDirectory = new File(rootDir + "/" + year + "/" + month + "/" + day + "/");
					docDirectory.mkdirs();
					
					// Initialize doc writer		
					bwDoc = new BufferedWriter(new FileWriter(docDirectory+"/"+docno+".txt"));					

					
					//Write to doc file
					bwDoc.write(docTag); //<DOC> tag
					bwDoc.newLine();
					bwDoc.write(line); // <DOCNO> line
					bwDoc.newLine();
					
				}
				else if(line.equals("</DOC>")) {
					bwDoc.write(line);
					bwDoc.close();

					// Write to indexing file
					bwIndex.write(docno);
					bwIndex.newLine();
					
					// Implementation done by referencing pseudocode from lecture (Sept 29 2023)
					ArrayList<String> tokens = writeDocMetadataAndGetTokens(docDirectory, docno, internalId, date); // Parse metadata from document
					bwDocLength.write(tokens.size()+"\n");
					ArrayList<Integer> tokenIDs = convertTokensToIDs(tokens, lexicon, lexiconReverse, stem);
					Map<Integer, Integer> wordCounts = countWords(tokenIDs);
					addToPostingsList(wordCounts, internalId, invertedIndex);
					
					internalId += 1; // Increment internal id for next document
				}
				
				else {
					bwDoc.write(line);
					bwDoc.newLine();
				}				
			}
				
			
			System.out.println("number of tokens: " + lexicon.size());
			System.out.println("number of docs: " + num_docs);
			gzip.close();
			bwIndex.close();
			bwDocLength.close();

			// Below code used from https://howtodoinjava.com/java/collections/arraylist/serialize-deserialize-arraylist/
			// The serialization process takes ~ 8-10 minutes on top of document parsing
			try{	
			    ObjectOutputStream oosInvertedIndex = new ObjectOutputStream(new FileOutputStream(rootDir+"/invertedIndex.ser"));
			    ObjectOutputStream oosLexicon = new ObjectOutputStream(new FileOutputStream(rootDir+"/lexicon.ser"));
			    ObjectOutputStream oosLexiconReverse = new ObjectOutputStream(new FileOutputStream(rootDir+"/lexiconReverse.ser"));


			    oosInvertedIndex.writeObject(invertedIndex);

			    oosLexicon.writeObject(lexicon);

			    oosLexiconReverse.writeObject(lexiconReverse);
			    
			    oosInvertedIndex.close();
			    oosLexicon.close();
			    oosLexiconReverse.close();

			} catch (IOException e) {
				System.err.println(e.getMessage());
			}
			
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
		
	}
	
	/*
	 * The method populates the inverted index's posting lists with a document's
	 * tokens (token ids) and their counts.   
	 * 
	 * Algorithm implemented by referencing pseudocode from lecture (Oct 3 2023)
	 *
	 * @param wordCounts  		A map of token id to word count for a document.
	 * @param docID  		  	A document's internal id
	 * @param invertedIndex 	A list of posting lists
	 */
	public static void addToPostingsList(Map<Integer, Integer> wordCounts, int docID, ArrayList<ArrayList<Integer>> invertedIndex){
		Set<Integer> termIDs = wordCounts.keySet();
		for(Integer termID : termIDs) {
			int count = wordCounts.get(termID);

			if(invertedIndex.size() <= termID) {
				ArrayList<Integer> postings = new ArrayList<Integer>();
				invertedIndex.add(postings);
			}
			
			invertedIndex.get(termID).add(docID);
			invertedIndex.get(termID).add(count);
		}
	}
	
	/*
	 * The method parses through the token IDs found in a document and
	 * counts the occurrences of each token ID.
	 *
	 * Algorithm implemented by referencing pseudocode from lecture (Sept 29 2023).
	 * 
	 * @param tokenIDs   An array list of token IDs to be parsed and counted
	 * @return A mapping of token ID to token ID count
	 */
	public static Map<Integer, Integer> countWords (ArrayList<Integer> tokenIDs){
		Map<Integer, Integer> wordCounts = new TreeMap<Integer, Integer>();
		
		for (Integer tokenID: tokenIDs) {
			if (wordCounts.containsKey(tokenID)) {
				wordCounts.put(tokenID, wordCounts.get(tokenID) + 1);
			} else {
				wordCounts.put(tokenID, 1);
			}
		}
		return wordCounts;
	}
	
	/*
	 * The method parses through tokens and generates a unique token id for
	 * each newly encountered token. Each id is mapped to the term in the lexicon. 
	 * 
	 * The returned list of ids is not unique as it is later used to count
	 * token occurrences in a document.
	 *
	 * Algorithm implemented by referencing pseudocode from lecture (Sept 29 2023).
	 * 
	 * @param tokens   			An array list of tokens to be parsed.
	 * @param lexicon			A map to map token ids to token terms
	 * @param lexiconReverse	A reverse map to map token terms to token ids
	 * @return A list of token ids corresponding to the tokens
	 */
	public static ArrayList<Integer> convertTokensToIDs(ArrayList<String> tokens, Map<String, Integer> lexicon, Map<Integer, String> lexiconReverse, boolean stem) {
		ArrayList<Integer> tokenIDs = new ArrayList<Integer>();
		
		for (String token : tokens) {
			if (stem == true) {
				token = PorterStemmer.stem(token);
			}
			if (lexicon.containsKey(token)) {
				tokenIDs.add(lexicon.get(token));
			} else {
				int id = lexicon.size();
				lexicon.put(token, id);
				lexiconReverse.put(id, token);
				tokenIDs.add(id);
			}
		}
		
		return tokenIDs;
	}
	
	/*
	 * The method breaks a string into a list of tokens. Only alphanumeric 
	 * characters are considered within tokens.
	 *
	 * Algorithm implemented by referencing pseudocode from lecture (Sept 29 2023).
	 * 
	 * @param line   The string representing the query.
	 * @return A list of alphanumeric tokens from the query.
	 */
	public static ArrayList<String> tokenizeText(String line) {
		line = line.toLowerCase();
		ArrayList<String> tokens = new ArrayList<String>();
		
		int start = 0;
		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			if (!Character.isLetter(c) && !Character.isDigit(c)) {
				if (start != i) {
					String token = line.substring(start, i);
					tokens.add(token);
				}
				start = i + 1;
			}
		}
		
		if (start != line.length()) {
			tokens.add(line.substring(start, line.length()));
		}
		
		return tokens;
	}
	
	/*
	 * The method writes a documents metadata to a file and tokenizes
	 * text from the headline, graphic and text XML tags.
	 *	 
	 * @param docDirectory 	A location to a document file
	 * @param docno  		A document's DOCNO value for its metadata file
	 * @param internalId 	A document's internal id for its metadata file
	 * @param date			A string value of the document's date for its metadata file
	 * @return 				An array list of all the tokens in the document's specified tags.
	 */
	public static ArrayList<String> writeDocMetadataAndGetTokens(File docDirectory, String docno, int internalId, String date) {
		try {	
			BufferedReader br = new BufferedReader(new FileReader(docDirectory+"/"+docno+".txt"));
			BufferedWriter bw = new BufferedWriter(new FileWriter(docDirectory+"/"+docno+"-metadata.txt")); // METADATA WRITER
			
			// Write DOCNO, internal id, and date to metadata file
			bw.write("docno: " + docno);
			bw.newLine();
			bw.write("internal id: " + internalId);
			bw.newLine();
			bw.write("date: " + date);
			bw.newLine();
			
			String line;
			boolean hasHeadline = false;
			ArrayList<String> tokens = new ArrayList<String>();
			
			while ((line = br.readLine()) != null) {
				if(line.equals("<HEADLINE>")){
					String headline = "";
					hasHeadline = true;
	
					while (!line.equals("</HEADLINE>")) {
						if (!line.contains("<") && !line.contains(">")) {
							headline += line;
							tokens.addAll(tokenizeText(line));
						}
						line = br.readLine();
					}

					bw.write("headline: " + headline);					
				}
				
				if(line.equals("<TEXT>")) {
					while (!line.equals("</TEXT>")) {
						if (!line.contains("<") && !line.contains(">")) {
							tokens.addAll(tokenizeText(line));
						}
						line = br.readLine();
					}
				}
				
				if(line.equals("<GRAPHIC>")) {
					while (!line.equals("</GRAPHIC>")) {
						if (!line.contains("<") && !line.contains(">")) {
							tokens.addAll(tokenizeText(line));
						}
						line = br.readLine();
					}
				}
				
			}
			
			if (!hasHeadline) {
				bw.write("headline: " + "NO HEADLINE");
			}
			
			br.close();
			bw.close();
			
			return tokens;
			
			
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
		
		return null;		
	}
	
	/*
	 * Returns a formatted version of the document date.
	 * 
	 * @param month A string representing the month (01-12).
	 * @param day   A string representing the day.
	 * @param year  A string representing the year (89 or 90).
	 * @return A string of the formatted date (e.g., January 1, 1990).
	 */
	public static String dateFormatter(String month, String day, String year) {
		String formattedDate = "";
		
		Map <Integer, String> monthMap = new HashMap<>(); 
		monthMap.put(1, "January");
		monthMap.put(2, "February");
		monthMap.put(3, "March");
		monthMap.put(4, "April");
		monthMap.put(5, "May");
		monthMap.put(6, "June");
		monthMap.put(7, "July");
		monthMap.put(8, "August");
		monthMap.put(9, "September");
		monthMap.put(10, "October");
		monthMap.put(11, "November");
		monthMap.put(12, "December");
				
		formattedDate = monthMap.get(Integer.parseInt(month)) + " " + Integer.parseInt(day) + ", 19" + year;
		
		return formattedDate;	
	}

}
