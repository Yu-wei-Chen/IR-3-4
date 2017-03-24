package Search;


import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;

import Classes.Path;
import Classes.Query;
import Classes.Stemmer;

public class ExtractQuery {

	ArrayList<String> titleInfo = new ArrayList<String>();
	ArrayList<String>  topicId = new ArrayList<String>();
	int q = 0;
	public ExtractQuery() throws IOException {
		//you should extract the 4 queries from the Path.TopicDir
		//NT: the query content of each topic should be 1) tokenized, 2) to lowercase, 3) remove stop words, 4) stemming
		//NT: you can simply pick up title only for query, or you can also use title + description + narrative for the query content.
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(Path.TopicDir)));
		
		String line = reader.readLine();
		
		int p = 0;
		while ( line != null ) {
			
			if ( line.indexOf( "<num>" ) != -1 ) {
				String queryid = line.substring(14);
				//System.out.println(queryid); // query ID
				topicId.add(queryid);
			}
			
	        if ( line.indexOf( "<title>" ) != -1 ) { 
	   
	            String content = line.substring(7); // start after "<title>"
	            
	            // StopWord remove
	            HashSet<String> object = new HashSet <String> (); 
	        	FileInputStream stopwordfile = null;
	        	BufferedReader reader1;
	        	stopwordfile = new FileInputStream(Path.StopwordDir); // get stopword list
	    		reader1 = new BufferedReader(new InputStreamReader(stopwordfile));    
	    		String line1 = reader1.readLine();
	    		while (line1 != null){
	    			object.add(line1); // add to hashset
	    			line1 = reader1.readLine(); // go to next line
	    		}
	            
	            // Tokenizer
	            String word = null;
	            String tokenizerword[];
	            int i = 0;
	        	int wordlength;
	        	String tokenizer =new String(content);
	    		tokenizer = tokenizer.replaceAll("[\\pP‘’“”]", ""); // remove punctuations
	    		tokenizerword = tokenizer.split(" "); // split word
	    		wordlength = tokenizerword.length; // the size of the tokenizerword
	        	
	    		String wr=""; // tem storage
	           
	    		for (int k=0;k<wordlength;k++){
	    			
	    			word = tokenizerword[k];
					word = word.toLowerCase(); // change to lowercase
					
					if(!object.contains(word)) { // compare if word is contained in hashset(stop word) or not 
						
						// stemming
						String str="";
						char[] charArray = word.toCharArray(); // change String to Char[] 
						Stemmer stemming = new Stemmer();
						stemming.add(charArray, charArray.length); // call add() in stemmer
						stemming.stem(); // call stem() to do stemming 
						str = stemming.toString(); // change back to String 

						wr = wr+ " "+str; // sum of results
					}
					
				}
	            //System.out.println(wr);
	            //System.out.println();
	    		titleInfo.add(wr);
	            p++;
	        }
	        line = reader.readLine();
	    }
        
		
	}
	
	public boolean hasNext()
	{
		if (q < topicId.size()){
			q++;
			return true; 
		}
		return false;
	}
	
	public Query next()
	{
		// set GetTopicId & GetQueryContent
		Query aQuery = new Query();
		aQuery.SetQueryContent(titleInfo.get(q-1)); // q-1 because q++ before here
		aQuery.SetTopicId(topicId.get(q-1));
		return aQuery;
	}
}
