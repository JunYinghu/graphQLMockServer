import com.google.gson.stream.JsonReader;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class JsonConfig {

    public String obtainJsonLocationJar(boolean bSchema) {
        Path jarPath;
        Path jarDirectory;
        Path jsonschemaPath;
        try {
            jarPath = Paths.get(JsonReader.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            jarDirectory = jarPath.getParent();
            String fileName;
            if (bSchema) {
                fileName = "schema.json";
            }
            else {
                fileName = "response.json";
            }
            jsonschemaPath = jarDirectory.resolve(fileName);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        String jsonPathString = jsonschemaPath.toString();
        return jsonPathString;
    }
    String getJsonFileFullPath(String jsonFileLocation, String jsonFileName) {
        String jsonsPathString = null;
        if (jsonFileLocation.contains(".m2")) {
            String jsonFilePath = "/GraphQLSetup/"+jsonFileName;
            if (System.getProperty("user.dir").contains("\\MockServer")) {
                jsonsPathString = System.getProperty("user.dir") + jsonFilePath;
            } else {
                jsonsPathString = System.getProperty("user.dir") + "/MockServer" + jsonFilePath;
            }
            return jsonsPathString;
        }
        else {
            return jsonFileLocation;
        }
    }
}