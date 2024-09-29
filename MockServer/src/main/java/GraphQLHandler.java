
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import graphql.ExecutionInput;
import graphql.GraphQL;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import java.io.IOException;
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
            if ("/graphql".equalsIgnoreCase(s) && "POST".equalsIgnoreCase(request.getMethod())) {
                // get request body from request
                String requestBody = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
                // covert request boy as jsonObject
                JsonObject jsonObject = JsonParser.parseString(requestBody).getAsJsonObject();

                // extract query part from jsonObject
                String query = jsonObject.get("query").getAsString();
                //System.out.println("Request Body - query: "+ query.toString());
                // extract variables part from jsonObject
                JsonObject variablesObj = jsonObject.getAsJsonObject("variables");
                //System.out.println("Request Body - variables: "+ variablesObj.toString());
                // reform variables as map type
                LinkedHashMap<String, Object> variables = new LinkedHashMap<>();
                Map<String, Object> requestVariables = new LinkedHashMap<>();
                // use it if all variables are string
                //variablesObj.entrySet().forEach(entry -> {
                //    variables.put(entry.getKey(), entry.getValue().getAsString());
                //});

                variablesObj.entrySet().forEach(stringJsonElementEntry -> {
                    JsonElement value = stringJsonElementEntry.getValue();
                    String key = stringJsonElementEntry.getKey();

                // determine the type of JSON element and convert it accordingly
                // add to deal other format
                if (value.isJsonPrimitive()){
                    if (value.getAsJsonPrimitive().isBoolean()){
                        variables.put(key,value.getAsBoolean());
                        requestVariables.put(key, value.getAsBoolean());
                    }else if (value.getAsJsonPrimitive().isNumber()){
                        variables.put(key,value.getAsNumber());
                        requestVariables.put(key, value.getAsNumber());
                    } else if (value.getAsJsonPrimitive().isString()){
                        variables.put(key,value.getAsString());
                        requestVariables.put(key, value.getAsString());
                    }
                }
                });
                // covert query with variables as execution input
                ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                        .query(query)
                        .variables(variables)
                        .build();

                JsonNode responseJsonData = null;
                if (query.contains("get")){
                    responseJsonData = LoadJsonAndResponse.getResponseForQuery(query, requestVariables);
                    System.out.println("INFO: Response Data" + responseJsonData );
                }

                // perform execution - while using data fetch
                //ExecutionResult executionResult = graphQL.execute(executionInput);

                httpServletResponse.setContentType("application/json");
                httpServletResponse.setStatus(HttpServletResponse.SC_OK);

                // reform returned data as json
                ObjectMapper objectMapper = new ObjectMapper();
                //- while using data fetch
                objectMapper.writeValue(httpServletResponse.getWriter(), responseJsonData);

                //objectMapper.writeValue(httpServletResponse.getWriter(), executionResult.toSpecification());
                request.setHandled(true);
            }
            else {
                httpServletResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                httpServletResponse.getWriter().println("请求地址有误");
                request.setHandled(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            httpServletResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            httpServletResponse.getWriter().println("Internal Server Error");
            request.setHandled(true);
        }
    }
}
