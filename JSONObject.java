import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JSONObject {
    private final Map<String, Object> data;

    public JSONObject(String jsonString) {
        this.data = parseJson(jsonString);
    }

    public JSONObject() {
        this.data = new HashMap<>();
    }

    public JSONObject put(String key, Object value) {
        data.put(key, value);
        return this;
    }

    public String getString(String key) {
        Object value = data.get(key);
        return value != null ? value.toString() : null;
    }

    public int getInt(String key) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0; // Default value if not an integer
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("{\n");
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            result.append("    \"").append(entry.getKey()).append("\": ");
    
            if (entry.getValue() instanceof String) {
                result.append("\"").append(entry.getValue()).append("\"");
            } else {
                result.append(entry.getValue());
            }
    
            result.append(",\n");
        }
        result.append("}");
        return result.toString();
    }
    

    private Map<String, Object> parseJson(String jsonString) throws IllegalArgumentException {
        Map<String, Object> parsedMap = new HashMap<>();
        Pattern pattern = Pattern.compile("\"(\\w+)\":\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(jsonString);
    
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = matcher.group(2);
            parsedMap.put(key, value);
        }
    
        // Check if the entire string was successfully parsed
        if (!matcher.hitEnd()) {
            throw new IllegalArgumentException("Invalid JSON format: " + jsonString);
        }
    
        return parsedMap;
    }
    
    // New function to get a specific JSON object from the parsed configuration
    public JSONObject getJSONObject(String key) {
        Object value = data.get(key);
        
        if (value instanceof Map) {
            Map<String, Object> nestedMap = (Map<String, Object>) value;
            return new JSONObject(nestedMap.toString());
        } else {
            System.err.println("Error: Value associated with key '" + key + "' is not a JSON object.");
            // Optionally, you can throw an exception or return an empty JSONObject.
            // For simplicity, let's return an empty JSONObject.
            return new JSONObject();
        }
    }
    
    public static JSONObject readConfigFile(String filePath) {
        try {
            String completeFilePath = "../" + filePath;
            String content = new String(Files.readAllBytes(Paths.get(completeFilePath)));
            return new JSONObject(content);
        } catch (IOException e) {
            System.err.println("Error reading the configuration file '" + filePath + "': " + e.getMessage());
            // Optionally, you can log the exception or rethrow it based on your application's requirements.
            return null; // Or throw a custom exception if needed.
        }
    }    
}
