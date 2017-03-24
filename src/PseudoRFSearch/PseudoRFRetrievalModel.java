package PseudoRFSearch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import Classes.Document;
import Classes.Query;
import IndexingLucene.MyIndexReader;

public class PseudoRFRetrievalModel {

	
	protected MyIndexReader indexReader;
	int cLength = 0; // Total length of the whole collection
	
	public PseudoRFRetrievalModel(MyIndexReader ixreader) throws IOException {
		indexReader = ixreader;
		for ( int i = 0; i < indexReader.getDocNum(); i++ ) {// indexReader.getDocNum()= 503473
            cLength += indexReader.docLength(i); // get cLength
        }
	}
	/**
	 * Search for the topic with pseudo relevance feedback in 2017 spring assignment 4. 
	 * The returned results (retrieved documents) should be ranked by the score (from the most relevant to the least).
	 * 
	 * @param aQuery The query to be searched for.
	 * @param TopN The maximum number of returned document
	 * @param TopK The count of feedback documents
	 * @param alpha parameter of relevance feedback model
	 * @return TopN most relevant document, in List structure
	 */
	public List<Document> RetrieveQuery( Query aQuery, int TopN, int TopK, double alpha) throws Exception {	
		// this method will return the retrieval result of the given Query, and this result is enhanced with pseudo relevance feedback
		// (1) you should first use the original retrieval model to get TopK documents, which will be regarded as feedback documents
		// (2) implement GetTokenRFScore to get each query token's P(token|feedback model) in feedback documents
		// (3) implement the relevance feedback model for each token: combine the each query token's original retrieval score P(token|document) with its score in feedback documents P(token|feedback model)
		//     P(token|document')=αP(token|document)+(1-α)P(token|feedback model)
		// (4) for each document, use the query likelihood language model to get the whole query's new score, P(Q|document)=P(token_1|document')*P(token_2|document')*...*P(token_n|document')
		
		
		//get P(token|feedback documents)
		HashMap<String,Double> TokenRFScore=GetTokenRFScore(aQuery,TopK); 
		
		
		// sort all retrieved documents from most relevant to least, and return TopN
		//List<Document> results = new ArrayList<Document>();
		//return results;
		
		List<Document> documents = new ArrayList<>(); // result unsort
		List<Document> documents1 = new ArrayList<>(); // result sort and pick up TopN
		String queryall = aQuery.GetQueryContent(); // get query content
		String[] query_token = queryall.split(" "); // get query token

		double [] cf1s = new double[query_token.length]; //cf
		int [][] postingList; 
		
		Map<Integer, HashMap<String, Integer>> docx = new HashMap<Integer, HashMap<String, Integer>>(); 
		
		for (int q=0;q<query_token.length;q++ ) {
			cf1s[q] = (double) indexReader.CollectionFreq(query_token[q]); //cf
			postingList = indexReader.getPostingList(query_token[q]); // postinglist
			
			if(postingList != null){
				for (int j = 0; j < postingList.length; j++) {
					int docid = postingList[j][0]; // docid in posting list for this query token
					if(docx.containsKey(docid)){ 
						docx.get(docid).put(query_token[q], postingList[j][1]);
					}else{
						HashMap<String, Integer> tem = new HashMap<String, Integer>();
						tem.put(query_token[q], postingList[j][1]); // inside hashmap
						docx.put(docid, tem); // double hashmap
					}
				}		
			}
		}
		

        // set u value
        double u = 2000;
        
        // calculate score of each document  
        for ( int i = 0; i < indexReader.getDocNum(); i++ ) { // indexReader.numDocs()
            int dLength = indexReader.docLength(i);
            double score = 1;
            for (int k=0;k<query_token.length;k++ ) {
                int cwd = 0;
                double cf = cf1s[k];
                if ( cf == 0 ){
                	continue; // query_token not in index
                }
                if(docx.containsKey(i) && docx.get(i).containsKey(query_token[k])){ // prevent not found situation
                	cwd = docx.get(i).get(query_token[k]);
                }
                //P(token|document')=αP(token|document)+(1-α)P(token|feedback model)
                score = score * ( ( alpha * ( ( cwd + u*( cf/cLength ) ) / ( dLength + u ) ) ) + ( (1-alpha) * TokenRFScore.get(query_token[k])) );
                 
            }
            
            
            //store documents score
            Document aDocument = new Document(Integer.toString(i), indexReader.getDocno(i), score); 
			documents.add(aDocument);
            
        }
        
        // sort array list
        Collections.sort(documents, new Comparator<Document>(){
        	@Override
        	public int compare(Document o1, Document o2) {
        		double temp = o1.score() - o2.score();
        		if (temp>0){
        			return 1;
        		}else if (temp<0){
        			return -1;
        		}else{
        			return 0;
        		}
        	}   
        	});
        
        // reverse array list 
        Collections.reverse(documents);
        
        // get Top N
        for (int t=0;t<TopN;t++){
        	documents1.add(documents.get(t));
        }
        
        return documents1;
		
	}
	
