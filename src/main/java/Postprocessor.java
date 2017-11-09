import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.io.File;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;

public class Postprocessor {
	private static Model RDFGraph;
	
	// CONSTRUCTEUR
	public Postprocessor() {
		RDFGraph = ModelFactory.createMemModelMaker().createDefaultModel();
	}
	
	public boolean loadModelFromFile(String filename) throws IOException {
		if(filename == null) return false;
		
		InputStream in = new FileInputStream(new File(filename));
		
		RDFGraph.read(in,"");
		in.close();
		
		return true;
	}
	
	public boolean writeOutputFile(String content, String filename) throws IOException {
		FileOutputStream fop = null;
		File file;

		try {
			file = new File(filename);
			fop = new FileOutputStream(file);
			
			if (!file.exists()) {file.createNewFile();}

			byte[] contentInBytes = content.getBytes();

			fop.write(contentInBytes);
			fop.flush();
			fop.close();

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (fop != null) {
					fop.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return true;
	}
	
	public boolean process(String filename) throws Exception {
		/*Model model = ModelFactory.createDefaultModel();
		String queryString = "SELECT * WHERE {    ?person <http://xmlns.com/foaf/0.1/>:name ?name. ?person <http://xmlns.com/foaf/0.1/>:mbox ?email.}" ;
		Query query = QueryFactory.create(queryString);
		QueryExecution qexec = null;
		try {
			qexec = QueryExecutionFactory.create(query, model);
			
			ResultSet results = qexec.execSelect();
			for ( ; results.hasNext() ; ) {	
				QuerySolution soln = results.nextSolution();
				RDFNode x = soln.get("varName");       // Get a result variable by name.
				Resource r = soln.getResource("VarR"); // Get a result variable - must be a resource
				Literal l = soln.getLiteral("VarL");   // Get a result variable - must be a literal
			}
		} finally {
			qexec.close();
		}*/
		String processedText = "";
		
		loadModelFromFile(filename);
		
		ResIterator iRes = RDFGraph.listSubjects();
		while(iRes.hasNext()) {
		    Resource r = iRes.nextResource();
		    ArrayList<Property> seenStatements = new ArrayList<Property>();
		    
		    processedText += r.getLocalName().replace('_', ' ') + "'s " + '\n';
		    
		    StmtIterator iProp = r.listProperties();
		    while(iProp.hasNext()) {
		    	Statement s = iProp.nextStatement();
		    	
		    	if(!seenStatements.contains(s.getPredicate())) {
		    		processedText += "	" + s.getPredicate().getLocalName().replace('_', ' ') + " is ";
			    	
			    	StmtIterator iSameProp = r.listProperties();
			    	while(iSameProp.hasNext()) {
			    		Statement sSame = iSameProp.nextStatement();
			    		if(sSame.getPredicate().equals(s.getPredicate())) {
			    			if(sSame.getObject().isResource())
			    				processedText += sSame.getObject().asResource().getLocalName().replace('_', ' ') + ", ";
			    			else if(sSame.getObject().isLiteral())
			    				processedText += sSame.getObject().asLiteral().getString().replace('_', ' ') + ", ";
			    			else
			    				processedText += sSame.getObject().toString().replace('_', ' ') + ", ";
			    		}
			    	}
			    	seenStatements.add(s.getPredicate());
			    	processedText += '\n';
		    	}
		    }
		}
		System.out.println(processedText);
		writeOutputFile(processedText, "result.txt");
		
		return true;
	}
}