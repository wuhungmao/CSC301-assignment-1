import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JSONObject {
    //Create a JSON class that hold JSON objects
        private final Map<String, Object> data;

        // Updated constructor for parsing JSON string
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
                result.append("    \"").append(entry.getKey()).append("\": ").append(entry.getValue()).append(",\n");
            }
            result.append("}");
            return result.toString();
        }

        
        // Helper method to parse JSON string
        private Map<String, Object> parseJson(String jsonString) {
            Map<String, Object> parsedMap = new HashMap<>();

            // Match key-value pairs using regular expression
            Pattern pattern = Pattern.compile("\"(\\w+)\":\"([^\"]*)\"");
            Matcher matcher = pattern.matcher(jsonString);

            while (matcher.find()) {
                String key = matcher.group(1);
                String value = matcher.group(2);
                parsedMap.put(key, value);
            }

            return parsedMap;
        }
    }
