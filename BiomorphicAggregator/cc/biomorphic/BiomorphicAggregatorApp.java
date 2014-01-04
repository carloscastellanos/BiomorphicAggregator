package cc.biomorphic;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
/**
 * 
 */

/**
 * @author carlos
 *
 */
public class BiomorphicAggregatorApp
{
	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		// read from a text file
		FileInputStream fin = null;
		// ArrayList to hold the items from the file
		ArrayList<String> items = new ArrayList<String>();
		// ArrayList to hold the serch terms
		//ArrayList<String> terms = new ArrayList<String>();
		/*
		if(args.length != 1) {
			System.out.println("usage: java BiomorphicAggregatorApp <file>");
			System.exit(1);
		}
		*/
		System.out.println("BiomorphicAggregatorApp starting...");
		try {
			fin =  new FileInputStream("data/terms.txt");
			Reader in = new InputStreamReader(fin);
			BufferedReader bin = new BufferedReader(in);
			System.out.println("Search terms file loaded...");
			String entry = null;
			while((entry = bin.readLine()) != null) {
				items.add(entry);
			}
		} catch(IOException e) {
			System.out.println("Exception: " + "error retrieving file " + e);
			System.exit(1);
			
		}
		
		/*
		// get random numbers (w/no repeats) to use for getting 3 unique search terms
		int[] randomInts = RandomNoRepeat.generate(items.size()-2, items.size()-1);
		for(int i=0; i<randomInts.length; i++) {
			terms.add(items.get(randomInts[i]));
			System.out.println("Added "+items.get(randomInts[i])+" to the search list");
		}
		*/
		
		System.out.println("Ready to search and parse...");
		//SearchAndParse sap = new SearchAndParse(terms);
		SearchAndParse sap = new SearchAndParse(items);
		try {
			sap.doSearchAndParse();
		} catch(Exception e) {
			System.out.print("Error searching/parsing feeds! " + e.getMessage());
			//System.exit(1);
		}
		
		System.exit(0);
	}

}
