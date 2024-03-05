package ProductService;

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

import java.sql.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ProductService {
    public static String jdbcUrl = "jdbc:postgresql://localhost:5432/product";
    String username = "postgres"; 
    String password = "password"; 
    private static int requestCount = 0;
    public static void main(String[] args) throws IOException, SQLException {
        // Read the JSON configuration file
        String configFile = args[0];

        try {
            // Read the content of the configuration file
            String configContent = new String(Files.readAllBytes(Paths.get(configFile)));

            JSONObject config = new JSONObject(configContent);
            JSONObject productServiceConfig = config.getJSONObject("ProductService");

            // Extract IP address and port number
            String ipAddress = productServiceConfig.getString("ip");
            int port = productServiceConfig.getInt("port");

            /*For Http request*/
            HttpServer ProductServer = HttpServer.create(new InetSocketAddress(ipAddress, port), 0);
            // Example: Set a custom executor with a fixed-size thread pool
            ProductServer.setExecutor(Executors.newFixedThreadPool(10)); // Adjust the pool size as needed
            // Set up context for /test POST request
            ProductServer.createContext("/product", new TestHandler());

            ProductServer.start();

            System.out.println("Product Server started on port " + port);
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
            String username = "postgres"; 
            String password = "password"; 
            if ("POST".equals(exchange.getRequestMethod())) {
                JSONObject requestbody = getRequestBody(exchange);
                String command = requestbody.getString("command");
                if (requestCount == 1 && !command.equals("restart")) {
                    System.out.println("Creating new database");
                    createNewDatabase();
                }
                if (command.equals("create")) {
                    // Check if this is the first request
                    try  (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)){
                        // Get body parameters
                        int id_int = requestbody.getInt("id");
                        String id = String.valueOf(id_int);
                        String productName = requestbody.getString("name");
                        String description = requestbody.getString("description");
                        double price_double = requestbody.getDouble("price");
                        int quantity_int = requestbody.getInt("quantity");
                
                        // Check for negative quantity or price
                        if (quantity_int < 0 || price_double < 0) {
                            int statusCode = 400; // Bad Request
                            sendResponse(exchange, statusCode, "");
                            return;
                        }
                
                        // Check for duplicate ID
                        String checkDuplicateQuery = "SELECT * FROM Product WHERE productId = ?";
                        PreparedStatement checkDuplicateStatement = connection.prepareStatement(checkDuplicateQuery);
                        checkDuplicateStatement.setInt(1, id_int);
                        ResultSet duplicateResultSet = checkDuplicateStatement.executeQuery();
                
                        if (duplicateResultSet.next()) {
                            // Duplicate ID found
                            int statusCode = 409; // Conflict
                            sendResponse(exchange, statusCode, "");
                            return;
                        }
                
                        // Close resources for duplicate check
                        duplicateResultSet.close();
                        checkDuplicateStatement.close();
                
                        // Continue with insertion
                        String insertQuery = "INSERT INTO Product (productId, productName, description, price, quantity) VALUES (?, ?, ?, ?, ?)";
                        PreparedStatement preparedStatement = connection.prepareStatement(insertQuery);
                        preparedStatement.setInt(1, id_int);
                        preparedStatement.setString(2, productName);
                        preparedStatement.setString(3, description);
                        preparedStatement.setDouble(4, price_double);
                        preparedStatement.setInt(5, quantity_int);
                        int rowsAffected = preparedStatement.executeUpdate();
                        preparedStatement.close();
                
                        JSONObject responseBody = createResponse(exchange, command, id_int);
                
                        // Put in all information that needs to be sent to the client
                        int statusCode = 200;
                        sendResponse(exchange, statusCode, responseBody.toString());
                        if (rowsAffected > 0) {
                            System.out.println("Product information created successfully.");
                        }

                    } catch (SQLException e) {
                        System.out.println("You screw up at post create");
                        // e.printStackTrace();
                    } catch (JSONException e) {
                        System.out.println("Status code 400 in post create");
                        int statusCode = 400;
                        sendResponse(exchange, statusCode, "");
                    }
                } else if (command.equals("update")) {
                    /* update product */
                    try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
                        // Get body parameters
                        int id_int = requestbody.getInt("id");

                        // Build the dynamic part of the UPDATE query based on provided attributes
                        StringBuilder updateQueryBuilder = new StringBuilder("UPDATE Product SET ");
                        List<String> setClauses = new ArrayList<>();
                
                        if (requestbody.has("name")) {
                            setClauses.add("productName = ?");
                        }
                        if (requestbody.has("description")) {
                            setClauses.add("description = ?");
                        }
                        if (requestbody.has("price")) {
                            setClauses.add("price = ?");
                        }
                        if (requestbody.has("quantity")) {
                            setClauses.add("quantity = ?");
                        }
                
                        // Combine the set clauses
                        updateQueryBuilder.append(String.join(", ", setClauses));
                
                        // Add the WHERE clause to identify the product by ID
                        updateQueryBuilder.append(" WHERE productId = ?");
                
                        // Create the prepared statement
                        PreparedStatement preparedStatement = connection.prepareStatement(updateQueryBuilder.toString());
                
                        // Set values for each attribute
                        int parameterIndex = 1;
                        if (requestbody.has("name")) {
                            preparedStatement.setString(parameterIndex++, requestbody.getString("name"));
                        }
                        if (requestbody.has("description")) {
                            preparedStatement.setString(parameterIndex++, requestbody.getString("description"));
                        }
                        if (requestbody.has("price")) {
                            preparedStatement.setDouble(parameterIndex++, requestbody.getDouble("price"));
                        }
                        if (requestbody.has("quantity")) {
                            preparedStatement.setInt(parameterIndex++, requestbody.getInt("quantity"));
                        }
                
                        // Set the productId for the WHERE clause
                        preparedStatement.setInt(parameterIndex, id_int);
                
                        int rowsAffected = preparedStatement.executeUpdate();
                        preparedStatement.close();
                
                        /* Create response JSON */
                
                        // Put in all information that needs to be sent to the client
                        JSONObject responseBody = createResponse(exchange, command, id_int);
                        int statusCode = (rowsAffected > 0) ? 200 : 404; // 404 if no product found
                        sendResponse(exchange, statusCode, responseBody.toString());
                
                        if (rowsAffected > 0) {
                            System.out.println("Product information updated successfully.");
                        } else {
                            System.out.println("No product found with the specified ID.");
                        }
                    } catch (SQLException e) {
                        System.out.println("you screw up at post update");
                        // e.printStackTrace();
                    }
                } else if (command.equals("delete")) {

                    try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
                        int id_int = requestbody.getInt("id");
                        String productName = requestbody.getString("name");
                        String description = requestbody.getString("description");
                        double price = requestbody.getDouble("price");
                        int quantity = requestbody.getInt("quantity");
                
                        String selectQuery = "SELECT * FROM Product WHERE productId = ? AND productName = ? AND description = ? AND price = ? AND quantity = ?";
                        PreparedStatement selectStatement = connection.prepareStatement(selectQuery);
                        selectStatement.setInt(1, id_int);
                        selectStatement.setString(2, productName);
                        selectStatement.setString(3, description);
                        selectStatement.setDouble(4, price);
                        selectStatement.setInt(5, quantity);
                
                        ResultSet resultSet = selectStatement.executeQuery();
                
                        if (resultSet.next()) {
                            // Valid credentials, proceed with deletion
                            resultSet.close();
                            selectStatement.close();
                
                            String deleteQuery = "DELETE FROM Product WHERE productId = ?";
                            PreparedStatement deleteStatement = connection.prepareStatement(deleteQuery);
                            deleteStatement.setInt(1, id_int);
                
                            int rowsAffected = deleteStatement.executeUpdate();
                            deleteStatement.close();
                
                            /* Create response JSON */
                            // Put in all information that needs to be sent to the client
                            JSONObject responseBody = createResponse(exchange, command, id_int);
                
                            int statusCode = (rowsAffected > 0) ? 200 : 404; // 404 if no product found
                            sendResponse(exchange, statusCode, responseBody.toString());
                
                            if (rowsAffected > 0) {
                                System.out.println("Product deleted successfully.");
                            } else {
                                System.out.println("No product found with the specified ID.");
                            }
                        } else {
                            // Invalid credentials, send 401 Unauthorized
                            resultSet.close();
                            selectStatement.close();
                
                            int statusCode = 401;
                            sendResponse(exchange, statusCode, "");
                        }
                    } catch (SQLException e) {
                        System.out.println("you screw up at post delete");
                        // e.printStackTrace();
                    } catch (JSONException e) {
                        System.out.println("status code 400 at post delete");
                        int statusCode = 400;
                        sendResponse(exchange, statusCode, "");
                    }
                } else if (command.equals("shutdown")) {
                    //Additional requirements
                    JSONObject responseBody = new JSONObject();
                    responseBody.put("command", command);
                    sendResponse(exchange, 200, responseBody.toString());

                    System.out.println("Product Server has been shut down gracefully.");
                    System.exit(0); // Exit the application
                } else if (command.equals("restart")) {
                    JSONObject responseBody = new JSONObject();
                    responseBody.put("command", command);
                    sendResponse(exchange, 200, responseBody.toString());
                    System.out.println("Product Server has been restarted.");
                }
            }
            // Handle Get request 
            else if("GET".equals(exchange.getRequestMethod())){
                if (requestCount == 1) {
                    System.out.println("Creating new database");
                    createNewDatabase();
                }
                try{
                    String[] pathSegments = exchange.getRequestURI().getPath().split("/");

                    int id_int = Integer.parseInt(pathSegments[pathSegments.length - 1]);
        
                    // Get product information
                    JSONObject responseBody = createResponse(exchange, "", id_int);
            
                    int statusCode = 200;
                    sendResponse(exchange, statusCode, responseBody.toString());
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                    System.out.println("status code 400 at get");
                    // Handle invalid or missing product ID in the URL
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
            String username = "postgres"; 
            String password = "password"; 
            if ("GET".equals(exchange.getRequestMethod())) {
                try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {    
                    String selectQuery = "SELECT * FROM Product WHERE productId = ?";
                    PreparedStatement preparedStatement = connection.prepareStatement(selectQuery);
                    preparedStatement.setInt(1, id_int);

                    ResultSet resultSet = preparedStatement.executeQuery();
                    if (resultSet.next()) {
                        // Fetch user details from the result set
                        String productName_get = resultSet.getString("productName");
                        String description_get = resultSet.getString("description");
                        double price_get = resultSet.getDouble("price");
                        Integer quantity_get = resultSet.getInt("quantity");

                        // Create a JSONObject with the fetched user details
                        JSONObject responseBody = new JSONObject()
                                .put("id", id_int)
                                .put("name", productName_get)
                                .put("description", description_get)
                                .put("price", price_get)
                                .put("quantity", quantity_get);

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
                    System.out.println("screw up with get method in create response");
                    // e.printStackTrace();
                }
                return null; // Return null if there's an error or if the user is not found

            } else if ("POST".equals(exchange.getRequestMethod())) {
                if (!command.equals("delete")) {
                    try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
                        String selectQuery = "SELECT * FROM Product WHERE productId = ?";
                        PreparedStatement preparedStatement = connection.prepareStatement(selectQuery);
                        preparedStatement.setInt(1, id_int);
            
                        ResultSet resultSet = preparedStatement.executeQuery();
                        if (resultSet.next()) {
                            // Fetch product details from the result set
                            String productName = resultSet.getString("productName");
                            String description = resultSet.getString("description");
                            double price = resultSet.getDouble("price");
                            int quantity = resultSet.getInt("quantity");
            
                            // Create a JSONObject with the fetched product details
                            JSONObject responseBody = new JSONObject()
                                    .put("id", id_int)
                                    .put("name", productName)
                                    .put("description", description)
                                    .put("price", price)
                                    .put("quantity", quantity);
            
                            // Close resources
                            resultSet.close();
                            preparedStatement.close();
            
                            return responseBody;
                        } else {
                            // Product not found
                            System.out.println("No product found with the specified ID.");
                        }
            
                        // Close resources
                        resultSet.close();
                        preparedStatement.close();
            
                    } catch (SQLException e) {
                        System.out.println("screw up at create response post method with create/update");
                        // e.printStackTrace();
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
            File databaseFile = new File("db/ProductDatabase.db");
            String username = "postgres"; 
            String password = "password"; 
        
            // Check if the database file exists
            if (databaseFile.exists()) {
                // Delete the existing database file
                if (databaseFile.delete()) {
                    System.out.println("Existing database deleted successfully.");
                } else {
                    System.out.println("Failed to delete the existing database.");
                }
            }
            try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
                // Create the Product table in the new database
                String createTableQuery = "CREATE TABLE IF NOT EXISTS Product ("
                        + "productId INTEGER PRIMARY KEY,"
                        + "productName TEXT NOT NULL,"
                        + "description TEXT NOT NULL,"
                        + "price INTEGER NOT NULL,"
                        + "quantity INTEGER NOT NULL)";
                try (Statement statement = connection.createStatement()) {
                    // Execute the query to create the Product table
                    statement.executeUpdate(createTableQuery);
                    System.out.println("Product table created successfully in a new database.");
                } catch (SQLException sqle) {
                    System.out.println("Error creating Product table: " + sqle.getMessage());
                }
        
            } catch (SQLException e) {
                System.out.println("Error creating new database: " + e.getMessage());
            }
        }
        
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.sendResponseHeaders(statusCode, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes(StandardCharsets.UTF_8));
        os.close();
    }

}
