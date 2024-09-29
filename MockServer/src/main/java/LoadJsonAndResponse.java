import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.stream.JsonReader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class LoadJsonAndResponse {
    private static JsonNode queriesArray;

   // private static final JsonConfig JsonConfig = new JsonConfig();
    static {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            // Load JSON configuration
            JsonConfig jsonConfig = new JsonConfig();
            //String fileName = "productDetails.json";
            String fileName = "vistDoc.json";
            String responseFilePath = jsonConfig.getJsonFileFullPath(jsonConfig.obtainJsonLocationJar(false),fileName);

            System.out.println("INFO: Response Json File: " + responseFilePath);
            JsonNode rootNode = objectMapper.readTree(new File(responseFilePath));
            queriesArray = rootNode.path("queries");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Helper method to create an error response
    private static JsonNode createErrorResponse(String message) {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.createObjectNode().put("message", message);
    }

    static JsonNode getResponseForQuery(String constructedQuery, Map<String, Object> requestVariables) {
        if (queriesArray == null) {
            return createErrorResponse("INFO: Configuration not loaded");
        }

        for (JsonNode queryNode : queriesArray) {
            String queryText = queryNode.path("query").asText().trim();

            System.out.println("INFO: Request Body: "+ constructedQuery);
            System.out.println("INFO: Expected Request Body : "+ queryText);

            // Iterate over the queries
            if (queryText.contentEquals(constructedQuery)) {
                JsonNode jsonVariables = queryNode.path("variables");
                // Check if the provided variables match those in the JSON configuration
                boolean variablesMatch = true;
                Map<String, Object> VariablesInJson = new LinkedHashMap<>();
                // Iterate over the variables and populate the map
                Iterator<Map.Entry<String, JsonNode>> fields = jsonVariables.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    String key = field.getKey();
                    JsonNode valueNode = field.getValue();
                    // Determine the type of JSON element and add it to the map
                    if (valueNode.isTextual()) {
                        VariablesInJson.put(key, valueNode.asText());
                    } else if (valueNode.isNumber()) {
                        // Handle different number types
                        if (valueNode.isIntegralNumber()) {
                            VariablesInJson.put(key, valueNode.asLong());
                        } else {
                            VariablesInJson.put(key, valueNode.asDouble());
                        }
                    } else if (valueNode.isBoolean()) {
                        VariablesInJson.put(key, valueNode.asBoolean());
                    } else if (valueNode.isNull()) {
                        VariablesInJson.put(key, null);
                    }
                    // Add handling for other types if necessary
                }
                System.out.println("INFO: Expect Variable "+  VariablesInJson);
                System.out.println("INFO: Request Variable"+  requestVariables);

                variablesMatch = VariablesInJson.equals(requestVariables);
                if (variablesMatch) {
                    JsonNode responseNode = queryNode.path("response").path("data");
                    // Call method to get the node with key starting with "get"
                    //JsonNode dynamicDataNode = getNodeStartingWithGet(responseNode);

                    //JsonNode responseNode = queryNode.path("response").path("data").path("getStaffVisit");
                    return responseNode;
                }
                else {
                    System.out.println("INFO: Request Query Variable Not Found In Schema, Proceed Next Search");
                }
            }
            else {
                System.out.println("INFO: Request Query Not Found In Schema, Proceed Next Search");
            }
        }
        // Return "no data found" if no matching query is found
        return createErrorResponse("No Data Found");
    }

    private static JsonNode getNodeStartingWithGet(JsonNode dataNode) {
        // Iterate through the fields to find the one starting with "get"
        Iterator<Map.Entry<String, JsonNode>> fields = dataNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String key = field.getKey();
            if (key.startsWith("get")) {
                return field.getValue();
            }
        }
        // Return an empty node if no matching key is found
        return null;
    }


}
