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

//Database
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.sqlite.JDBC;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
/*
Product Service:
This must be written in Java.
Implement a product microservice responsible for managing products in an e-commerce system.
Products should have attributes such as id, name, description, price, and quantity in stock.
Provide endpoints for product creation, updating, info, and deletion.
API endpoint: /product
POST Methods: 
{
    "command": "create/update/delete",
    "id": 23823,
    "productname": "productname-32843hnksjn4398",
    "price": 3.99 ,
    "quantity": 9
}
==> Creates update, delete work identical to the methods for /user. 
 
GET method: 
/product/23823
if the product with this ID exists, service will return the following JSON in the response body 
{
    "id": 23823,
    "productname": "productname-32843hnksjn4398",
    "price": 3.99 ,
    "quantity": 9
}
*/
public class ProductService {
    static String jdbcUrl = "jdbc:sqlite:/student/wuhungma/Desktop/a1/db/ProductDatabase.db";
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

            // Register the SQLite JDBC driver
            // Class.forName("org.sqlite.JDBC");
            // Open a connection
            // Class.forName("org.sqlite.JDBC");
            Connection connection = DriverManager.getConnection(jdbcUrl);

            //Before starting server, create User database
            String createTableQuery = "CREATE TABLE IF NOT EXISTS Product ("
                        + "productId INTEGER PRIMARY KEY,"
                        + "productName TEXT NOT NULL,"
                        + "description TEXT NOT NULL,"
                        + "price INTEGER NOT NULL,"
                        + "quantity INTEGER NOT NULL)";
            try (Statement statement = connection.createStatement()) {
                // Execute the query to create the User table
                statement.executeUpdate(createTableQuery);
                System.out.println("Product table created successfully.");
            } catch (SQLException sqle) {
                sqle.printStackTrace();
            }
            
