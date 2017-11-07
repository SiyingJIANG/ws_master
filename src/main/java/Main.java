import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import fr.insalyon.ws.preprocess.*;
public class Main {

    public static void main(String[] args) throws Exception {
        Aggregator aggregator1 = new Aggregator();
        Postprocessor postprocessor = new Postprocessor();

        int nbPages;
        int aggregMode;
        int offset;
        float confidence;
        String userRequest;

        Scanner scan = new Scanner(System.in);
        System.out.println("Veuillez entrer votre requête:");
        userRequest = scan.nextLine();
        System.out.println("Combien de résultats voulez vous prendre en compte ?");
        nbPages = scan.nextInt();
        System.out.println("Veuillez indiquer la méthode d'aggrégation souhaitée:");
        System.out.println("11: Aggrégation 'triviale' (une simple union)");
        System.out.println("12: Aggrégation 'simple' (occurence dans au moins la moitié des réponses)");
        System.out.println("Un autre nombre: Aggrégation 'stricte' (occurence dans tous les résultats)");
        aggregMode = scan.nextInt();
        System.out.println("Quel offset de résultat désirez vous ?");
        offset= scan.nextInt();
        System.out.println("Quel niveau de confiance désirez vous ? (Entre 0.4 et 1.0)");
        confidence = scan.nextFloat();
        System.out.println("Start Preprocess");

        PreprocessService ps = new PreprocessService(System.getProperty("user.dir"),false);
        try {
            List<String> a = ps.ProcessQuery(userRequest,nbPages,offset);
            int i = 0;
            for (String s:
                    a) {
                Set<String> set = ps.ProcessSpotlight(s,confidence);
                int res = ps.ProcessSparql(set,Integer.toString(i++));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("End Preprocess");
        System.out.println("start Process");

        for(int i=0;i<nbPages;i++)
        {
            aggregator1.loadModelFromFile(i+".rdf");
        }

        try {
            //postprocessor.process("em.rdf");
            Model resultModel = ModelFactory.createDefaultModel();
            switch (aggregMode) {
                case 11:
                    resultModel = aggregator1.strictAggregation(1);
                    break;
                case 12:
                    resultModel = aggregator1.strictAggregation(2);
                    break;
                default:
                    resultModel = aggregator1.strictAggregation(0);
                    break;
            }
            aggregator1.writeOutputFileFromModel(resultModel, userRequest+".rdf");
            System.out.println("End process");
            System.out.println("RESULT 1 =============================================================");
            postprocessor.process(userRequest+".rdf");

        } catch (Exception e) {e.printStackTrace();}

        System.out.println("end Program");
    }

}
