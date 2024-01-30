/* Name: Larissa Troper 
 * MSCI 541 Homework 1 - Program 2: getDoc
 * Date Written: 25 September 2023
 * Description: This program will display a queried document's metadata and contents.
 * Input: File path to set of documents, specification of parameter index (id or docno), 
 * and the internal id number or docno string.
 * Output: Document metadata and contents.
*/

package component.GetDoc;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

public class GetDoc {

	public static void main(String[] args) {
		if(args.length < 3) {
			System.out.println("\nThe GetDoc program requires three parameters: <path/root_directory_of_latimes_documents> <'id' or 'docno'> <The integer ID value or the DOCNO>\n"
					+ "\nThe first parameter is the path to the location holding the set of latimes documents. The second parameter is the string 'id' or 'docno'. The"
					+ " third parameter is the internal id or docno of the document being fetched. \n");
			System.exit(1);
		};
		
		File rootDir = new File(args[0]);
		
		if (!rootDir.exists()) {
			System.out.println("\nERROR: " + rootDir + " does not exist. Please enter an existing directory\n");
			System.exit(2);
		}
		
		if (args[1].equals("id")) {
			int id = 0;
			
			try {
				id = Integer.parseInt(args[2]);
			} catch (NumberFormatException e) {
				System.err.println("\nERROR: '" + args[2] + "' is not a valid integer.\n");
				System.exit(3);
			}
			getDocnoById(rootDir, id);
			
		} else if (args[1].equals("docno")) {
			getDataByDocno(rootDir, args[2]);
		} else {
			System.out.println("\nERROR: '" + args[1] + "' is not a valid parameter. Please specify id or docno as your second argument.\n");
			System.exit(4);
		}	
	}
	
	/*
	 * The method uses the index mapping within the root directory to find the docno
	 * of a document corresponding with an internal id. 
	 * 
	 * @param rootDir  The file representing the root directory of the document set.
	 * @param id  	   An integer value representing the internal id of a document.
	 */
	public static void getDocnoById(File rootDir, int id) {
		try {
			// Parse the index mapping file into a dictionary
			BufferedReader br = new BufferedReader(new FileReader(rootDir+"/index.txt"));
			
			Map <Integer, String> indexMap = new HashMap<>(); 
			String line;
			int count = 0;
			
			while ((line = br.readLine()) != null) {
				indexMap.put(count, line);
				count += 1;
				
			}
						
			if (!indexMap.containsKey(id)) {
				System.out.println("\nERROR: There is no document corresponding to the provided internal id: " + id + "\n");
				System.exit(5);
			}
			
			getDataByDocno(rootDir, indexMap.get(id));
			br.close();
			
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
	}
	
	
	/*
	 * The method uses a document's docno to fetch the document's metadata and contents
	 * within the root directory and outputs the data.
	 * 
	 * @param rootDir  The file representing the root directory of the document set.
	 * @param docno    A string representing the docno of a unique document in the set.
	 */
	public static void getDataByDocno(File rootDir, String docno) {
		try {
			
			if (docno.length() < 13) {
				System.out.println("\nERROR: '" + docno + "' is not a valid DOCNO. Please provide a valid DOCNO (LAXXXXXX-XXXX) where X represents whole numbers.\n");
				System.exit(6);
			}
			
			String month = docno.substring(2,4);
			String day = docno.substring(4,6);
			String year = docno.substring(6,8);
			
			String basePath = rootDir+"/"+year+"/"+month+"/"+day+"/"; // Document parent directory 
			
			if (!new File(basePath+docno+".txt").exists() 
					|| !new File(basePath+docno+"-metadata.txt").exists()) {
				System.out.println("\nERROR: There is no document corresponding to the DOCNO: '" + docno + "'\n");
				System.exit(5);
			}
			
			BufferedReader brMeta = new BufferedReader(new FileReader(basePath+docno+"-metadata.txt"));
			
			String line;
			while((line = brMeta.readLine()) != null){
				System.out.println(line); // Print out metadata
			}
			
			brMeta.close();
			
			BufferedReader brDoc = new BufferedReader(new FileReader(basePath+docno+".txt"));
			
			System.out.println("raw document:");
			
			while((line = brDoc.readLine()) != null){
				System.out.println(line); // Print out document content
			}
			
			brDoc.close();
			
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
	}

}