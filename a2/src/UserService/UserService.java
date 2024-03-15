package UserService;

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
import org.json.JSONArray;

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

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
/*users Service:
This must be written in Java and must be started with the following command: "java UserService config.json"
Implement a users microservice responsible for users management.
users should have attributes such as id, username, email, and password.
Provide endpoints for users creation, retrieval, updating, and deletion.
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

==> Updates users with id 23823
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
==> delete this users ONLY IFF all fields (username, email and password) correspond. 

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
    public static Map<String, JSONObject> cache = new HashMap<>();
    private static HikariDataSource dataSource;
    //Delete this after
    private static int requestCount = 0;
    public static void main(String[] args) throws IOException, SQLException {
        // Read the JSON configuration file
        String configFile = args[0];
        try {
            String configContent = new String(Files.readAllBytes(Paths.get(configFile)));
            JSONObject config = new JSONObject(configContent);
            JSONObject productServiceConfig = config.getJSONObject("ProductService");

            // Extract IP address
            String ipAddress = productServiceConfig.getString("ip");
            // Extract array of ports
            JSONArray ports = productServiceConfig.getJSONArray("ports");

            HikariConfig configData = new HikariConfig();
            //public static String url = "jdbc:postgresql://172.17.0.2:5432/users";
            configData.setJdbcUrl("jdbc:postgresql://127.0.0.1:5432/users");
            configData.setUsername("postgres");
            configData.setPassword("password");
            configData.setMaximumPoolSize(100); 
            configData.setMinimumIdle(10); 
            configData.setIdleTimeout(30000); 
            configData.setConnectionTimeout(30000); 
    
            dataSource = new HikariDataSource(configData);

            // Create and start an HttpServer for each port
            for (int i = 0; i < ports.length(); i++) {
                int port = ports.getInt(i);
                HttpServer ProductServer = HttpServer.create(new InetSocketAddress(ipAddress, port), 0);
                ProductServer.setExecutor(Executors.newFixedThreadPool(10));
                ProductServer.createContext("/product", new TestHandler());
                ProductServer.start();
                System.out.println("Product Server started on port " + port);
            }
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
            // Handle POST request for /test
            if ("POST".equals(exchange.getRequestMethod())) {
                JSONObject requestbody = getRequestBody(exchange);  
                String command = requestbody.getString("command");
                if (requestCount == 1 && !command.equals("restart")) {
                    createNewDatabase();
                }
                if (command.equals("create")) 
                {
                    try (Connection connection = dataSource.getConnection()) {
                        /* Get values */
                        int id_int = requestbody.getInt("id");
                        String id = String.valueOf(id_int);
                        String username = requestbody.getString("username");
                        String email = requestbody.getString("email");
                        String password = requestbody.getString("password");
                        // //System.out,or("before testing");
                        
                        if (id.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                            // Send a response indicating a bad request due to empty fields
                            int statusCode = 400; // Bad request
                            JSONObject responseBody = new JSONObject();
                            sendResponse(exchange, statusCode, responseBody.toString());
                            return; // Return from the method to prevent further execution
                        }

                        boolean ifduplicate = duplicate(id_int, connection);
                        if (ifduplicate) {
                            int statusCode = 409;
                            JSONObject responseBody = new JSONObject();
                            sendResponse(exchange, statusCode, responseBody.toString());    
                        }

                        //cache
                        JSONObject info = new JSONObject();
                        info.put("username", username);
                        info.put("email", email);
                        info.put("password", password);
                        cache.put(id, info);

                        String insertQuery = "INSERT INTO Users (user_id, username, email, password) VALUES (?, ?, ?, ?)";
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
                    } catch (JSONException e) {
                        int statusCode = 400;
                        JSONObject responseBody = new JSONObject();
                        sendResponse(exchange, statusCode, responseBody.toString());
                    } catch (SQLException e) {
                        int statusCode = 409;
                        JSONObject responseBody = new JSONObject();
                        sendResponse(exchange, statusCode, responseBody.toString());    
                    }
                }
                else if (command.equals("update")) 
                {
                    /*update users */
                    try (Connection connection = dataSource.getConnection()) {
                        if(requestbody.has("id")) {

                            int id_int = requestbody.getInt("id");
                            String id = String.valueOf(id_int);//for cache
                            
                            // Build the dynamic part of the UPDATE query based on provided attributes
                            StringBuilder updateQueryBuilder = new StringBuilder("UPDATE users SET ");
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
                            
                            if (!requestbody.has("username") && !requestbody.has("email") && !requestbody.has("password")){
                                //If none of them is present, don't execute the query
                                //Have to catch this edge case here before sqlexception get thrown
                                JSONObject responseBody = createResponse(exchange, command, id_int);
                                int statusCode = 400;
                                sendResponse(exchange, statusCode, responseBody.toString());
                            } else {
                                Object emailValue = requestbody.get("email");
                                if ((emailValue instanceof String) && !((String)emailValue).isEmpty()) {
                                    ////System.out,or("before updatequerybuilder");
                                    // Combine the set clauses
                                    updateQueryBuilder.append(String.join(", ", setClauses));
                                    
                                    // Add the WHERE clause to identify the users by ID
                                    updateQueryBuilder.append(" WHERE user_id = ? ");
                                    
                                    ////System.out,or("after updatequerybuilder");
                                    
                                    // Create the prepared statement
                                    PreparedStatement preparedStatement = connection.prepareStatement(updateQueryBuilder.toString());
                                    
                                    ////System.out,or("suspected error 1");

                                    JSONObject original_info = cache.get(id);//get original jsonobject in cache for updating
                                    // Set values for each attribute
                                    int parameterIndex = 1;
                                    if (requestbody.has("username")) {
                                        preparedStatement.setString(parameterIndex++, requestbody.getString("username"));
                                        original_info.put("username", requestbody.getString("username"));
                                    }
                                    if (requestbody.has("email")) {
                                        preparedStatement.setString(parameterIndex++, requestbody.getString("email"));
                                        original_info.put("email", requestbody.getString("email"));
                                    }
                                    if (requestbody.has("password")) {
                                        preparedStatement.setString(parameterIndex++, requestbody.getString("password"));
                                        original_info.put("password", requestbody.getString("password"));
                                    }
                                    
                                    // Set the user_id for the WHERE clause
                                    preparedStatement.setInt(parameterIndex, id_int);
                                    
                                    int rowsAffected = 0;
                                    rowsAffected = preparedStatement.executeUpdate();
                                    preparedStatement.close();
                                    
                                    /* Create response JSON */
                                    //Put in all information that needs to be sent to the client
                                    JSONObject responseBody = createResponse(exchange, command, id_int);
                                    int statusCode = (rowsAffected > 0) ? 200 : 404;
                                    sendResponse(exchange, statusCode, responseBody.toString());
                                    
                                    if (rowsAffected > 0) {
                                        cache.put(id ,original_info);// update for cache
                                        //System.out,or("Users information updated successfully.");
                                    } else {
                                        //System.out,or("No users found with the specified ID.");
                                    }
                                } else {
                                    //Invalid email type
                                    JSONObject responseBody = new JSONObject();
                                    int statusCode = 400;
                                    sendResponse(exchange, statusCode, responseBody.toString());
                                }
                            }
                        } else {
                            //No users id. Return 400 with empty json
                            JSONObject responseBody = new JSONObject();
                            int statusCode = 400;
                            sendResponse(exchange, statusCode, responseBody.toString());
                        }
                    } catch (SQLException e) {
                        int statusCode = 400;
                        //System.out,or("sql exception");
                        JSONObject responseBody = new JSONObject();
                        sendResponse(exchange, statusCode, responseBody.toString());
                    }
                } 
                else if (command.equals("delete")) {
                    try (Connection connection = dataSource.getConnection()) {
                        int id_int = requestbody.getInt("id");
                        String id = String.valueOf(id_int);//for cache
                        String username = requestbody.getString("username");
                        String email = requestbody.getString("email");
                        String password = requestbody.getString("password");
                
                        String selectQuery = "SELECT * FROM users WHERE user_id = ? AND username = ? AND email = ? AND password = ?";
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
                
                            String deleteQuery = "DELETE FROM users WHERE user_id = ?";
                            PreparedStatement deleteStatement = connection.prepareStatement(deleteQuery);
                            deleteStatement.setInt(1, id_int);
                
                            int rowsAffected = deleteStatement.executeUpdate();
                            deleteStatement.close();
                
                            /* Create response JSON */
                            // Put in all information that needs to be sent to the client
                            JSONObject responseBody = createResponse(exchange, command, id_int);
                
                            int statusCode = (rowsAffected > 0) ? 200 : 404; // 404 if no users found
                            sendResponse(exchange, statusCode, responseBody.toString());
                
                            if (rowsAffected > 0) {
                                cache.remove(id);//delete from cache
                                //System.out,or("Users deleted successfully.");
                            } else {
                                //System.out,or("No users found with the specified ID.");
                            }
                        } else {
                            // Invalid credentials, send 401 Unauthorized
                            resultSet.close();
                            selectStatement.close();
                            JSONObject responseBody = new JSONObject();
                            int statusCode = 401;
                            sendResponse(exchange, statusCode, responseBody.toString());
                        }
                    } catch (SQLException e) {
                        int statusCode = 400;
                        //System.out,or("sql exception");
                        JSONObject responseBody = new JSONObject();
                        sendResponse(exchange, statusCode, responseBody.toString());
                    } catch (JSONException e) {
                        int statusCode = 400;
                        //System.out,or("json exception");
                        JSONObject responseBody = new JSONObject();
                        sendResponse(exchange, statusCode, responseBody.toString());
                    }
                } else if (command.equals("shutdown")) {
                    //Additional requirements
                    JSONObject responseBody = new JSONObject();
                    responseBody.put("command", command);
                    sendResponse(exchange, 200, responseBody.toString());

                    //System.out,or("users Server has been shut down gracefully.");
                    System.exit(0); // Exit the application
                } else if (command.equals("restart")) {
                    JSONObject responseBody = new JSONObject();
                    responseBody.put("command", command);
                    sendResponse(exchange, 200, responseBody.toString());
                    //System.out,or("users Server has been restarted.");
                }
            }
            // Handle Get request 
            else if("GET".equals(exchange.getRequestMethod())){
                if (requestCount == 1) {
                    //System.out,or("Creating new database");
                    createNewDatabase();
                }
                try {
                    // Extract users ID from the request URI
                    //System.out,or("in get");

                    String[] pathSegments = exchange.getRequestURI().getPath().split("/");
                    int id_int = Integer.parseInt(pathSegments[pathSegments.length - 1]);
                    String id = String.valueOf(id_int);//for cache
                    //System.out,or("id_int is "+ String.valueOf(id_int));
                    // Get Users information
//                    JSONObject responseBody = createResponse(exchange, "", id_int);
//
//                    int statusCode = 200;
//                    sendResponse(exchange, statusCode, responseBody.toString());
                    if (cache.containsKey(id)) {//get info directly from cache
                        JSONObject responseBody = cache.get(id);
                        int statusCode = 200;
                        sendResponse(exchange, statusCode, responseBody.toString());
                    }else{
                        int statusCode = 400;
                        JSONObject responseBody = new JSONObject();
                        sendResponse(exchange, statusCode, responseBody.toString());
                    }

                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                    // Handle invalid or missing users ID in the URL
                    int statusCode = 400;
                    JSONObject responseBody = new JSONObject();
                    sendResponse(exchange, statusCode, responseBody.toString());
                }
            }
        }
        
        // Returns true if there is at least one row in the result set
        private static boolean duplicate(int id_int, Connection connection ) throws SQLException {
            String selectQuery = "SELECT * FROM users WHERE user_id = ?";
            PreparedStatement selectStatement = connection.prepareStatement(selectQuery);
            selectStatement.setInt(1, id_int);
            
            ResultSet resultSet = selectStatement.executeQuery();
            return resultSet.next(); 
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
                try (Connection connection = dataSource.getConnection()) {
                    String selectQuery = "SELECT * FROM users WHERE user_id = ?";
                    PreparedStatement preparedStatement = connection.prepareStatement(selectQuery);
                    preparedStatement.setInt(1, id_int);

                    ResultSet resultSet = preparedStatement.executeQuery();
                    if (resultSet.next()) {
                        // Fetch users details from the result set
                        String Username = resultSet.getString("username");
                        String Email = resultSet.getString("email");
                        String Password = resultSet.getString("password");

                        // Create a JSONObject with the fetched users details
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
                        // users not found
                        JSONObject responseBody = new JSONObject();
                        // Close resources
                        resultSet.close();
                        preparedStatement.close();

                        return responseBody;
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                return null; // Return null if there's an error or if the users is not found

            } else if ("POST".equals(exchange.getRequestMethod())) {
                if (!command.equals("delete")) {
                    try  (Connection connection = dataSource.getConnection()){
                        String selectQuery = "SELECT * FROM users WHERE user_id = ?";
                        PreparedStatement preparedStatement = connection.prepareStatement(selectQuery);
                        preparedStatement.setInt(1, id_int);

                        ResultSet resultSet = preparedStatement.executeQuery();
                        if (resultSet.next()) {
                            // Fetch users details from the result set
                            String Username = resultSet.getString("username");
                            String Email = resultSet.getString("email");
                            String Password = resultSet.getString("password");

                            // Create a JSONObject with the fetched users details
                            JSONObject responseBody = new JSONObject()
                                    .put("id", id_int)
                                    .put("username", Username)
                                    .put("email", Email)
                                    .put("password", hashPassword(Password));

                            // Close resources
                            resultSet.close();
                            preparedStatement.close();

                            return responseBody;
                        } else {
                            // users not found
                            //System.out,or("No users found with the specified ID.");
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
                    //System.out,or("Existing database deleted successfully.");
                } else {
                    //System.out,or("Failed to delete the existing database.");
                }
            }
            try (Connection connection = dataSource.getConnection()) {
                // Create the Product table in the new database
                                    
                //Before starting server, create users database
                String createTableQuery = "CREATE TABLE IF NOT EXISTS users ("
                            + "user_id INTEGER PRIMARY KEY,"
                            + "username TEXT NOT NULL,"
                            + "email TEXT NOT NULL,"
                            + "password TEXT NOT NULL)";
                
                try (Statement statement = connection.createStatement()) {
                    // Execute the query to create the users table
                    statement.executeUpdate(createTableQuery);
                    //System.out,or("users table created successfully.");
                } catch (SQLException sqle) {
                    //System.out,or("Error creating users table: " + sqle.getMessage());
                }
            } catch (SQLException e) {
                //System.out,or("Error creating new database: " + e.getMessage());
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

