import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

/* Need to install jackson library to process JSON request body faster, 
   How to use JAR file to overcome this? */
// import com.fasterxml.jackson.databind.JsonNode;
// import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
/*User Service:
This must be written in Java and must be started with the following command: "java UserService config.json"
Implement a user microservice responsible for user management.
Users should have attributes such as id, username, email, and password.
Provide endpoints for user creation, retrieval, updating, and deletion.
API endpoint: /user
POST Methods: 
{
    "command": "create",
    "id": 23823,
    "username": "username-32843hnksjn4398",
    "email": "foo@bar.com",
    "password": "34289nkjni3w4u"
}
==> Creates an entry in the DB for a user. 

{
    "command": "update",
    "id": 23823,
    "username": "username-32843hnksjn4398",
    "email": "foo@foobar.com",
    "password": "34289nkjni3w4u"
}

==> Updates user with id 23823
--> Updates all fields that are present (username, email, password); 
--> If fields are missing, only update the fields that are present. 
(e.g., if no "password" is transmitted, then only update the username and email).

{
    "command": "delete",
    "id": 23823,
    "username": "username-32843hnksjn4398",
    "email": "foo@bar.com",
    "password": "34289nkjni3w4u"
}
==> delete this user ONLY IFF all fields (username, email and password) correspond. 

GET Methods:
/user/23823
==> 
returns the following JSON in the response body. 
{
    "id": 23823,
    "username": "username-32843hnksjn4398",
    "email": "foo@bar.com",
}
*/
public class UserService {
    public static void main(String[] args) throws IOException {
        /*For Http request*/
        int port = 80;
        HttpServer UserServer = HttpServer.create(new InetSocketAddress(port), 0);
        // Example: Set a custom executor with a fixed-size thread pool
        UserServer.setExecutor(Executors.newFixedThreadPool(1)); // Adjust the pool size as needed
        // Set up context for /test POST request
        UserServer.createContext("/user", new TestHandler());

        UserServer.setExecutor(null); // creates a default executor

        UserServer.start();

        System.out.println("User Server started on port " + port);
    }
    /* Need to do:
     * 1. Method that create response in JSON format
     * 2. Create database and execute command
     */
    static class TestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Handle POST request for /test
            if ("POST".equals(exchange.getRequestMethod())) {
                String clientAddress = exchange.getRemoteAddress().getAddress().toString();
                String requestMethod = exchange.getRequestMethod();
                String requestURI = exchange.getRequestURI().toString();
                Map<String, List<String>> requestHeaders = exchange.getRequestHeaders();

                System.out.println("Client Address: " + clientAddress);
                System.out.println("Request Method: " + requestMethod);
                System.out.println("Request URI: " + requestURI);
                System.out.println("Request Headers: " + requestHeaders);
                // Print all request headers
                for (Map.Entry<String, List<String>> header : requestHeaders.entrySet()) {
                    System.out.println(header.getKey() + ": " + header.getValue().getFirst());
                }

                JSONObject requestbody = getRequestBody(exchange);
                
                /* Get values */
                String command = requestbody.getString("command");

                /* How do we handle the database and that sort of things */
                if (command.equals("create")) {
                    /*create new user */
                    String username = requestbody.getString("username");
                    String email = requestbody.getString("email");
                    String password = requestbody.getString("password");
                }
                else if (command.equals("update")) {
                    /*update user */
                    int id = requestbody.getInt("id");
                    String username = requestbody.getString("username");
                    String email = requestbody.getString("email");
                    String password = requestbody.getString("password");
                } 
                else if (command.equals("delete")) {
                    /* delete user */
                    int id = requestbody.getInt("id");
                    String username = requestbody.getString("username");
                    String email = requestbody.getString("email");
                    String password = requestbody.getString("password");
                }
                /* Create response JSON */
                String response = "Status code: 200";
                sendResponse(exchange, response);
            }
            else if("GET".equals(exchange.getRequestMethod())){
                JSONObject requestbody = getRequestBody(exchange);
                int id = requestbody.getInt("id");
                String username = requestbody.getString("username");
                String email = requestbody.getString("email");
                String password = requestbody.getString("password");

            }
        }
        //Get request message if it is a post request
        private static JSONObject getRequestBody(HttpExchange exchange) throws IOException {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
                /* Three different post method, create/update/delete */
                StringBuilder requestBody = new StringBuilder();

                /* Are we guaranteed to get a json format string like 
                "{\n" +
                "    \"command\": \"delete\",\n" +
                "    \"id\": 23823,\n" +
                "    \"username\": \"username-32843hnksjn4398\",\n" +
                "    \"email\": \"foo@bar.com\",\n" +
                "    \"password\": \"34289nkjni3w4u\"\n" +
                "}"*/
                JSONObject jsonObject = new JSONObject(requestBody);
                return jsonObject;
            } catch (JSONException e) {
                // Handle the exception (e.g., log, print, or perform error handling)
                System.err.println("Error parsing JSON: " + e.getMessage());
            }
        }
    }

    
    private static void sendResponse(HttpExchange exchange, String response) throws IOException {
        exchange.sendResponseHeaders(200, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes(StandardCharsets.UTF_8));
        os.close();
    }
}

