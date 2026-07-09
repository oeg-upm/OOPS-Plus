//DEPS dev.langchain4j:langchain4j-ollama:0.29.1
//DEPS org.slf4j:slf4j-simple:2.0.12

import dev.langchain4j.model.ollama.OllamaChatModel;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class PromptP10 {
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
                .modelName(nombreModelo).temperature(0.0)               
                .build();
                
        // gestion csv
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line = "";
            
            while ((line = br.readLine()) != null) {
                String[] fila = line.split(cvsSplitBy);
                
                if (fila.length >= 2) {
                    totalLineas++;
                    
                    String contexto = fila[0].trim();
                    String p1 = fila[1].trim().toLowerCase(); 
                    String p2 = fila[2].trim().toLowerCase();      
                    String valorCorrecto = fila[3].trim().toLowerCase(); 

                    String prompt = "In the context of " + contexto + " Determine if the following concepts are DISJOINT (They are mutually exclusive concepts that do not share any common ground, so it is impossible for an element to belong to both at the same time) or NOT. Return only yes or no. The concepts are:" + p1 +", "+p2;
                    
                    
               long startTime = System.currentTimeMillis();
                    String respuesta = model.generate(prompt);
                    long endTime = System.currentTimeMillis();
                    
                    long tiempoMs = endTime - startTime;
                    
                    respuesta = respuesta.replaceAll("\\W", "")
                                         .replaceAll("-", "")
                                         .replaceAll("\\.", "")
                                         .replaceAll("_", "")
                                         .toLowerCase();

                   

                    System.out.println("\nContexto: " + contexto + "Palabras: " +p1 +", "+p2);
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
            System.out.println(cont + "/" + totalLineas + " correctas. % de aciertos "+ porcentaje +"%");
            System.out.println("-------------------------------------------");

        } catch (IOException e) {
            System.err.println("Error al procesar el archivo CSV: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
