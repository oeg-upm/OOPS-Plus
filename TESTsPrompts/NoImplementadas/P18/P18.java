//DEPS dev.langchain4j:langchain4j-ollama:0.29.1
//DEPS org.slf4j:slf4j-simple:2.0.12

import dev.langchain4j.model.ollama.OllamaChatModel;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class P18 {
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
                    
                    String supClass = fila[0].trim();     
                    String property = fila[1].trim().toLowerCase(); 
                    String valorCorrecto = fila[2].trim().toLowerCase(); 

                    String prompt = " Task: Determine if the class " + supClass  + " could serve as subject of the property " + property + ".Output strictly 'Yes' or 'No'.";
                    String respuesta = model.generate(prompt);
                    
                    respuesta = respuesta.replaceAll("\\W", "")
                                         .replaceAll("-", "")
                                         .replaceAll("\\.", "")
                                         .replaceAll("_", "")
                                         .toLowerCase();

                    if (respuesta.equals(valorCorrecto)) {
                        cont++;
                    }

                    System.out.println("\nSupClass: " + supClass + " Prop: "+ property );
                    System.out.println("Respuesta del modelo: " + respuesta + " | Esperada: " + valorCorrecto);
                }
            }

            System.out.println("\n-------------------------------------------");
            System.out.println(cont + "/" + totalLineas + " correctas");
            System.out.println("-------------------------------------------");

        } catch (IOException e) {
            System.err.println("Error al procesar el archivo CSV: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
