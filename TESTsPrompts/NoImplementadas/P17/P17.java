//DEPS dev.langchain4j:langchain4j-ollama:0.29.1
//DEPS org.slf4j:slf4j-simple:2.0.12

import dev.langchain4j.model.ollama.OllamaChatModel;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class P17 {
    public static void main(String[] args) {
        int cont = 0;
        int totalLineas = 0;

        Properties config = new Properties();
        
        try (FileInputStream configInput = new FileInputStream("config.properties")) {
            config.load(configInput);
        } catch (IOException e) {
            System.err.println("No se pudo cargar el archivo config.properties.");
            e.printStackTrace();
            return;
        }

      	//saca los datos del config.properties
        String urlModelo = config.getProperty("ollama.url");
        String nombreModelo = config.getProperty("ollama.model");
        String csvFile = config.getProperty("csv.file");
        String cvsSplitBy = config.getProperty("csv.separator");

        // Configuracion del modelo
        OllamaChatModel model = OllamaChatModel.builder()
                .baseUrl(urlModelo) 
                .modelName(nombreModelo)               
                .build();
                
        // gestion csv
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line = "";
            
            while ((line = br.readLine()) != null) {
                String[] fila = line.split(cvsSplitBy);
                
                if (fila.length >= 2) {
                    totalLineas++;
                    
                    String clase = fila[0].trim();     
                    String s1 = fila[1].trim().toLowerCase(); 
                    String s2 = fila[2].trim().toLowerCase(); 
                    String s3 = fila[3].trim().toLowerCase(); 
                    String valorCorrecto = fila[4].trim().toLowerCase();

                    String prompt = "You are a strict logical classifier. Your only task is to determine if a specific 'Class' in an ontology is actually a unique real-world individual. "
                        + "Rule: If any of the presented classes represents a unique, specific entity that cannot have instances of its own, answer YES. If all of them represents a general category that can have multiple instances, answer NO. \n"+
                         "YOU MUST ANSWER ONLY 'YES' OR 'NO'. DO NOT ADD ANY OTHER WORDS.\n "+
                         "Examples: Class: 'Madrid', 'village', 'city'  | Parent Class: 'Populated place' Output: YES\n"+
                        " Class: 'European_City' | Parent Class: 'City' Output: NO "+
                        "Class: 'Kilimanjaro', 'Everest' | Parent Class: 'Mountain' Output: YES "+
                        "Class: 'Karst_Cave' , 'Volcanic_Cave'| Parent Class: 'Cave' Output: NO "+
                        "Class: " + s1 + ", " +s2 +", "+s3 + " | Parent Class: " + clase + " Output:";
                        
                        
                     long startTime = System.currentTimeMillis();
                    String respuesta = model.generate(prompt);
                    long endTime = System.currentTimeMillis();
                    
                    long tiempoMs = endTime - startTime;
                    
                    respuesta = respuesta.replaceAll("\\W", "")
                                         .replaceAll("-", "")
                                         .replaceAll("\\.", "")
                                         .replaceAll("_", "")
                                         .toLowerCase();

                  

                    System.out.println("\nClase padre: " + clase+ " Subclases: " + s1+ ", "+s2 + ", "+ s3 );
                    System.out.println("Respuesta del modelo: " + respuesta + " | Esperada: " + valorCorrecto);
                                         System.out.println("Tiempo de peticion: " + tiempoMs + "ms");
                      if (respuesta.equals(valorCorrecto)) {
                        System.out.println("YES");
                        cont++;
                    }
                }
            }
	    double porcentaje = ( (double) cont / totalLineas)*100;
            System.out.println("\n-------------------------------------------");
            System.out.println(cont + "/" + totalLineas + " correctas. % de aciertos "+ porcentaje +"%" );
            System.out.println("-------------------------------------------");

        } catch (IOException e) {
            System.err.println("Error al procesar el archivo CSV: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
