import java.io.*;
import java.util.*;

import org.apache.jena.rdf.model.*;
import org.json.JSONArray;
import org.json.JSONObject;


public class Aggregator {
    private ArrayList<Model> dataset;

    // CONSTRUCTEUR
    public Aggregator() {
        dataset = new ArrayList<Model>();
    }

    public boolean loadModelFromFile(String filename) throws IOException {
        if (filename == null) return false;

        InputStream in = new FileInputStream(new File(filename));

        Model graph = ModelFactory.createMemModelMaker().createDefaultModel();
        graph.read(in, "");
        in.close();

        dataset.add(graph);

        return true;
    }

    // TODO : REWRITE USING WRITE METHOD FOR A MODEL OBJECT (EASY) =>faire la méthode suivante mais qui prend un model
    public boolean writeOutputFile(String content, String filename) throws IOException {
        FileOutputStream fop = null;
        File file;

        try {
            file = new File(filename);
            fop = new FileOutputStream(file);

            if (!file.exists()) {
                file.createNewFile();
            }

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

    // TODO : REWRITE USING WRITE METHOD FOR A MODEL OBJECT (EASY) =>faire la méthode suivante mais qui prend un model
    public void writeOutputFileFromModel(Model modelContent, String filename) throws IOException {
        PrintWriter out = new PrintWriter(filename);
        modelContent.write(out, "RDF/XML");
        out.close();
    }

    public JSONObject getJSONOutput(HashMap<Statement, Integer> weightMap, HashMap<String, Integer> groupMap,Set<String> resources) throws IOException {
        JSONArray listLinks = new JSONArray();
        JSONArray listNodes = new JSONArray();
        HashMap<String, Integer> resourcesIndex = new HashMap<>();
        int i = 0;
        for (String nodeName : resources) {
            JSONObject node = new JSONObject();
            node.put("id", nodeName);
            node.put("group", groupMap.get(nodeName));
            listNodes.put(node);
            resourcesIndex.put(nodeName, i++);
        }
        for (HashMap.Entry<Statement, Integer> entry : weightMap.entrySet()) {
            JSONObject link = new JSONObject();
            String subject = entry.getKey().getSubject().getLocalName();
            link.put("source", resourcesIndex.get(subject));
            String object = entry.getKey().getObject().toString().substring(31, entry.getKey().getObject().toString().length());
            link.put("target", resourcesIndex.get(object));
            link.put("value", entry.getValue());
            listLinks.put(link);
        }

        JSONObject nodes = new JSONObject();
        nodes.put("nodes", listNodes);
        nodes.put("links", listLinks);
        return nodes;
    }


    public Model strictAggregation(int aggregationType) {
        int weightLimit = 0;
        Model union = ModelFactory.createMemModelMaker().createDefaultModel();
        Model intersection = ModelFactory.createMemModelMaker().createDefaultModel();

        // UNION AND WEIGHT VALUES COMPUTING
        for (Model g : dataset) {
            /*for(int i = 0; i < g.size(); i++) {
                union.addTripleFromArray(g.getTriple(i));
			}*/
            union.add(g.difference(union));
            //union.add(union.difference(g));
        }

        HashMap<Statement, Integer> weightMap = getWeightMap(union);

        // CHECKING TYPE OF AGGREGATION
        switch (aggregationType) {
            case 1:
                weightLimit = 1;
                break;
            case 2:
                weightLimit = (int) (dataset.size() / 2);
                break;
            default:
                weightLimit = dataset.size();
                break;
        }

        // INTERSECTION AND AGGREGATE COMPUTING
        StmtIterator iUnionStat2 = union.listStatements();
        while (iUnionStat2.hasNext()) {
            Statement s = iUnionStat2.nextStatement();

            if (weightMap.get(s) >= weightLimit)
                intersection.add(s);
        }
        try {
            HashMap<String, Integer> groupMap = new HashMap<>();
            Set<String> resources = getAllResource(intersection);
            for (String resource : resources) {
                groupMap.put(resource, 1);
            }
            JSONObject links = getJSONOutput(getWeightMap(intersection), groupMap, resources);

            writeOutputFile(links.toString(), "links.json");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return intersection;
    }

    // TODO : DO THIS AFTER THE kSNAP (MEDIUM)
    public Model semanticAggregation() {
        Model union = ModelFactory.createMemModelMaker().createDefaultModel();
        Model intersection = ModelFactory.createMemModelMaker().createDefaultModel();

        return intersection;
    }

    // TODO : DO THIS FIRST (HARD)
    public Model SNAPAggregation() {
        Model union = ModelFactory.createMemModelMaker().createDefaultModel();
        Model result = ModelFactory.createMemModelMaker().createDefaultModel();
        // UNION COMPUTING
        for (Model g : dataset) {
            union.add(g.difference(union));
        }

        // ATTRIBUTES INITIALIZATION
        Property type = union.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "type");
        Property contains = union.createProperty("http://INSA.WebSem/", "content");

        Resource naturalPerson = union.createResource("http://www.ontologydesignpatterns.org/ont/dul/DUL.owl#NaturalPerson");
        Resource foafPerson = union.createResource("http://xmlns.com/foaf/0.1/Person");
        Resource dbpediaPlace = union.createResource("http://dbpedia.org/ontology/PopulatedPlace");
        Resource w3SpatialThing = union.createResource("http://www.w3.org/2003/01/geo/wgs84_pos#SpatialThing");

        StmtIterator it = null;

        // PERSONS GROUP COMPUTING
        it = union.listStatements(new SimpleSelector(null, type, (RDFNode) naturalPerson));
        while (it.hasNext()) {
            Statement s = it.nextStatement();

            //System.out.println(s.getResource().toString());

            result.add(naturalPerson, contains, s.getSubject());

		    /*if(s.getSubject().isResource())
                result.add(person, contains, s.getSubject());
			else if(s.getObject().isLiteral())
				result.add(person, contains, s.getLiteral());
			else
				result.add(person, contains, s.getObject().toString());*/
        }

        it = union.listStatements(new SimpleSelector(null, type, (RDFNode) foafPerson));
        while (it.hasNext()) {
            Statement s = it.nextStatement();

            result.add(foafPerson, contains, s.getSubject());
        }

        // PLACES GROUP COMPUTING
        it = union.listStatements(new SimpleSelector(null, type, (RDFNode) dbpediaPlace));
        while (it.hasNext()) {
            Statement s = it.nextStatement();

            result.add(dbpediaPlace, contains, s.getSubject());
        }

        it = union.listStatements(new SimpleSelector(null, type, (RDFNode) w3SpatialThing));
        while (it.hasNext()) {
            Statement s = it.nextStatement();

            result.add(w3SpatialThing, contains, s.getSubject());
        }

		/*StmtIterator i = result.listStatements();
        while(i.hasNext()) {
		    Statement s = i.nextStatement();
		    System.out.println(s.getSubject().toString() + " " + s.getPredicate().toString() + " " + s.getObject().toString());
		}*/

        // COMPUTING LINKS BETWEEN GROUPS - TODO SWAP WHILE
        ResIterator iRes1 = result.listSubjects();
        while (iRes1.hasNext()) {
            Resource r1 = iRes1.nextResource();

            ResIterator iRes2 = result.listSubjects();
            while (iRes2.hasNext()) {
                Resource r2 = iRes2.nextResource();

                HashMap<Property, Integer> predicateWeightMap = new HashMap<Property, Integer>();

                System.out.println("-------------------- " + r1.toString() + " " + r2.toString());

                StmtIterator iProp1 = r1.listProperties();
                while (iProp1.hasNext()) {
                    Statement s1 = iProp1.nextStatement();

                    StmtIterator iProp2 = r2.listProperties();
                    while (iProp2.hasNext()) {
                        Statement s2 = iProp2.nextStatement();

                        //System.out.println(s1.getSubject().toString() + " " + s1.getPredicate().toString() + " " + s1.getObject().toString());
                        System.out.println(s2.getSubject().toString() + " " + s2.getPredicate().toString() + " " + s2.getObject().toString());

                        StmtIterator iUnion = union.listStatements();
                        while (iUnion.hasNext()) {
                            Statement s = iUnion.nextStatement();

                            if (s.getObject().isResource() && s1.getObject().isResource() && s2.getObject().isResource() && s1.getPredicate().equals(contains) && s2.getPredicate().equals(contains)) {
                                if (s.getSubject().equals(s1.getObject().asResource()) && s.getObject().asResource().equals(s2.getObject().asResource()) && !s1.getObject().asResource().equals(s2.getObject().asResource())) {
                                    if (!predicateWeightMap.containsKey(s.getPredicate())) {
                                        predicateWeightMap.put(s.getPredicate(), 1);
                                    } else if (predicateWeightMap.containsKey(s.getPredicate())) {
                                        predicateWeightMap.replace(s.getPredicate(), predicateWeightMap.get(s.getPredicate()), predicateWeightMap.get(s.getPredicate()) + 1);
                                    }
                                    //if(s.getPredicate().equals(type))
                                    //System.out.println(s2.getSubject().toString() + " " + s2.getPredicate().toString() + " " + s2.getObject().toString());
                                }
                            } else if (s.getObject().isLiteral() && s1.getObject().isResource() && s2.getObject().isLiteral() && s1.getPredicate().equals(contains) && s2.getPredicate().equals(contains)) {
                                if (s.getSubject().equals(s1.getObject().asResource()) && s.getObject().asLiteral().equals(s2.getObject().asLiteral())) {
                                    if (!predicateWeightMap.containsKey(s.getPredicate())) {
                                        predicateWeightMap.put(s.getPredicate(), 1);
                                    } else if (predicateWeightMap.containsKey(s.getPredicate())) {
                                        predicateWeightMap.replace(s.getPredicate(), predicateWeightMap.get(s.getPredicate()), predicateWeightMap.get(s.getPredicate()) + 1);
                                    }
                                }
                            }
                        }
                    }
                }

                if (predicateWeightMap.size() != 0 && !r1.equals(r2)) {
                    Map.Entry<Property, Integer> maxEntry = null;

                    for (Map.Entry<Property, Integer> entry : predicateWeightMap.entrySet()) {
                        if (maxEntry == null || entry.getValue() > maxEntry.getValue()) {
                            maxEntry = entry;
                        }
                    }

                    //System.out.println("----------------");
                    //System.out.println(r1.toString() + " " + r2.toString());
                    result.add(r1, maxEntry.getKey(), r2);
                    //System.out.println("----------------");
                }
                //predicateWeightMap.clear();
            }
        }
        try {
            HashMap<String, Integer> groupMap = new HashMap<>();
            Set<String> resources = getAllResource(result);
            for (String resource : resources) {
                groupMap.put(resource, 1);
            }
            JSONObject links = getJSONOutput(getWeightMap(result), groupMap, resources);

            writeOutputFile(links.toString(), "links.json");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }


    // DO NOT USE YET
    public boolean addGraph(Model graph) {
        if (graph == null)
            return false;

        for (Model g : dataset) {
            if (g.equals(graph))
                return false;
        }

        dataset.add(graph);
        return true;
    }

    public HashMap<Statement, Integer> getWeightMap(Model model) {
        HashMap<Statement, Integer> weightMap = new HashMap<Statement, Integer>();

        StmtIterator iUnionStat1 = model.listStatements();
        while (iUnionStat1.hasNext()) {
            Statement sUnion = iUnionStat1.nextStatement();

            weightMap.put(sUnion, 0);
            for (Model g : dataset) {
                StmtIterator iStat = g.listStatements();
                while (iStat.hasNext()) {
                    Statement s = iStat.nextStatement();

                    if (sUnion.equals(s))
                        weightMap.replace(sUnion, weightMap.get(sUnion), weightMap.get(sUnion) + 1);
                }
            }
        }
        return weightMap;

    }

    public Set<String> getAllResource(Model model) {
        Set<String> resourceSet = new HashSet<>();
        StmtIterator iUnionStat1 = model.listStatements();
        while (iUnionStat1.hasNext()) {
            Statement statement = iUnionStat1.nextStatement();
            String subject = statement.getSubject().getLocalName();
            String object = statement.getObject().toString().substring(31, statement.getObject().toString().length());
            resourceSet.add(subject);
            resourceSet.add(object);
        }
        return resourceSet;
    }


    // DO NOT USE YET
    /*public boolean setQuery(Model graph) {
        if(ResultGraph.equals(graph))
			return false;
		
		ResultGraph = graph;
		return true;
	}*/
}
