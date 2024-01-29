import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

//import JSONObject
import org.json.JSONObject;
import org.json.JSONException;
//Database
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
    public static String jdbcUrl = "jdbc:sqlite:db/UserDatabase.db";
    private static int requestCount = 0;
    public static void main(String[] args) throws IOException, SQLException {
        // Read the JSON configuration file
        String configFile = args[0];
        try {
            String configContent = new String(Files.readAllBytes(Paths.get(configFile)));

            JSONObject config = new JSONObject(configContent);
            JSONObject userServiceConfig = config.getJSONObject("UserService");

            // Extract IP address and port number
            String ipAddress = userServiceConfig.getString("ip");
            int port = userServiceConfig.getInt("port");

            /*For Http request*/
            HttpServer userServer = HttpServer.create(new InetSocketAddress(ipAddress, port), 0);
            // Example: Set a custom executor with a fixed-size thread pool
            userServer.setExecutor(Executors.newFixedThreadPool(10)); // Adjust the pool size as needed
            // Set up context for /test POST request
            userServer.createContext("/user", new TestHandler());

            userServer.start();

            System.out.println("user Server started on port " + port);
        } catch (IOException e) {
            System.err.println("Error reading the configuration file: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("An unexpected error occurred: " + e.getMessage());
        }
    }


    static class TestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            requestCount++;
            // Check if this is the first request
            if (requestCount == 1){
                if("POST".equals(exchange.getRequestMethod())) {
                    JSONObject requestbody = getRequestBody(exchange);
                    String command = requestbody.getString("command");
                    if (!command.equals("restart")) {
                        System.out.println("Creating new database");
                        createNewDatabase();
                    }
                } else {
                    System.out.println("Creating new database");
                    createNewDatabase();
                }
            }

            // Handle POST request for /test
            if ("POST".equals(exchange.getRequestMethod())) {
                JSONObject requestbody = getRequestBody(exchange);  
                String command = requestbody.getString("command");
                if (command.equals("create")) 
                {
                    try (Connection connection = DriverManager.getConnection(jdbcUrl)) {
                        /* Get values */
                        int id_int = requestbody.getInt("id");
                        String id = String.valueOf(id_int);
                        String username = requestbody.getString("username");
                        String email = requestbody.getString("email");
                        String password = requestbody.getString("password");

                        String insertQuery = "INSERT INTO User (user_id, username, email, password) VALUES (?, ?, ?, ?)";
                        PreparedStatement preparedStatement = connection.prepareStatement(insertQuery);
                        preparedStatement.setInt(1, id_int);
                        preparedStatement.setString(2, username);
                        preparedStatement.setString(3, email);
                        preparedStatement.setString(4, password);
                        int rowsAffected = preparedStatement.executeUpdate();
                        preparedStatement.close();
                        /* Create response JSON */

                        //Put in all information that needs to be sent to the client
                        JSONObject responseBody = createResponse(exchange, command, id_int);
                        int statusCode = 200;
                        sendResponse(exchange, statusCode, responseBody.toString());

                        if (rowsAffected > 0) {
                            System.out.println("User information created successfully.");
                        }

                    } catch (SQLException e) {
                        if (e.getSQLState().equals("23505")) {
                            // 23505 is the SQLState code for a unique constraint violation
                            int statusCode = 409;
                            JSONObject responseBody = new JSONObject();
                            responseBody.put("error", "Conflict");
                            responseBody.put("message", "The 'user_id' already exists.");
                            sendResponse(exchange, statusCode, responseBody.toString());
                        } else {
                            // Handle other SQL exceptions
                            e.printStackTrace();
                        }
                    } catch (JSONException e) {
                        int statusCode = 400;
                        sendResponse(exchange, statusCode, "");
                    }
                }
                else if (command.equals("update")) 
                {
                    /*update user */
                    try (Connection connection = DriverManager.getConnection(jdbcUrl)) {
                        int id_int = requestbody.getInt("id");

                        // Build the dynamic part of the UPDATE query based on provided attributes
                        StringBuilder updateQueryBuilder = new StringBuilder("UPDATE User SET ");
                        List<String> setClauses = new ArrayList<>();

                        if (requestbody.has("username")) {
                            setClauses.add("username = ?");
                        }
                        if (requestbody.has("email")) {
                            setClauses.add("email = ?");
                        }
                        if (requestbody.has("password")) {
                            setClauses.add("password = ?");
                        }

                        // Combine the set clauses
                        updateQueryBuilder.append(String.join(", ", setClauses));

                        // Add the WHERE clause to identify the user by ID
                        updateQueryBuilder.append(" WHERE user_id = ?");

                        // Create the prepared statement
                        PreparedStatement preparedStatement = connection.prepareStatement(updateQueryBuilder.toString());

                        // Set values for each attribute
                        int parameterIndex = 1;
                        if (requestbody.has("username")) {
                            preparedStatement.setString(parameterIndex++, requestbody.getString("username"));
                        }
                        if (requestbody.has("email")) {
                            preparedStatement.setString(parameterIndex++, requestbody.getString("email"));
                        }
                        if (requestbody.has("password")) {
                            preparedStatement.setString(parameterIndex++, requestbody.getString("password"));
                        }

                        // Set the user_id for the WHERE clause
                        preparedStatement.setInt(parameterIndex, id_int);

                        int rowsAffected = preparedStatement.executeUpdate();
                        preparedStatement.close();

                        /* Create response JSON */

                        //Put in all information that needs to be sent to the client
                        JSONObject responseBody = createResponse(exchange, command, id_int);
                        int statusCode = (rowsAffected > 0) ? 200 : 404;
                        sendResponse(exchange, statusCode, responseBody.toString());

                        if (rowsAffected > 0) {
                            System.out.println("User information updated successfully.");
                        } else {
                            System.out.println("No user found with the specified ID.");
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                } 
                else if (command.equals("delete")) {
                    try (Connection connection = DriverManager.getConnection(jdbcUrl)) {
                        int id_int = requestbody.getInt("id");
                        String username = requestbody.getString("username");
                        String email = requestbody.getString("email");
                        String password = requestbody.getString("password");
                
                        String selectQuery = "SELECT * FROM User WHERE user_id = ? AND username = ? AND email = ? AND password = ?";
                        PreparedStatement selectStatement = connection.prepareStatement(selectQuery);
                        selectStatement.setInt(1, id_int);
                        selectStatement.setString(2, username);
                        selectStatement.setString(3, email);
                        selectStatement.setString(4, password);
                
                        ResultSet resultSet = selectStatement.executeQuery();
                
                        if (resultSet.next()) {
                            // Valid credentials, proceed with deletion
                            resultSet.close();
                            selectStatement.close();
                
                            String deleteQuery = "DELETE FROM User WHERE user_id = ?";
                            PreparedStatement deleteStatement = connection.prepareStatement(deleteQuery);
                            deleteStatement.setInt(1, id_int);
                
                            int rowsAffected = deleteStatement.executeUpdate();
                            deleteStatement.close();
                
                            /* Create response JSON */
                            // Put in all information that needs to be sent to the client
                            JSONObject responseBody = createResponse(exchange, command, id_int);
                
                            int statusCode = (rowsAffected > 0) ? 200 : 404; // 404 if no user found
                            sendResponse(exchange, statusCode, responseBody.toString());
                
                            if (rowsAffected > 0) {
                                System.out.println("User deleted successfully.");
                            } else {
                                System.out.println("No user found with the specified ID.");
                            }
                        } else {
                            // Invalid credentials, send 401 Unauthorized
                            resultSet.close();
                            selectStatement.close();
                
                            int statusCode = 401;
                            sendResponse(exchange, statusCode, "");
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        int statusCode = 400;
                        sendResponse(exchange, statusCode, "");
                    }
                } else if (command.equals("shutdown")) {
                    //Additional requirements
                    JSONObject responseBody = new JSONObject();
                    responseBody.put("command", command);
                    sendResponse(exchange, 200, responseBody);

                    System.out.println("User Server has been shut down gracefully.");
                    System.exit(0); // Exit the application
                } else if (command.equals("restart")) {
                    JSONObject responseBody = new JSONObject();
                    responseBody.put("command", command);
                    sendResponse(exchange, 200, responseBody);
                    System.out.println("User Server has been restarted.");
                }
            }
            // Handle Get request 
            else if("GET".equals(exchange.getRequestMethod())){
                try {
                    // Extract user ID from the request URI
                    String[] pathSegments = exchange.getRequestURI().getPath().split("/");
                    int id_int = Integer.parseInt(pathSegments[pathSegments.length - 1]);
            
                    // Get user information
                    JSONObject responseBody = createResponse(exchange, "", id_int);
            
                    int statusCode = 200;
                    sendResponse(exchange, statusCode, responseBody.toString());
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                    // Handle invalid or missing user ID in the URL
                    int statusCode = 400;
                    sendResponse(exchange, statusCode, "");
                }
            }
        }
        
        //Get request message if it is a post request
        private static JSONObject getRequestBody(HttpExchange exchange) throws IOException {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
                StringBuilder requestBody = new StringBuilder();
                String line;

                while ((line = br.readLine()) != null) {
                    requestBody.append(line);
                }

                return new JSONObject(requestBody.toString());
            }
        }

        private static JSONObject createResponse(HttpExchange exchange, String command, Integer id_int) {
            if ("GET".equals(exchange.getRequestMethod())) {
                try (Connection connection = DriverManager.getConnection(jdbcUrl)) {
                    String selectQuery = "SELECT * FROM User WHERE user_id = ?";
                    PreparedStatement preparedStatement = connection.prepareStatement(selectQuery);
                    preparedStatement.setInt(1, id_int);

                    ResultSet resultSet = preparedStatement.executeQuery();
                    if (resultSet.next()) {
                        // Fetch user details from the result set
                        String Username = resultSet.getString("username");
                        String Email = resultSet.getString("email");
                        String Password = resultSet.getString("password");

                        // Create a JSONObject with the fetched user details
                        JSONObject responseBody = new JSONObject()
                                .put("id", id_int)
                                .put("username", Username)
                                .put("email", Email)
                                .put("password", Password);

                        // Close resources
                        resultSet.close();
                        preparedStatement.close();

                        return responseBody;
                    } else {
                        // User not found
                        System.out.println("No user found with the specified ID.");
                    }

                    // Close resources
                    resultSet.close();
                    preparedStatement.close();

                } catch (SQLException e) {
                    e.printStackTrace();
                }

                return null; // Return null if there's an error or if the user is not found

            } else if ("POST".equals(exchange.getRequestMethod())) {
                if (!command.equals("delete")) {
                    try  (Connection connection = DriverManager.getConnection(jdbcUrl)){
                        String selectQuery = "SELECT * FROM User WHERE user_id = ?";
                        PreparedStatement preparedStatement = connection.prepareStatement(selectQuery);
                        preparedStatement.setInt(1, id_int);

                        ResultSet resultSet = preparedStatement.executeQuery();
                        if (resultSet.next()) {
                            // Fetch user details from the result set
                            String Username = resultSet.getString("username");
                            String Email = resultSet.getString("email");
                            String Password = resultSet.getString("password");

                            // Create a JSONObject with the fetched user details
                            JSONObject responseBody = new JSONObject()
                                    .put("id", id_int)
                                    .put("username", Username)
                                    .put("email", Email)
                                    .put("password", Password);

                            // Close resources
                            resultSet.close();
                            preparedStatement.close();

                            return responseBody;
                        } else {
                            // User not found
                            System.out.println("No user found with the specified ID.");
                        }

                        // Close resources
                        resultSet.close();
                        preparedStatement.close();

                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                } else {
                    // delete command requires an empty response
                    return new JSONObject();
                }
            }

            // Add a default return statement in case the request method is neither "GET" nor "POST"
            return new JSONObject();
        }

        public static String hashPassword(String password) {
            try {
                // Create a SHA-256 message digest
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
    
                // Update the digest with the password bytes
                byte[] hashedBytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
    
                // Convert the byte array to a hexadecimal string
                StringBuilder hexString = new StringBuilder();
                for (byte hashedByte : hashedBytes) {
                    String hex = Integer.toHexString(0xff & hashedByte);
                    if (hex.length() == 1) {
                        hexString.append('0');
                    }
                    hexString.append(hex);
                }
    
                return hexString.toString();
            } catch (NoSuchAlgorithmException e) {
                // Handle the exception (e.g., log, print, or perform error handling)
                throw new RuntimeException("Error hashing password: " + e.getMessage());
            }
        }
        
        private static void createNewDatabase() {
            File databaseFile = new File("db/UserDatabase.db");
        
            // Check if the database file exists
            if (databaseFile.exists()) {
                // Delete the existing database file
                if (databaseFile.delete()) {
                    System.out.println("Existing database deleted successfully.");
                } else {
                    System.out.println("Failed to delete the existing database.");
                }
            }
            try (Connection connection = DriverManager.getConnection(jdbcUrl)) {
                // Create the Product table in the new database
                                    
                //Before starting server, create User database
                String createTableQuery = "CREATE TABLE IF NOT EXISTS User ("
                            + "user_id INTEGER PRIMARY KEY,"
                            + "username TEXT NOT NULL,"
                            + "email TEXT NOT NULL,"
                            + "password TEXT NOT NULL)";
                
                try (Statement statement = connection.createStatement()) {
                    // Execute the query to create the User table
                    statement.executeUpdate(createTableQuery);
                    System.out.println("User table created successfully.");
                } catch (SQLException sqle) {
                    System.out.println("Error creating User table: " + sqle.getMessage());
                }
            } catch (SQLException e) {
                System.out.println("Error creating new database: " + e.getMessage());
            }
        }
    
        private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
            exchange.sendResponseHeaders(statusCode, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes(StandardCharsets.UTF_8));
            os.close();
        }
    }
}

