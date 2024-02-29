package component.Ranking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import component.IndexEngine.IndexEngine;

import component.Stemmer.PorterStemmer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.Comparator;
import java.lang.Math;

public class BM25Ranking {
	@SuppressWarnings({ "unchecked" })
	public static void main(String[] args) throws ClassNotFoundException, IOException {
		if(args.length < 3) {
			System.out.println("\nThe BM25 program requires three parameters: <path/root_directory_of_index> <path/queries_text_file> <path/write_output_filename>\n"
					+ "\nThe first parameter is the path to the location holding the set of documents. The second parameter is the path to the queries file. The"
					+ " third parameter is the path to where the program should write the results. You can also include an optional fourth parameter <stem> to "
					+ "apply Porter Stemming to the tokens. You must then ensure that the first parameter leads to directory that includes documents that also"
					+ " underwent Porter Stemming for best results. \n");
			System.exit(1);
		};
			
		// Directory of index
		File indexDir = new File(args[0]);
		File queryFile = new File(args[1]);
		File outputFile = new File(args[2]);
		
		if (!indexDir.exists()) {
			System.out.println("\nERROR: " + indexDir + " does not exist. Please enter an existing index directory\n");
			System.exit(2);
		}
		
		if (!queryFile.exists()) {
			System.out.println("\nERROR: " + args[1] + " does not exist. Please enter an existing file\n");
			System.exit(3);
		}
		
		if (outputFile.exists()) {
			System.out.println("\nERROR: " + outputFile + " is a file that already exists. Please provide a new file name.\n");
			System.exit(4);
		}

		boolean stemming = false;
		if (args.length == 4) {
			if(args[3].toLowerCase().equals("stem")) {
				stemming = true;
			}
		}

		try {
			// Reading in the lexicon and inverted index
            ObjectInputStream lexiconObject = new ObjectInputStream(new FileInputStream(indexDir + "/lexicon.ser"));
            ObjectInputStream invertedIndexObject = new ObjectInputStream(new FileInputStream(indexDir + "/invertedIndex.ser"));            
			Map<String, Integer> lexicon = (Map<String, Integer>) lexiconObject.readObject();
			ArrayList<ArrayList<Integer>> invertedIndex = (ArrayList<ArrayList<Integer>>) invertedIndexObject.readObject();
            
            Map <Integer, String> indexMap = new HashMap<>();
            parseDocIndex(indexDir, indexMap); // Parse index file into a map
            
            // Read doc length file
            Map <Integer, Integer> docLengths = new HashMap<>();
			Double averageDocLength = getDocLengthData(indexDir, docLengths);

			// Read query file
			BufferedReader br = new BufferedReader(new FileReader(args[1]));
			
			// Write to output file with results
			BufferedWriter bwResults = new BufferedWriter(new FileWriter(args[2]));
			ArrayList<String> queryResults = new ArrayList<String>();

			
			// Used to store the doc number and their BM25 score			
			String line;
			String query;
			ArrayList<String> queryTokens = new ArrayList<String>();
			int topicId;
			
			while ((line = br.readLine()) != null) {
				Map <Integer, Double> docScores = new HashMap<>();
				topicId = Integer.parseInt(line.replaceAll("\\s", "")); // Removes whitespace preventing integer parsing
				query = br.readLine();
				queryTokens = IndexEngine.tokenizeText(query); // Breaking query into tokens
				
				ArrayList<Integer> postingsList = new ArrayList<Integer>(); 
				
				for (int i = 0; i < queryTokens.size(); i++) {
					String token = queryTokens.get(i);
					if (stemming == true) {
						token = PorterStemmer.stem(token);
					}

					if(lexicon.containsKey(token)) {
						int tokenId = lexicon.get(token);
						postingsList = invertedIndex.get(tokenId);
						termAtATime(docScores, postingsList, docLengths, averageDocLength);
					}
				}
				
				// Sort the BM25 scores of each document in descending order
				ArrayList<Entry<Integer, Double>> sortedDocScores = entriesSortedByValues(docScores);
				
				// Limit documents to top 1000 scores
				int minVal = Math.min(sortedDocScores.size(), 1000);
				for (int i = 0; i < minVal; i++) {
		            Entry<Integer, Double> entry = sortedDocScores.get(i);
		            Integer key = entry.getKey();
		            Double value = entry.getValue();
		            String version = "lmtroperBM25noStem";
		            
		            if (stemming == true) {
		            	version = "lmtroperBM25stem";
		            }
		           
					String result = topicId + " Q0 " + indexMap.get(key) + " " + (i+1) + " " + value + " "+ version;
					queryResults.add(result);
		        }
				
				
				
			}
					
			// Reading output to the file
			for (int m = 0; m < queryResults.size(); m++) {
				String result = queryResults.get(m);				
				bwResults.write(result);
				
				if (m + 1 != queryResults.size()) {
					bwResults.newLine();
				}
			}
			
			lexiconObject.close();
			invertedIndexObject.close();
			
			br.close();
			bwResults.close();
			
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
	}


	/*
	 * Method used to sort a Map in descending order based on their value (key-value pair). The entries in the 
	 * Map are stored into an ArrayList of Entry objects<Integer, Double> where the integer represents the doc
	 * internal id and double represents their BM25 score for a query.
	 * 
	 * @param   Map of key-pair values representing a doc id and their BM25 score
	 * @return  An ArrayList of Entry objects in descending order by the Double value.
	 * 
	 * Code to sort map obtained stack overflow
	 * https://stackoverflow.com/questions/11647889/sorting-the-mapkey-value-in-descending-order-based-on-the-value
	 */
    public static <K, V extends Comparable<? super V>> ArrayList<Entry<K, V>> entriesSortedByValues(Map<K, V> map) {
        ArrayList<Entry<K, V>> sortedEntries = new ArrayList<>(map.entrySet());

        Collections.sort(sortedEntries, Comparator.comparing((Entry<K, V> e) -> e.getValue()).reversed());

        return sortedEntries;
    }
    
    /*
     * The method is used to perform the term at a time algorithm which is used to sum up documents'
     * BM25 for a specific query token. 
     * 
     * @param docScores			A map used to hold the summations of a document's BM25 score.
     * @param postingsList		The posting list for a query term (includes data on document ids and term frequency in a document)
     * @param docLengths		A map used to obtain the doc length (value) of a specific document (key)
     * @param averageDocLength  The average document length in the collection
     * 
     */
	public static void termAtATime(Map<Integer, Double> docScores, ArrayList<Integer> postingsList, 
			Map<Integer, Integer> docLengths, Double averageDocLength) {
		int collectionSize = docLengths.size();
		int numDocsWithTerm = postingsList.size()/2;
		

		for(int i = 0; i < postingsList.size(); i++) {
			int docId = postingsList.get(i);
			int tokenFreq = postingsList.get(i+1);
			int docLength = docLengths.get(docId);
			
			Double docPartialScore = calculateBM25 (tokenFreq, collectionSize, numDocsWithTerm, docLength, averageDocLength);
				
			// If the map contains the document internal id 
			if (docScores.containsKey(docId)) {
				Double prevScore = docScores.get(docId);
				docScores.put(docId, prevScore + docPartialScore);

			} else {
				// Else, add the new document id and its partial score to the map
				docScores.put(docId, docPartialScore);

			}
			
			i++;
		}
	}
	
	/*
	 * Method used to calculate the partial score of the BM25 (i.e., not the summation).
	 * 
	 * @param freq				The term frequency in a document (obtained from posting list)
	 * @param N					The number of documents in the collection
	 * @param n					The number of documents where the query term is found.
	 * @param docLength			The length of the document.
	 * @param avgDocLength		The average document length in the collection.
	 * @return 					The partial BM25 score for a token in a document 
	 * 
	 */
	public static Double calculateBM25(int freq, int N, int n, int docLength, Double avgDocLength) {
		Double k1 = 1.2;
		Double b = 0.75;
		
		// Length normalization
		Double K = k1*((1-b)+b*docLength/avgDocLength);
		
		// Term Frequency
		Double tf = freq/(freq + K);
		
		// Inverse Document Frequency
		Double idf_numerator = N - n + 0.5;
		Double idf_denom = n + 0.5;
		Double idf = Math.log(idf_numerator/idf_denom);
				
		return tf*idf;
	}

	
	/*
	 * The method parses the doc index file to populate a map of internal ids to
	 * DOCNOs. The code segment is taken from GetDoc.java.

	 * @param dir  		  The file representing the directory location of the index.
	 * @param indexMap 	  The map holding the mapping between internal ID and DOCNO.
	 */
	public static void parseDocIndex(File dir, Map <Integer, String> indexMap) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(dir+"/index.txt"));
			
			String line;
			int count = 0;
			
			while ((line = br.readLine()) != null) {
				indexMap.put(count, line);
				count += 1;
			}
			
			br.close();
			
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}	
	}
	
	/*
	 * Method used to create the mapping of doc length to doc internal id.
	 * 
	 * @param indexDir		Root directory where the doc-lengths.txt file is stored.
	 * @param docLengths 	Map used to store the internal id to doc length data.
	 * @return 				The average length of a document in the document collection.
	 * 
	 */
	public static Double getDocLengthData(File indexDir, Map <Integer, Integer> docLengths) {
		Double averageDocLength = 0.0;
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(indexDir+"/doc-lengths.txt"));
			
			String line;
			int count = 0;
			
			while ((line = br.readLine()) != null) {
				if (line.equals("")) {
					continue; // Used to ignore empty line at end of text file
				} else {
					int docLength = Integer.parseInt(line);
					averageDocLength += docLength;
					docLengths.put(count, docLength);
					count += 1;
				}
			}
			
			averageDocLength = averageDocLength / count;
			
			br.close();
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}	
		return averageDocLength;
	}
	
}
