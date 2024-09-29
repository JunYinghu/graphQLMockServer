import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.stream.JsonReader;
import graphql.GraphQL;
import graphql.schema.*;
import org.eclipse.jetty.server.Server;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static graphql.Scalars.*;

public class MockGraphQLServer {

    private Server server;

    public static void main(String[] args) {
        MockGraphQLServer graphQLServer = new MockGraphQLServer();
        GraphQLSchema schema = graphQLServer.schemaBuilder();
        int port = 8083;
        graphQLServer.serverStart(schema, port);

        // disable serverStop
        // graphQLServer.serverStop();
    }

    public GraphQLSchema schemaBuilder() {
        JsonNode rootNode = getJsonNode();
        // Define types
        Map<String, GraphQLObjectType> typeMap = new HashMap<>();
        JsonNode typesNode = rootNode.path("types");
        System.out.println("INFO: Schema Builder Start");
        for (JsonNode typeNode : typesNode) {
            String typeName = typeNode.path("name").asText();
            GraphQLObjectType.Builder typeBuilder = GraphQLObjectType.newObject().name(typeName);

            JsonNode fieldsNode = typeNode.path("fields");
            for (JsonNode fieldNode : fieldsNode) {
                String fieldName = fieldNode.path("name").asText();
                String fieldType = fieldNode.path("type").asText();
                GraphQLOutputType graphqlType = getGraphQLType(fieldType);
                typeBuilder.field(GraphQLFieldDefinition.newFieldDefinition().name(fieldName).type(graphqlType));
            }
            typeMap.put(typeName, typeBuilder.build());
        }

        // Define queries
        GraphQLObjectType.Builder queryTypeBuilder = GraphQLObjectType.newObject().name("Query");
        JsonNode queriesNode = rootNode.path("queries");
        for (JsonNode queryNode : queriesNode) {
            String queryName = queryNode.path("name").asText();
            String returnType = queryNode.path("type").asText();
            GraphQLObjectType returnTypeObject = typeMap.get(returnType);

            GraphQLFieldDefinition.Builder queryBuilder = GraphQLFieldDefinition.newFieldDefinition().name(queryName).type(new GraphQLList(returnTypeObject));

            JsonNode argumentsNode = queryNode.path("arguments");
            for (JsonNode argNode : argumentsNode) {
                String argName = argNode.path("name").asText();
                String argType = argNode.path("type").asText();
                GraphQLScalarType graphqlType = getGraphQLScalarType(argType);
                queryBuilder.argument(GraphQLArgument.newArgument().name(argName).type(graphqlType));
            }
            queryTypeBuilder.field(queryBuilder.build());
        }

        GraphQLSchema schema = GraphQLSchema.newSchema().query(queryTypeBuilder).build();

        return schema;
    }

    private JsonNode getJsonNode() {
        JsonConfig jsonConfig = new JsonConfig();
        //String jsonSchemaFileName = "schemaDefinition_productDetails.json";
        String jsonSchemaFileName = "schemaDefinition_VisitDoc.json";

        String jsonschemaPathString = jsonConfig.getJsonFileFullPath(jsonConfig.obtainJsonLocationJar(true),jsonSchemaFileName);
        System.out.println("INFO: Schema Definition File " + jsonschemaPathString);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode;
        try {
            rootNode = objectMapper.readTree(new File(jsonschemaPathString));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return rootNode;
    }


    private GraphQLScalarType getGraphQLScalarType(String type) {
        switch (type) {
            case "String":
                return GraphQLString;
            case "Int":
                return GraphQLInt;
            case "Float":
                return GraphQLFloat;
            case "Boolean":
                return GraphQLBoolean;
            case "ID":
                return GraphQLID;
            default:
                throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }

    private GraphQLOutputType getGraphQLType(String type) {
        switch (type) {
            case "String":
                return GraphQLString;
            case "Int":
                return GraphQLInt;
            case "Float":
                return GraphQLFloat;
            case "Boolean":
                return GraphQLBoolean;
            case "ID":
                return GraphQLID;
            default:
                throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }

    public void serverStart(GraphQLSchema schema, int port) {
        GraphQL graphQL = GraphQL.newGraphQL(schema).build();
        server = new Server(port);

        server.setHandler(new GraphQLHandler(graphQL));
        try {
            server.start();
            System.out.println("INFO: Local Server With Port " + port + " Has Been Started");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void serverStop() {
        if (server != null) {
            try {
                server.stop();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private DataFetcher<?> getStaffLevelDetails() {
        return environment -> {
            String name = environment.getArgument("name");
            String fileName = environment.getArgument("fileName");
            boolean confidential = environment.getArgument("confidential");
            List<Map<String, Object>> staffLevelList = new ArrayList<>();

            long visitDate = 1978042345670L;
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("department", "LAB A");
            details.put("name", name);
            details.put("visitDate", visitDate);
            details.put("level", 1);
            details.put("confidential", confidential);
            staffLevelList.add(details);
            return staffLevelList;
        };
    }

}

