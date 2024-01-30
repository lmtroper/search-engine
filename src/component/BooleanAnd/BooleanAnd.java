/* Name: Larissa Troper
 * MSCI 541 Homework 2 - Program 2: BooleanAnd
 * Date Written: 16 October 2023
 * Description: This program performs Boolean AND retrieval for a list of queries.
 * Input: The file path of the index directory, the file path of the query file, and a file pathname
 * to write the output.
 * Output: Output file of the query results.
*/

package component.BooleanAnd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import component.IndexEngine.IndexEngine;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.Comparator;

public class BooleanAnd {

	@SuppressWarnings({ "unchecked" })
	public static void main(String[] args) throws ClassNotFoundException, IOException {
		if(args.length < 3) {
			System.out.println("\nThe BooleanAnd program requires three parameters: <path/root_directory_of_index> <path/queries_text_file> <path/write_output_filename>\n"
					+ "\nThe first parameter is the path to the location holding the set of documents. The second parameter is the path to the queries file. The"
					+ " third parameter is the path to where the program should write the results. \n");
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
		
		try {
			// Reading in the lexicon and inverted index
            ObjectInputStream lexiconObject = new ObjectInputStream(new FileInputStream(indexDir + "/lexicon.ser"));
            ObjectInputStream invertedIndexObject = new ObjectInputStream(new FileInputStream(indexDir + "/invertedIndex.ser"));
			Map<String, Integer> lexicon = (Map<String, Integer>) lexiconObject.readObject();
			ArrayList<ArrayList<Integer>> invertedIndex = (ArrayList<ArrayList<Integer>>) invertedIndexObject.readObject();
            
            Map <Integer, String> indexMap = new HashMap<>();
            parseDocIndex(indexDir, indexMap); // Parse index file into a map

			// Read query file
			BufferedReader br = new BufferedReader(new FileReader(args[1]));
			
			// Write to output file with results
			BufferedWriter bwResults = new BufferedWriter(new FileWriter(args[2]));
			ArrayList<String> queryResults = new ArrayList<String>();
			
			String line;
			String query;
			ArrayList<String> queryTokens = new ArrayList<String>();
			int topicId;
			
			while ((line = br.readLine()) != null) {
				topicId = Integer.parseInt(line.replaceAll("\\s", "")); // Removes whitespace preventing integer parsing
				query = br.readLine();
				queryTokens = IndexEngine.tokenizeText(query); // Breaking query into tokens
				
				ArrayList<ArrayList<Integer>> postingsList = new ArrayList<ArrayList<Integer>>(); 
				
				boolean isTermFound = true; // Used to flag if a token does not exist in lexicon 

				for (int i = 0; i < queryTokens.size(); i++) {
					String token = queryTokens.get(i);
					if(!lexicon.containsKey(token)) {
						isTermFound = false; // Token not found in lexicon
						break;
					} else {
						int tokenId = lexicon.get(token);
						postingsList.add(invertedIndex.get(tokenId));
					}
				}
				
				
				if (isTermFound && postingsList.size() > 0) {

					ArrayList<Integer> docSet = new ArrayList<Integer>(); // Stores IDs of docs that have all query tokens
					
					if (postingsList.size() > 1) {
						
						// Code from https://stackoverflow.com/questions/3477272/java-how-to-sort-list-of-lists-by-their-size
						// Sorts the array of postings lists from smallest to biggest size
						Collections.sort(postingsList, new Comparator<ArrayList<Integer>>(){
						    public int compare(ArrayList<Integer> a1, ArrayList<Integer> a2) {
						        return a1.size() - a2.size();
						    }
						});
						
						
						for (int j = 0; j < postingsList.size(); j++) {
							// Initial intersect algorithm (using true flag)
							if (j == 0) {
								docSet = intersect(postingsList.get(j), postingsList.get(j+1), true);
								j += 1;
							} else {
								docSet = intersect(docSet, postingsList.get(j), false);
							}
						}

						
					} else {
						// For single query terms, need to strip the term counts
						ArrayList<Integer> singleQueryResult = postingsList.get(0);
						for (int i = 0; i < singleQueryResult.size(); i++) {
							int docId = singleQueryResult.get(i);
							docSet.add(docId); // Store the doc ids into doc set
							i += 1;
						}
					}

					
					// Storing the query's results
					int rank = 1;
					for (int k = 0; k < docSet.size(); k ++) {
						int docId = docSet.get(k);
						int score = docSet.size() - rank;
					
						String result = topicId + " Q0 " + indexMap.get(docId) + " " + rank + " " + score + " lmtroperAND";
						queryResults.add(result);
						
						rank += 1;
					}
					
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
	 * The method takes two lists and returns a merged list based on common doc IDs. If
	 * a flag of true is passed, the two array lists passed are posting lists and need to
	 * be incremented by two's (doc ID, count of term). Otherwise, the first array list 
	 * represents an array of doc IDs and only needs to be incremented by 1 each loop.
	 *
	 * Algorithm implemented by referencing pseudocode from lecture (Oct 3 2023)
	 *
	 * @param p1  		  		A list representing the shorter of the two posting lists.
	 * @param p2  		  		A list representing the longer of the two posting lists..
	 * @param initialIntersect 	Boolean to flag for the first intersection
	 * @return An array of the doc IDs that appeared in both array lists.  
	 */
	public static ArrayList<Integer> intersect(ArrayList<Integer> p1, ArrayList<Integer> p2, boolean initialIntersect) {
		
		int i = 0;
		int j = 0;
		
		ArrayList<Integer> answers = new ArrayList<Integer>();
		
		int increase;
		if (initialIntersect) {
			increase = 2;
		} else {
			increase = 1;
		}
		
		while (i != p1.size() && j != p2.size()) {
			if (p1.get(i).equals(p2.get(j))) {
				answers.add(p1.get(i));
				i += increase;
				j += 2;
			} else if (p1.get(i) < p2.get(j)) {
				i += increase;
			} else {
				j += 2;
			}
		}
		return answers;
	}
	

	
}