            // Close the connection
            connection.close();
            
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
            if ("POST".equals(exchange.getRequestMethod())) {
                {
                    // For debugging purposes
                    // String clientAddress = exchange.getRemoteAddress().getAddress().toString();
                    // String requestMethod = exchange.getRequestMethod();
                    // String requestURI = exchange.getRequestURI().toString();
                    // Map<String, List<String>> requestHeaders = exchange.getRequestHeaders();

                    // System.out.println("Client Address: " + clientAddress);
                    // System.out.println("Request Method: " + requestMethod);
                    // System.out.println("Request URI: " + requestURI);
                    // System.out.println("Request Headers: " + requestHeaders);
                    // Print all request headers
                    // for (Map.Entry<String, List<String>> header : requestHeaders.entrySet()) {
                    //     System.out.println(header.getKey() + ": " + header.getValue().getFirst());
                    // }
                }

                JSONObject requestbody = getRequestBody(exchange);
                
                /* Get values */

                String command = requestbody.getString("command");
                
                if (command.equals("create")) 
                {
                    /*create new product */
                    try {
                        //Get body parameters
                        int id_int = requestbody.getInt("id");
                        String id = String.valueOf(id_int);
                        String productName = requestbody.getString("name");
                        String description = requestbody.getString("description");
                        double price_double = requestbody.getDouble("price");
                        // String price = String.valueOf(price_int);
                        int quantity_int = requestbody.getInt("quantity");
                        // String quantity = String.valueOf(quantity_int);

                        Connection connection = DriverManager.getConnection(jdbcUrl);             
                        String insertQuery = "INSERT INTO Product (productId, productName, description, price, quantity) VALUES (?, ?, ?, ?, ?)";
                        PreparedStatement preparedStatement = connection.prepareStatement(insertQuery);
                        preparedStatement.setInt(1, id_int);
                        preparedStatement.setString(2, productName);
                        preparedStatement.setString(3, description);
                        preparedStatement.setDouble(4, price_double);
                        preparedStatement.setInt(5, quantity_int);
                        int rowsAffected = preparedStatement.executeUpdate();
                        preparedStatement.close();
                        connection.close();

                        JSONObject responseBody = createResponse(exchange, requestbody, id, id_int, productName, description, price_double, quantity_int);
                        
                        //Put in all information that needs to be sent to the client
                        int statusCode = 200;
                        sendResponse(exchange, statusCode, responseBody.toString());
                        
                        if (rowsAffected > 0) {
                            System.out.println("Product information updated successfully.");
                        } 

                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                else if (command.equals("update")) 
                {
                    /*update product */
                    try {
                        //Get body parameters
                        int id_int = requestbody.getInt("id");
                        String id = String.valueOf(id_int);
                        String productName = requestbody.getString("name");
                        String description = requestbody.getString("description");
                        double price_double = requestbody.getDouble("price");
                        // String price = String.valueOf(price_int);
                        int quantity_int = requestbody.getInt("quantity");
                        // String quantity = String.valueOf(quantity_int);

                        Connection connection = DriverManager.getConnection(jdbcUrl);             
                        String updateQuery = "UPDATE Product SET productName = ?, description = ?, price = ?, quantity = ? WHERE productId = ?";
                        PreparedStatement preparedStatement = connection.prepareStatement(updateQuery);
                        preparedStatement.setString(1, productName);
                        preparedStatement.setString(2, description);
                        preparedStatement.setDouble(3, price_double);
                        preparedStatement.setInt(4, quantity_int);
                        preparedStatement.setInt(5, id_int);

                        int rowsAffected = preparedStatement.executeUpdate();
                        preparedStatement.close();
                        connection.close();

                        JSONObject responseBody = createResponse(exchange, requestbody, id, id_int, productName, description, price_double, quantity_int);
                        //Put in all information that needs to be sent to the client
                        int statusCode = 200;
                        sendResponse(exchange, statusCode, responseBody.toString());
                        
                        if (rowsAffected > 0) {
                            System.out.println("Product information updated successfully.");
                        } else {
                            System.out.println("No product found with the specified ID.");
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                } 
                else if (command.equals("delete")) 
                {
                    /* delete product */
                    try 
                    {
                        int id_int = requestbody.getInt("id");
                        String id = String.valueOf(id_int);

                        Connection connection = DriverManager.getConnection(jdbcUrl);
                        String deleteQuery = "DELETE FROM Product WHERE productId = ?";
                        PreparedStatement preparedStatement = connection.prepareStatement(deleteQuery);
                        preparedStatement.setInt(1, id_int);
                        int rowsAffected = preparedStatement.executeUpdate();
                        preparedStatement.close();
                        connection.close();
                
                        if (rowsAffected > 0) {
                            System.out.println("Product deleted successfully.");
                        } else {
                            System.out.println("No product found with the specified ID.");
                        }
                        JSONObject responseBody = createResponse(exchange, requestbody, id, id_int, "", "", 0, 0);
                        //Put in all information that needs to be sent to the client
                        int statusCode = 200;
                        sendResponse(exchange, statusCode, responseBody.toString());
                        
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }

            }
            // Handle Get request 
            else if("GET".equals(exchange.getRequestMethod())){
                JSONObject requestbody = getRequestBody(exchange);
                int id_int = requestbody.getInt("id");
                String id = String.valueOf(id_int);

                //Get user information
                JSONObject responseBody = createResponse(exchange, requestbody, id, id_int, "", "", 0, 0);

                int statusCode = 200;
                sendResponse(exchange, statusCode, responseBody.toString());
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

    private static JSONObject createResponse(HttpExchange exchange, JSONObject requestBody,
        String id, Integer id_int, String productName, String description, double price_double, int quantity_int) {
        if ("GET".equals(exchange.getRequestMethod())) {
            try {
                Connection connection = DriverManager.getConnection(jdbcUrl);
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
                            .put("id", id)
                            .put("name", productName_get)
                            .put("description", description_get)
                            .put("price", price_get)
                            .put("quantity", quantity_get);

                    // Close resources
                    resultSet.close();
                    preparedStatement.close();
                    connection.close();

                    return responseBody;
                } else {
                    // User not found
                    System.out.println("No user found with the specified ID.");
                }

                // Close resources
                resultSet.close();
                preparedStatement.close();
                connection.close();

            } catch (SQLException e) {
                e.printStackTrace();
            }

            return null; // Return null if there's an error or if the user is not found

        } else if ("POST".equals(exchange.getRequestMethod())) {
            if (!requestBody.getString("command").equals("delete")) {
                return new JSONObject()
                        .put("id", id)
                        .put("name", productName)
                        .put("description", description)
                        .put("price", price_double)
                        .put("quantity", quantity_int);
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
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.sendResponseHeaders(statusCode, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes(StandardCharsets.UTF_8));
        os.close();
    }
}
