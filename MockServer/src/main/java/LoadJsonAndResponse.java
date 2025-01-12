import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.stream.JsonReader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
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
            String fileName = "productDetails_rule.json";
            //String fileName = "vistDoc.json";
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

    private static final Map<String, JsonNode> QUERY_CACHE = new HashMap<>();

    static void preloadQueries(JsonNode queriesArray) {
        if (queriesArray == null) {
            throw new IllegalStateException("INFO: Configuration not loaded");
        }

        for (JsonNode queryNode : queriesArray) {
            String queryText = queryNode.path("query").asText().trim();
            JsonNode variables = queryNode.path("variables");
            JsonNode response = queryNode.path("response").path("data");

            // Combine query text and variables into a unique key
            String cacheKey = createCacheKey(queryText, variables);
            QUERY_CACHE.put(cacheKey, response);
        }

        System.out.println("INFO: Preloaded Query Cache: " + QUERY_CACHE.keySet());
    }

    private static String createCacheKey(String queryText, JsonNode variables) {
        return queryText + ":" + variables.toString();
    }

    static JsonNode getResponseForQuery(String constructedQuery, Map<String, Object> requestVariables) {
        // Create a JSON representation of the request variables
        preloadQueries( queriesArray);
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode requestVariablesNode = objectMapper.convertValue(requestVariables, JsonNode.class);

        // Generate the cache key
        String cacheKey = createCacheKey(constructedQuery, requestVariablesNode);

        // Fetch from cache
        JsonNode cachedResponse = QUERY_CACHE.get(cacheKey);

        if (cachedResponse != null) {
            return cachedResponse; // Return matched response
        } else {
            return createErrorResponse("No Data Found"); // Return 404 equivalent
        }
    }

    static JsonNode getResponseForQuery_b(String constructedQuery, Map<String, Object> requestVariables) {
        if (queriesArray == null) {
            return createErrorResponse("INFO: Configuration not loaded");
        }

        JsonNode matchedResponse = null; // Track the matching response
        boolean isMatched = false;      // Track if a match was found

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
                System.out.println("INFO: Request Variable "+  requestVariables);

                variablesMatch = VariablesInJson.equals(requestVariables);
                if (variablesMatch) {
                    matchedResponse  = queryNode.path("response").path("data");

                    break;
                }
                else {
                    System.out.println("INFO: Request Query Variable Not Found In Schema, Proceed Next Search");
                }
            }
            else {
                System.out.println("INFO: Request Query Not Found In Schema, Proceed Next Search");
            }
        }
        if (matchedResponse!=null){
            return matchedResponse ;
        }else {
            // Return "no data found" if no matching query is found
            return createErrorResponse("No Data Found");
        }
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