	public HashMap<String,Double> GetTokenRFScore(Query aQuery,  int TopK) throws Exception
	{
		// for each token in the query, you should calculate token's score in feedback documents: P(token|feedback documents)
		// use Dirichlet smoothing
		// save <token, score> in HashMap TokenRFScore, and return it
		HashMap<String,Double> TokenRFScore=new HashMap<String,Double>();
		
		List<Document> documents = new ArrayList<>(); // unsort
		List<Document> documents1 = new ArrayList<>(); // sort and pick Top K as feedback
		String queryall = aQuery.GetQueryContent();
		String[] query_token = queryall.split(" ");

		double [] cf1s = new double[query_token.length]; 
		int [][] postingList;
		
		Map<Integer, HashMap<String, Integer>> docx = new HashMap<Integer, HashMap<String, Integer>>(); 
		
		for (int q=0;q<query_token.length;q++ ) {
			cf1s[q] = (double) indexReader.CollectionFreq(query_token[q]); //cf
			postingList = indexReader.getPostingList(query_token[q]); // postinglist
			
			if(postingList != null){
				for (int j = 0; j < postingList.length; j++) {
					int docid = postingList[j][0];
					if(docx.containsKey(docid)){
						docx.get(docid).put(query_token[q], postingList[j][1]);
					}else{
						HashMap<String, Integer> tem = new HashMap<String, Integer>();
						tem.put(query_token[q], postingList[j][1]); // inside hashmap
						docx.put(docid, tem); // double hashmap
					}
				}		
			}
		}
		
		
        // set u value
        double u = 2000;
        
        // calculate score of each document  
        for ( int i = 0; i < indexReader.getDocNum(); i++ ) { // indexReader.numDocs()
            int dLength = indexReader.docLength(i);
            double score = 1;
            for (int k=0;k<query_token.length;k++ ) {
                int cwd = 0;
                double cf = cf1s[k];
                if ( cf == 0 ){
                	continue; // query_token not in index
                }
                if(docx.containsKey(i) && docx.get(i).containsKey(query_token[k])){ // prevent not found situation
                	cwd = docx.get(i).get(query_token[k]);
                }
                score = score * ( ( cwd + u*( cf/cLength ) ) / ( dLength + u ) ); 
            }
            
            //store documents score
            Document aDocument = new Document(Integer.toString(i), indexReader.getDocno(i), score); 
			documents.add(aDocument);
            
        }
        
        // sort array list
        Collections.sort(documents, new Comparator<Document>(){
        	@Override
        	public int compare(Document o1, Document o2) {
        		double temp = o1.score() - o2.score();
        		if (temp>0){
        			return 1;
        		}else if (temp<0){
        			return -1;
        		}else{
        			return 0;
        		}
        	}   
        	});
        
        // reverse array list 
        Collections.reverse(documents);
        
        HashSet<String> RFid=new HashSet <String> (); // store top 100 document docid

        // get Top K for feedback
        for (int t=0;t<TopK;t++){
        	documents1.add(documents.get(t));
        	String temp = documents.get(t).docid(); // put top 100 docid into hashset 
        	RFid.add(temp);
        }
		
        Double CLength_top100 = 0.0;
        
        // get top 100 collection length
        for(String str : RFid) {
        	int foo = Integer.parseInt(str); // transfer id to int type for seaching document length
        	int dLength_top100 = indexReader.docLength(foo);
        	CLength_top100 = CLength_top100 + dLength_top100; // sum the top 100 collection length
        }
        
        // pseudoRF find top 100 P(token|feedback model) 
        for (int q=0;q<query_token.length;q++ ) {

			postingList = indexReader.getPostingList(query_token[q]); // postinglist
			Double tokenfreq = 0.0; // token freauency
			if(postingList != null){
				for (int j = 0; j < postingList.length; j++) {
					int docid = postingList[j][0];
					if(RFid.contains(String.valueOf(docid))){ // transfer docid to string type
						tokenfreq = tokenfreq+postingList[j][1]; // sum token frequency
					}
				}		
			}
			
			Double temp1 =  tokenfreq/CLength_top100; // P(token|feedback model) 
			TokenRFScore.put(query_token[q], temp1); // put P(token|feedback model) into hashmap  

		}


		return TokenRFScore;
	}
	
	
}