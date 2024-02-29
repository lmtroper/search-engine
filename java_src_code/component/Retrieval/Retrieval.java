package component.Retrieval;
import component.GetDoc.GetDoc;
import component.IndexEngine.IndexEngine;
import component.Ranking.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Map.Entry;
import java.util.PriorityQueue;

public class Retrieval {
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws ClassNotFoundException, IOException {	
		if(args.length < 1) {
			System.out.println("\nThe Retrieval program requires one parameter: <path/root_directory_of_index>\n"
					+ "\nThe first parameter is the path to the location holding the set of documents. \n");
			System.exit(1);
		};


		// Directory of index
		File indexDir = new File(args[0]);
		
		if (!indexDir.exists()) {
			System.out.println("\nERROR: " + indexDir + " does not exist. Please enter an existing index directory\n");
			System.exit(2);
		}
		
		try {
			// Reading in the lexicon and inverted index
            ObjectInputStream lexiconObject = new ObjectInputStream(new FileInputStream(indexDir + "/lexicon.ser"));
            ObjectInputStream invertedIndexObject = new ObjectInputStream(new FileInputStream(indexDir + "/invertedIndex.ser")); 
            Map<String, Integer> lexicon = (Map<String, Integer>) lexiconObject.readObject();
			ArrayList<ArrayList<Integer>> invertedIndex = (ArrayList<ArrayList<Integer>>) invertedIndexObject.readObject();
	        
			// Mapping doc internal ID to DOCNO
			Map <Integer, String> indexMap = new HashMap<>();
	        BM25Ranking.parseDocIndex(indexDir, indexMap); // Parse index file into a map
	        
	        // Mapping internal id to doc length + obtaining average doc length in collection
	        Map <Integer, Integer> docLengths = new HashMap<>();
			Double averageDocLength = BM25Ranking.getDocLengthData(indexDir, docLengths);

			interactiveRetrieval(indexDir, lexicon, invertedIndex, indexMap, docLengths, averageDocLength);
			
			lexiconObject.close();
			invertedIndexObject.close();
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
    	
	}
	
	/*
	 * The method enables interactive search retrieval by prompting a user for a query input.
	 * 
	 * The top results are ordered by highest to lowest BM25 score.
     * 
     * @param indexDir			File of index directory (root)
     * @param lexicon			Loaded lexicon object (hashmap mapping of token to token id)
     * @param invertedIndex		Loaded inverted index object (array list of posting lists)
     * @param docLengths  		A mapping of doc internal id to doc length
     * @param avgDocLength		The average document length in the collection
     * 
     */
	public static void interactiveRetrieval(File indexDir, Map<String, Integer> lexicon, 
			ArrayList<ArrayList<Integer>> invertedIndex, Map<Integer, String> indexMap, 
			Map<Integer, Integer> docLengths, Double avgDocLength) throws FileNotFoundException, IOException {
		
		Scanner scanner = new Scanner(System.in);
		String userInput;		

		Boolean newQuery = true;
		
		// Stores mapping of doc rank k to doc's internal id (for doc retrieval)
		Map <Integer, Integer> topResults = new HashMap<Integer, Integer>();
		
		ArrayList<String> queryTokens = new ArrayList<String>();

        do {
        	if (newQuery) {
                System.out.print("Enter your query (type 'Q' to quit): ");
                userInput = scanner.nextLine();
                if (userInput.equals("")) {
                	continue; // If empty input, prompt user again for query
                }
                newQuery = false;
        	} else {
        		System.out.print("\nEnter the rank of the document you'd like to see or enter 'N' to make a new query (type 'Q' to quit): ");
                userInput = scanner.nextLine();
                
                if (!userInput.equalsIgnoreCase("Q") && !userInput.equalsIgnoreCase("N")) {
	                try {
	                    int rank = Integer.parseInt(userInput);
	                    
	                    if (rank >= 1 && rank <= 10) {
	                    	System.out.println("\n");
	                    	GetDoc.getDocnoById(indexDir, topResults.get(rank));
	                    } else {
	                    	System.out.println("Not a valid rank number.");
	                    }
	                } catch (NumberFormatException e) {
	                    System.out.println("Invalid input.");
	                }
	                continue;
                }
	                    	
                if (userInput.equalsIgnoreCase("N")) {
                	newQuery = true;
                	continue;
                }
        	}
        	
        	if (!userInput.equalsIgnoreCase("Q")) {
        		
        		long startTime = System.currentTimeMillis();
        	
	        	queryTokens = IndexEngine.tokenizeText(userInput);
	        	Map <Integer, Double> docScores = new HashMap<>();
	        	
	        	// Iterate through all the query tokens
	        	for (int i = 0; i < queryTokens.size(); i++) {
					String token = queryTokens.get(i);
					// Check if tokens are in lexicon
					if(lexicon.containsKey(token)) {
						int tokenId = lexicon.get(token);
						// Get posting list for token and obtain BM25 score
						ArrayList<Integer> postingsList = invertedIndex.get(tokenId);
						BM25Ranking.termAtATime(docScores, postingsList, docLengths, avgDocLength);
					}
				}
	        	// Sort results in descending order by their BM25 scores
	        	ArrayList<Entry<Integer, Double>> sortedDocScores = BM25Ranking.entriesSortedByValues(docScores);
	        	
	        	// Generate and output result summaries for the top 10 ranked docs
				int minVal = Math.min(sortedDocScores.size(), 10);
	        	for (int i = 0; i < minVal; i++) {
		            Entry<Integer, Double> entry = sortedDocScores.get(i);
		            Integer key = entry.getKey();
		            
		            // Put rank and doc's internal id in map for user retrieval
	        		topResults.put(i+1, key);

	        		printDocResultSummary(indexDir, i+1, indexMap.get(key), queryTokens);
		            System.out.println("\n");
		            
		        }
	        	
	        	long endTime = System.currentTimeMillis();
	        	long elapsedTime = endTime - startTime;
	        	
	            System.out.println("\nRetrieval took " + (double) elapsedTime / 1000.0 + " seconds\n");
        	}
        	
        } while (!userInput.equalsIgnoreCase("Q"));
        
        scanner.close();
		
	}
	
	/*
	 * The method takes the top results and outputs their metadata (date and docno) along with
	 * their result summary (two sentences).
	 *      
     * @param indexDir			File of index directory (root)
     * @param rank				The rank of a document
     * @param docno				The docno of a document
     * @param queryTokens  		The query tokens from the user's query
     * 
     */
	public static void printDocResultSummary(File indexDir, int rank, String docno, ArrayList<String> queryTokens) throws FileNotFoundException, IOException {
		String month = docno.substring(2,4);
		String day = docno.substring(4,6);
		String year = docno.substring(6,8);
		
		String date = "";
		String headline = "";
		
		String docPath = indexDir+"/"+year+"/"+month+"/"+day+"/";
		BufferedReader brMeta = new BufferedReader(new FileReader(docPath+docno+"-metadata.txt"));
		
		String line;
		while((line = brMeta.readLine()) != null){
			int data = line.indexOf(":");
			// Extract the date from metadata
			if (line.substring(0, data).equals("date")) {
				date = line.substring(data + 2, line.length());
			}
			
			// Extract the headline from metadata (if it exists)
			if (line.substring(0, data).equals("headline")) {
				if (!line.substring(data, line.length()).equals("NO HEADLINE")) {
					headline = line.substring(data + 2, line.length());
				}
			}
		}
	
		brMeta.close();
		
		PriorityQueue<String> scoredSentences = generateResultSummary(docPath, docno, queryTokens);
		
		String firstSentence = scoredSentences.poll();
		firstSentence = firstSentence.substring(firstSentence.indexOf('-') + 1).trim();
		String secondSentence = scoredSentences.poll();
		secondSentence = secondSentence.substring(secondSentence.indexOf('-') + 1).trim();
		
		if (headline == "") {
			String[] s = firstSentence.split(" ");
			if (s.length > 15) {
				headline = String.join(" ", Arrays.copyOfRange(s, 0, 15));
				headline += "...";
			} else {
				headline = firstSentence;
			}
		}
		
		System.out.println(rank + ". " + headline + " (" + date + ")");
		System.out.println(firstSentence + " " + secondSentence + " ("+docno+")");
	}
	
	/*
	 * The method generates the result summary to be printed by reading the document
	 * and extracting the document content to be scored. The score is appended into a
	 * Priority Queue.
	 *      
     * @param docPath				Path to document within root index directory
     * @param docno					The docno of document
     * @param queryTerms			The list of query terms inputed by the user
     * @return scoredSentences  	Returns a Priority Queue of sentences sorted by highest to lowest score
	 */
	public static PriorityQueue<String> generateResultSummary(String docPath, String docno, ArrayList<String> queryTerms) throws IOException {
		BufferedReader brDoc = new BufferedReader(new FileReader(docPath+docno+".txt"));
		PriorityQueue<String> scoredSentences = new PriorityQueue<>((s1, s2) -> Integer.compare(
		        Integer.parseInt(s2.split(" - ")[0].trim()),
		        Integer.parseInt(s1.split(" - ")[0].trim())
		));
		
		String line;
		String sentence = "";
		int score = 0;
		boolean flag = false;
		int lineCount = 0;
		
		while((line = brDoc.readLine()) != null){
			if(line.contains("<TEXT>") || line.contains("<GRAPHIC>")) {
				// Set flag to true when we reach content of document
				flag = true;
				continue;
			}
			
			if(line.contains("</TEXT>") || line.contains("</GRAPHIC>")) {
				flag = false;
				continue;
			}
			
			if(flag && !line.contains("<") && !line.contains(">")) {
				// Append <P>...</P> tag content together
				sentence += line + " ";
				
			} else if (flag) {
				int start = 0;
				int stop = 0;

				while (stop < sentence.length()) {
					// Parse through each character
					char currChar = sentence.charAt(stop);
					
					// Stop when you find a stop point
				    if(currChar == '.'|| currChar == '?' || currChar == '!') {
				    	String[] s = sentence.substring(start, stop).split(" ");
				    	
				    	if (s.length <= 5) {
				    		// Skip over short sentences (5 words or less)
				    		start = stop + 1;
					    	stop = start;
				    		continue;
				    	}

				    	score += scoreSentence(sentence.substring(start, stop), queryTerms);
				    	lineCount += 1;
				    	
				    	// Increase score if line is first or second sentence in document
				    	if (lineCount == 1) {
				    		score += 2;
				    	} else if (lineCount == 2) {
				    		score += 1;
				    	}
				    	
				    	scoredSentences.add(score + " - " + sentence.substring(start, stop + 1));
				    	start = stop + 1;
				    	stop = start;
				    	score = 0;
				    }
				    stop++;
				}
				sentence = "";
			}
		}
		brDoc.close();		
		
		return scoredSentences;
	}

	/*
	 * The method returns a score for a sentence based on its relation to a query.
	 * 
	 * The score sums up the number of occurrences of each query term (c), the number
	 * of distinct query terms found (d), and the largest contiguous run of the query
	 * terms. 
	 *      
     * @param sentence			The sentence to be scored
     * @param queryTerms		The query terms inputed by the user
     * @return score			The score of the sentence
	 */
	public static int scoreSentence(String sentence, ArrayList<String> queryTerms) {
		ArrayList<String> tokenizedText = IndexEngine.tokenizeText(sentence);
		Map<String, Integer> termCounts = new HashMap<>();
    	
		boolean termFound = false;
		ArrayList<String> contiguousTerms = new ArrayList<String>();
		int contiguousTermCount = 0;

		for (String token: tokenizedText) {
			for (String term : queryTerms) {
				termFound = false;					
	            if (token.equals(term)) {
	            	termFound = true;
	            	contiguousTerms.add(term);
	            	if(termCounts.containsKey(term)) {
	            		int prevScore = termCounts.get(term);
	            		termCounts.put(term, prevScore + 1);
	            	}else {
	            		termCounts.put(term, 1);
	            	}
	                break;
	            }
	        }
			
			if (!termFound) {
				// Only factor into score when there's at least two terms in the list
				if (contiguousTerms.size() >= 2) {
					contiguousTermCount = Math.max(contiguousTermCount, contiguousTerms.size());
				}
				contiguousTerms.clear();
			}
		}
		
		// Count up the total number of query terms found
		int c = 0;
        for (int value : termCounts.values()) {
            c += value;
        }
		
        // Number of distinct query terms found
		int d = termCounts.size();		

		return c+d+contiguousTermCount;
		
	}

}
