
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class GraphQLHandler extends AbstractHandler {
    private final GraphQL graphQL;

    public GraphQLHandler(GraphQL graphQL) {
        this.graphQL = graphQL;
    }

    @Override
    public void handle(String s, Request request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
        try {
            if ("/graphql" .equalsIgnoreCase(s) && "POST" .equalsIgnoreCase(request.getMethod())) {
                // get request body from request
                String requestBody = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
                // covert request boy as jsonObject
                JsonObject jsonObject = JsonParser.parseString(requestBody).getAsJsonObject();

                // extract query part from jsonObject
                String query = jsonObject.get("query").getAsString();
                JsonElement queryNode = jsonObject.get("query");
                //System.out.println("Request Body - query: "+ query.toString());
                // extract variables part from jsonObject
                JsonObject variablesObj = jsonObject.getAsJsonObject("variables");
                //System.out.println("Request Body - variables: "+ variablesObj.toString());
                // reform variables as map type
                LinkedHashMap<String, Object> variables = new LinkedHashMap<>();
                LinkedHashMap<String, Object> requestVariables = new LinkedHashMap<>();

                // use it if all variables are string
                //variablesObj.entrySet().forEach(entry -> {
                //    variables.put(entry.getKey(), entry.getValue().getAsString());
                //});
                variablesObj.entrySet().forEach(stringJsonElementEntry -> {
                    JsonElement value = stringJsonElementEntry.getValue();
                    String key = stringJsonElementEntry.getKey();
                    //System.out.println("-----------" + key + value);
                    // determine the type of JSON element and convert it accordingly
                    // add to deal other format
                    if (value.isJsonPrimitive()) {
                        if (value.getAsJsonPrimitive().isBoolean()) {
                            // variables.put(key,value.getAsBoolean());
                            //System.out.println("----(((-------" + key + value);
                            requestVariables.put(key, value.getAsBoolean());
                        } else if (value.getAsJsonPrimitive().isNumber()) {
                            //System.out.println("------)))-----" + key + value);
                            // variables.put(key,value.getAsNumber());
                            requestVariables.put(key, value.getAsNumber());
                        } else if (value.getAsJsonPrimitive().isString()) {
                            //System.out.println("-------+++----" + key + value);
                            // variables.put(key,value.getAsString());
                            requestVariables.put(key, value.getAsString());
                        }
                    }
                });
                // covert query with variables as execution input
                ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                        .query(query)
                        .variables(requestVariables)
                        .build();

                //System.out.println("testing ------");
                //System.out.println(query);
                //System.out.println(requestVariables);
                //System.out.println("testing ---++++++---");

                JsonNode responseJsonData = null;
                if (query.contains("get")) {
                    responseJsonData = LoadJsonAndResponse.getResponseForQuery(query, requestVariables);
                    System.out.println("INFO: Response Data " + responseJsonData);
                }

                // perform execution - while using data fetch
                //ExecutionResult executionResult = graphQL.execute(executionInput);
                //System.out.println("executionResult" + executionResult);
                httpServletResponse.setContentType("application/json");
                httpServletResponse.setStatus(HttpServletResponse.SC_OK);

                // reform returned data as json
                ObjectMapper objectMapper = new ObjectMapper();
                //- while using data fetch
                Map<String, Object> wrappedResponse = new HashMap<>();
                wrappedResponse.put("data", responseJsonData);  // Wrap your response inside "data"

                objectMapper.writeValue(httpServletResponse.getWriter(), wrappedResponse);

                //objectMapper.writeValue(httpServletResponse.getWriter(), executionResult.toSpecification());
                request.setHandled(true);



            } else if (s.contains("/download")) {
                //JsonConfig json = new JsonConfig();
                //json.getUrlValue();
                String fileName = extractFileNameFromUrl(s);
                System.out.println("============"+ fileName);
                if (fileName != null) {
                    System.out.println("INFO: File Start Downloading");
                    handleFileDownload(httpServletRequest, httpServletResponse, fileName);

                } else {
                    httpServletResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    httpServletResponse.getWriter().println("Invalid or Missing File Name");
                }

            } else {
                httpServletResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                httpServletResponse.getWriter().println("Request Path No Setup");
                request.setHandled(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            httpServletResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            httpServletResponse.getWriter().println("Internal Server Error");
            request.setHandled(true);
        }
    }

    void handleFileDownload(HttpServletRequest request, HttpServletResponse response, String fileName) {
        JsonConfig jsonConfig = new JsonConfig();
        String jsonResponseFullLocation = jsonConfig.getJsonFileFullPath(jsonConfig.obtainDownloadLocation(fileName),fileName);

        File file = new File(jsonResponseFullLocation);
        if (!file.exists()) {
            System.out.println("INFO: Downloaded File Not Found : " + jsonResponseFullLocation);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            try {
                response.getWriter().println("File Not Found" +fileName);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return;
        }
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename\"" + file.getName() + "\"");
        response.setContentLengthLong(file.length());
        try (FileInputStream in = new FileInputStream(file); OutputStream out = response.getOutputStream()) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            System.out.println("INFO: File Downloaded Successfully: " + fileName);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private String extractFileNameFromUrl(String url) {
        if (url != null && url.contains("/")) {
            return url.substring(url.lastIndexOf("/") + 1);
        }
        return null;
    }
}