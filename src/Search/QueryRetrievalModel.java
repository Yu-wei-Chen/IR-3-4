package Search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import Classes.Query;
import Classes.Document;
import IndexingLucene.MyIndexReader;

public class QueryRetrievalModel {
	
	protected MyIndexReader indexReader;
	int cLength = 0;
	
	public QueryRetrievalModel(MyIndexReader ixreader) throws IOException {
		indexReader = ixreader;
		for ( int i = 0; i < indexReader.getDocNum(); i++ ) {// indexReader.getDocNum()= 503473
        	//System.out.println(i+"-"+indexReader.docLength(i));
            cLength += indexReader.docLength(i); // get cLength
        }
	}
	
	/**
	 * Search for the topic's relevant documents. 
	 * The returned results (retrieved documents) should be ranked by the score (from the most relevant to the least).
	 * TopN specifies the maximum number of results to be returned.
	 * 
	 * @param aQuery The query to be searched for.
	 * @param TopN The maximum number of returned document
	 * @return
	 */
	
	public List<Document> retrieveQuery( Query aQuery, int TopN ) throws IOException {//List<Document>
		// NT: you will find our IndexingLucene.Myindexreader provides method: docLength()
		// implement your retrieval model here, and for each input query, return the topN retrieved documents
		// sort the docs based on their relevance score, from high to low
		
		List<Document> documents = new ArrayList<>();
		List<Document> documents1 = new ArrayList<>();
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
        
        // get Top N
        for (int t=0;t<TopN;t++){
        	documents1.add(documents.get(t));
        }
        
        return documents1;
	}
	
	
}