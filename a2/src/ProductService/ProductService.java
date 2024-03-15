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
import java.util.concurrent.ConcurrentHashMap;

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

public class ProductService {
    public static Map<String, JSONObject> cache = new ConcurrentHashMap<>();//cache
    public static String password = "password";
    public static String username = "postgres";
    public static String host = "172.17.0.2";
    public static String port = "5432";
    //public static String url = "jdbc:postgresql://172.17.0.2:5432/product";
    //DELETE THIS AFTER
    public static String url = "jdbc:postgresql://127.0.0.1:5432/product";

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

            System.out.println("product Server started on port " + port);
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
            System.out.println("product service catch the request");
            if ("POST".equals(exchange.getRequestMethod())) {
                JSONObject requestbody = getRequestBody(exchange);
                String command = requestbody.getString("command");
                if (requestCount == 1 && !command.equals("restart")) {
                    System.out.println("Creating new database");
                    createNewDatabase();
                }
                if (command.equals("create")) {
                    // Check if this is the first request
                    try  (Connection connection = DriverManager.getConnection(url, username, password)){
                        // Get body parameters
                        if(requestbody.has("id") && requestbody.has("name") && requestbody.has("description") && requestbody.has("price") && requestbody.has("quantity")) {
                            int id_int = requestbody.getInt("id");
                            String id = String.valueOf(id_int);
                            String productName = requestbody.getString("name");
                            String description = requestbody.getString("description");
                            double price_double = requestbody.getDouble("price");
                            Object quantity = requestbody.get("quantity");
                            if (!(quantity instanceof Integer) ) {
                                throw new NumberFormatException("quantity is not int");
                            }
                            int quantity_int = requestbody.getInt("quantity");
                            
                            // Check for negative quantity or price
                            if (!(quantity_int <= 0) && !(price_double < 0) && !(productName.isEmpty()) && !(description.isEmpty()) && !(id.isEmpty())) {
                                // Check for duplicate ID
                                String checkDuplicateQuery = "SELECT * FROM product WHERE productId = ?";
                                PreparedStatement checkDuplicateStatement = connection.prepareStatement(checkDuplicateQuery);
                                checkDuplicateStatement.setInt(1, id_int);
                                ResultSet duplicateResultSet = checkDuplicateStatement.executeQuery();
                                
                                if (duplicateResultSet.next()) {
                                    // Duplicate ID found
                                    throw new SQLException("duplicate id found");
                                }
                                
                                // Close resources for duplicate check
                                duplicateResultSet.close();
                                checkDuplicateStatement.close();

                                //cache
                                JSONObject info = new JSONObject();
                                info.put("name", productName);
                                info.put("description", description);
                                info.put("price", price_double);
                                info.put("quantity", quantity);
                                cache.put(id, info);
                                System.out.println("update");

                                // Continue with insertion
                                String insertQuery = "INSERT INTO product (productId, productName, description, price, quantity) VALUES (?, ?, ?, ?, ?)";
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
                                    System.out.println("product information created successfully.");
                                }
                            } else {
                                //Either field is empty or some values are not valid
                                JSONObject responseBody = new JSONObject();
                                int statusCode = 400; // Bad Request
                                sendResponse(exchange, statusCode, responseBody.toString());
                            } 
                        } else {
                            //Missing some fields
                            JSONObject responseBody = new JSONObject();
                            // Put in all information that needs to be sent to the client
                            int statusCode = 400; 
                            sendResponse(exchange, statusCode, responseBody.toString());
                        }
                    } catch (SQLException e) {
                        System.out.println("You screw up at post create");
                        JSONObject responseBody = new JSONObject();
                        int statusCode = 400;
                        sendResponse(exchange, statusCode, responseBody.toString());
                    } catch (JSONException e) {
                        System.out.println("Status code 400 in post create");
                        JSONObject responseBody = new JSONObject();
                        int statusCode = 400;
                        sendResponse(exchange, statusCode, responseBody.toString());
                    } catch (NumberFormatException e) {
                        JSONObject responseBody = new JSONObject();
                        int statusCode = 400;
                        sendResponse(exchange, statusCode, responseBody.toString());
                    }
                } else if (command.equals("update")) {
                    /* update product */
                    System.out.println("got the request in update");
                    try (Connection connection = DriverManager.getConnection(url, username, password)) {
                        if(requestbody.has("id")) {
                            // Get body parameters
                            int id_int = requestbody.getInt("id");
                            String id = String.valueOf(id_int);//for cache
                            
                            // Build the dynamic part of the UPDATE query based on provided attributes
                            StringBuilder updateQueryBuilder = new StringBuilder("UPDATE product SET ");
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

                            JSONObject original_info = cache.get(id);
                            // Set values for each attribute
                            int parameterIndex = 1;
                            if (requestbody.has("name")) {
                                String name = requestbody.getString("name");
                                if(!name.isEmpty()){
                                    preparedStatement.setString(parameterIndex++, name);
                                    if (original_info != null) {
                                        original_info.put("name", requestbody.getString("name"));
                                    }
                                } else {
                                    throw new IllegalArgumentException("name cannot be empty.");
                                }
                            }

                            if (requestbody.has("description")) {
                                String description = requestbody.getString("description");
                                if(!description.isEmpty()){
                                    preparedStatement.setString(parameterIndex++, description);
                                    if (original_info != null) {
                                        original_info.put("description", requestbody.getString("description"));
                                    }
                                } else {
                                    throw new IllegalArgumentException("description cannot be empty.");
                                }
                            }

                            if (requestbody.has("price")) {
                                Double price = requestbody.getDouble("price");
                                if(price > 0){
                                    preparedStatement.setDouble(parameterIndex++, price);
                                    if (original_info != null) {
                                        original_info.put("price", requestbody.getString("price"));
                                    }
                                } else {
                                    throw new IllegalArgumentException("price cannot be 0.");
                                }
                            }

                            if (requestbody.has("quantity")) {
                                int quantity = requestbody.getInt("quantity");
                                if(quantity >= 0){
                                    preparedStatement.setDouble(parameterIndex++, quantity);
                                    if (original_info != null) {
                                        original_info.put("quantity", requestbody.getString("quantity"));
                                    }
                                } else {
                                    throw new IllegalArgumentException("quantity cannot be 0.");
                                }
                            }
                            
                            // Set the productId for the WHERE clause
                            preparedStatement.setInt(parameterIndex, id_int);
                            
                            int rowsAffected = preparedStatement.executeUpdate();
                            preparedStatement.close();
                            
                            /* Create response JSON */
                            
                            // Put in all information that needs to be sent to the client
                            JSONObject responseBody = createResponse(exchange, command, id_int);
                            int statusCode = (rowsAffected > 0) ? 200 : 404; // 404 if no product fogitund
                            sendResponse(exchange, statusCode, responseBody.toString());
                            
                            if (rowsAffected > 0) {
                                cache.put(id ,original_info);
                                System.out.println("product information updated successfully.");
                            } 
                        } else {
                            // if(requestbody.has("id")) {
                            //     // Get body parameters
                            //     int id_int = requestbody.getInt("id");
                            //     System.out.println("id is " + id_int);
                            // }
                            // System.out.println("missing id field exception");

                            //Missing id field
                            System.out.println("missing updated successfully.");
                            JSONObject responseBody = new JSONObject();
                            // Put in all information that needs to be sent to the client
                            int statusCode = 400; 
                            sendResponse(exchange, statusCode, responseBody.toString());
                        }

                    } catch (SQLException e) {
                        // if(requestbody.has("id")) {
                            // Get body parameters
                            // int id_int = requestbody.getInt("id");
                            // System.out.println("id is " + id_int);
                        // }
                        // System.out.println("sql exception");
                        System.out.println("missing updated successfully.");
                        JSONObject responseBody = new JSONObject();
                        int statusCode = 400;
                        sendResponse(exchange, statusCode, responseBody.toString());

                    } catch (IllegalArgumentException e) {
                        // if(requestbody.has("id")) {
                            // Get body parameters
                            // int id_int = requestbody.getInt("id");
                            // System.out.println("id is " + id_int);
                        // }
                        // System.out.println("illegal argument exception");
                        // System.out.println(e.getMessage());
                        JSONObject responseBody = new JSONObject();
                        int statusCode = 400;
                        sendResponse(exchange, statusCode, responseBody.toString());
                    }
                } else if (command.equals("delete")) {
                    try (Connection connection = DriverManager.getConnection(url, username, password)) {
                        if(!requestbody.has("id")) {
                            // int id_int = requestbody.getInt("id");
                            Object id = requestbody.get("id");
                            if(!(id instanceof Integer) || (int) id <= 0){
                                throw new IllegalArgumentException("no id field");
                            }
                        }

                        int id_int = requestbody.getInt("id");
                        String id = String.valueOf(id_int);//for cache
                        String productName = requestbody.getString("name");
                        // String description = requestbody.getString("description");
                        double price = requestbody.getDouble("price");
                        int quantity = requestbody.getInt("quantity");
                
                        String selectQuery = "SELECT * FROM product WHERE productId = ? AND productName = ? AND price = ? AND quantity = ?";
                        PreparedStatement selectStatement = connection.prepareStatement(selectQuery);
                        selectStatement.setInt(1, id_int);
                        selectStatement.setString(2, productName);
                        // selectStatement.setString(3, description);
                        selectStatement.setDouble(3, price);
                        selectStatement.setInt(4, quantity);
                
                        ResultSet resultSet = selectStatement.executeQuery();
                
                        if (resultSet.next()) {
                            // Valid credentials, proceed with deletion
                            resultSet.close();
                            selectStatement.close();
                
                            String deleteQuery = "DELETE FROM product WHERE productId = ?";
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
                                cache.remove(id);//delete from cache
                            }
                            // if (rowsAffected > 0) {
                            //     System.out.println("product deleted successfully.");
                            // } else {
                            //     System.out.println("No product found with the specified ID.");
                            // }
                        } else {
                            // Invalid credentials, send 401 Unauthorized
                            resultSet.close();
                            selectStatement.close();
                            JSONObject responseBody = new JSONObject();
        
                            int statusCode = 401;
                            sendResponse(exchange, statusCode, responseBody.toString());
                        }
                    } catch (SQLException e) {
                        JSONObject responseBody = new JSONObject();
                        int statusCode = 400;
                        sendResponse(exchange, statusCode, responseBody.toString());
                        // e.printStackTrace();
                        if(!requestbody.has("id")) {
                            // int id_int = requestbody.getInt("id");
                            Object id = requestbody.get("id");
                            if(!(id instanceof Integer) || (int) id <= 0){
                                System.out.println("id is " + id);
                                System.out.println("sql exception");
                            }
                        }
                    } catch (JSONException e) {
                        JSONObject responseBody = new JSONObject();
                        int statusCode = 400;
                        sendResponse(exchange, statusCode, responseBody.toString());
                        if(!requestbody.has("id")) {
                            // int id_int = requestbody.getInt("id");
                            Object id = requestbody.get("id");
                            if(!(id instanceof Integer) || (int) id <= 0){
                                System.out.println("id is " + id);
                                System.out.println("json exception");
                            }
                        }
                    } catch (IllegalArgumentException e) {
                        JSONObject responseBody = new JSONObject();
                        int statusCode = 400;
                        sendResponse(exchange, statusCode, responseBody.toString());
                        if(!requestbody.has("id")) {
                            // int id_int = requestbody.getInt("id");
                            Object id = requestbody.get("id");
                            if(!(id instanceof Integer) || (int) id <= 0){
                                System.out.println("id is " + id);
                                System.out.println("illegal argument exception");
                            }
                        }
                    }
                } else if (command.equals("shutdown")) {
                    //Additional requirements
                    JSONObject responseBody = new JSONObject();
                    responseBody.put("command", command);
                    sendResponse(exchange, 200, responseBody.toString());

                    System.out.println("product Server has been shut down gracefully.");
                    System.exit(0); // Exit the application
                } else if (command.equals("restart")) {
                    JSONObject responseBody = new JSONObject();
                    responseBody.put("command", command);
                    sendResponse(exchange, 200, responseBody.toString());
                    System.out.println("product Server has been restarted.");
                }
            }
            // Handle Get request 
            else if("GET".equals(exchange.getRequestMethod())){
                if (requestCount == 1) {
                    System.out.println("Creating new database");
                    createNewDatabase();
                }
                try (Connection connection = DriverManager.getConnection(url, username, password)){
                    String[] pathSegments = exchange.getRequestURI().getPath().split("/");

                    int id_int = Integer.parseInt(pathSegments[pathSegments.length - 1]);
                    String id = String.valueOf(id_int);//for cache

                    String selectQuery = "SELECT * FROM product WHERE productId = ?";
                    PreparedStatement selectStatement = connection.prepareStatement(selectQuery);
                    selectStatement.setInt(1, id_int);
            
                    ResultSet resultSet = selectStatement.executeQuery();
                    if (cache.containsKey(id)) {//get info directly from cache
                        JSONObject responseBody = cache.get(id);
                        int statusCode = 200;
                        sendResponse(exchange, statusCode, responseBody.toString());

            
//                    if (resultSet.next()) {
//                        // Get product information
//                        JSONObject responseBody = createResponse(exchange, "", id_int);
//
//                        int statusCode = 200;
//                        sendResponse(exchange, statusCode, responseBody.toString());
                    } else {
                        //no product with product id given
                        JSONObject responseBody = new JSONObject();
                        int statusCode = 404;
                        sendResponse(exchange, statusCode, responseBody.toString());
                    }
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                    System.out.println("status code 400 at get");
                    // Handle invalid or missing product ID in the URL
                    JSONObject responseBody = new JSONObject();
                    int statusCode = 400;
                    sendResponse(exchange, statusCode, responseBody.toString());
                } catch (SQLException e) {
                    JSONObject responseBody = new JSONObject();
                    int statusCode = 400;
                    sendResponse(exchange, statusCode, responseBody.toString());
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
                try (Connection connection = DriverManager.getConnection(url, username, password)) {    
                    String selectQuery = "SELECT * FROM product WHERE productId = ?";
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
                    try (Connection connection = DriverManager.getConnection(url, username, password)) {
                        String selectQuery = "SELECT * FROM product WHERE productId = ?";
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
                            // product not found
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
        
            // Check if the database file exists
            if (databaseFile.exists()) {
                // Delete the existing database file
                if (databaseFile.delete()) {
                    System.out.println("Existing database deleted successfully.");
                } else {
                    System.out.println("Failed to delete the existing database.");
                }
            }
            try (Connection connection = DriverManager.getConnection(url, username, password)) {
                // Create the product table in the new database
                String createTableQuery = "CREATE TABLE IF NOT EXISTS product ("
                        + "productId INTEGER PRIMARY KEY,"
                        + "productName TEXT NOT NULL,"
                        + "description TEXT NOT NULL,"
                        + "price DOUBLE NOT NULL,"
                        + "quantity INTEGER NOT NULL)";
                try (Statement statement = connection.createStatement()) {
                    // Execute the query to create the product table
                    statement.executeUpdate(createTableQuery);
                    System.out.println("product table created successfully in a new database.");
                } catch (SQLException sqle) {
                    System.out.println("Error creating product table: " + sqle.getMessage());
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