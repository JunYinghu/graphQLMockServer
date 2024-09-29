import graphql.schema.GraphQLSchema;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;

public class GraphQLTest {

    public String requestBody(String name, Boolean confidential, String fileName) {
        String query = "query StaffLevel($name:String, $confidential: Boolean, $fileName: String)" + " { getStaffVisit(name: $name, confidential: $confidential, fileName:$fileName)" + " { department" + " name" + " visitDate" + " level" + " confidential" + "}" + "}";
        return "{\"query\":\"" + query + "\",\"variables\":{\n" + "\"name\":\"" + name + "\"," + "\"confidential\":" + confidential + "," + "\"fileName\":\"" + fileName + "\" }}";
    }

    public String requestBodyWithVisitTime(String name, Boolean confidential, String fileName) {
        String query = "query StaffLevel($name:String, $confidential: Boolean, $fileName: String)" + " { getStaffVisit(name: $name, confidential: $confidential, fileName:$fileName)" + " { department" + " name" + " visitDate" + " level" + " confidential" + "}" + "}";

        return "{\"query\":\"" + query + "\",\"variables\":{\n" + "\"name\":\"" + name + "\"," + "\"confidential\":" + confidential + "," + "\"fileName\":\"" + fileName + "\" }}";

    }

    public String requestBodyProd(String productId) {
        String query = "query productDetail($id: ID)" + " { getProdDetail(id: $id)" + " { id" + " name" + " description" + " price" + "}" + "}";

        return "{\"query\":\"" + query + "\",\"variables\":{\n" + "\"id\":\"" + productId + "\" }}";

    }

    @Test
    public void test_document() {
        // config JsonSchema, and JsonResponse
        // setup server and baseURI
        MockGraphQLServer graphQLServer = new MockGraphQLServer();
        GraphQLSchema schema = graphQLServer.schemaBuilder();
        int port = 808;
        graphQLServer.serverStart(schema, port);
        RestAssured.baseURI = "http://localhost:" + port + "/graphql";

        // query
        String name = "jun";
        Boolean confidential = true;
        String fileName = "projectA_Cost.doc";

        String requestBody = requestBody(name, confidential, fileName);
        Response response = given().contentType("application/json").body(requestBody).log().all().post();

        // verify
        response.then().log().all().statusCode(200);
        // response.then().body("data.getStaffLevel[0].level",containsString(verifyLevelMessage));
        // response.getBody().prettyPrint(); // print out response

        //stop server
        graphQLServer.serverStop();
    }

    @Test
    public void test_document_visit() {
        // setup server and baseURI
        MockGraphQLServer graphQLServer = new MockGraphQLServer();
        GraphQLSchema schema = graphQLServer.schemaBuilder();
        int port = 808;
        graphQLServer.serverStart(schema, port);
        RestAssured.baseURI = "http://localhost:" + port + "/graphql";

        // query
        String name = "Mary";
        Boolean confidential = true;
        String fileName = "projectA_design_confidential.doc";

        String requestBody = requestBodyWithVisitTime(name, confidential, fileName);
        Response response = given().contentType("application/json").body(requestBody).log().all().post();

        // verify
        response.then().log().all().statusCode(200);
        // response.then().body("data.getStaffLevel[0].level",containsString(verifyLevelMessage));
        // response.getBody().prettyPrint(); // print out response

        //stop server
        graphQLServer.serverStop();
    }

    @Test
    public void test_prodDetails() {
        // setup server and baseURI
        MockGraphQLServer graphQLServer = new MockGraphQLServer();
        GraphQLSchema schema = graphQLServer.schemaBuilder();
        int port = 8089;
        graphQLServer.serverStart(schema, port);
        RestAssured.baseURI = "http://localhost:" + port + "/graphql";

        // query
        String id = "001";
        String requestBody = requestBodyProd(id);
        Response response = given().contentType("application/json").body(requestBody).log().all().post();

        // verify
        response.then().log().all().statusCode(200);
        // response.then().body("data.getStaffLevel[0].level",containsString(verifyLevelMessage));
        // response.getBody().prettyPrint(); // print out response

        //stop server
        graphQLServer.serverStop();
    }
}
