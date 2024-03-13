package OrderService;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.Arrays;
import java.util.List;

import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.sql.Statement;


//import JSONObject
import org.json.JSONObject;

import java.util.ArrayList;

/*
Order Service:
This must be written in Java
This is the service's public facing endpoint. It is used to 
create users and products, but also to handle orders. 

Implement an order microservice responsible for handling customer orders.
An order should have attributes such as id, user_id, product_id, quantity, and order_date.
All endpoints on this service are: 
/order Provide endpoints for order creation, retrieval, and cancellation.
/users Provide endpoints for users functionality
/product Provide endpoints for product functionality

/order POST API (for A1)
{
    "command": "place order",
    "user_id": 1,
    "product_id": 2,
    "quantity": 3
}

==> if the product and the users exists and has a quantity of >= this quantity in stock, then quantity in DB is reduced
by this quantity and success is returned.

All communication from OrderService MUST go to ISCS which will route and load balance the requests.

Note: this means that to place an order, you may have to first make a GET request, then an "update order" POST request.
 */
public class OrderService {
    public static HttpServer server;
    public static ExecutorService httpThreadPool;
    public static String password = "password";
    public static String username = "postgres";
    public static String host = "172.17.0.2";
    public static String port = "5432";
    public static String jdbcUrl = String.format("jdbc:postgresql://%s:%s/users", host, port);
    public static String jdbcP = String.format("jdbc:postgresql://%s:%s/product", host, port);
    public static String jdbcUrl2 = String.format("jdbc:postgresql://%s:%s/orders", host, port);

    private static final String ISCS_ENDPOINT = "http://127.0.0.0.1/forward";

    //Shutdown Signals
    public static int workRunning = 0;
    public static String userURL = "";
    public static String productURL = "";
    public static String ISCSURL = "";
    public static int orderId = 0;
    public static int requestCount = 0;

    public static void main(String[] args) throws IOException {
        
        // Read the JSON configuration file
        String configFile = args[0];
        try{
            String configContent = new String(Files.readAllBytes(Paths.get(configFile)));

            JSONObject config = new JSONObject(configContent);
            JSONObject orderServiceConfig = config.getJSONObject("OrderService");
            
            JSONObject productServiceConfig = config.getJSONObject("ProductService");
            String productServiceIP = productServiceConfig.getString("ip"); 
            Integer productPort = productServiceConfig.getInt("port"); 

            JSONObject ISCSconfig = config.getJSONObject("InterServiceCommunication");
            String ISCSip = ISCSconfig.getString("ip"); 
            Integer ISCSport = ISCSconfig.getInt("port"); 

            JSONObject userServiceConfig = config.getJSONObject("UserService");
            String userServiceIP = userServiceConfig.getString("ip"); 
            Integer userPort = userServiceConfig.getInt("port"); 

            productURL = "http://" + productServiceIP + ":" + productPort + "/product";
            userURL = "http://" + userServiceIP + ":" + userPort + "/user";
            ISCSURL = "http://" + ISCSip + ":" + ISCSport + "/user";


            //System.out.println(productURL);
            //System.out.println(userURL);

            // Extract IP address and port
            String ipAddress = orderServiceConfig.getString("ip");
            int port = orderServiceConfig.getInt("port");

            /*For Http request*/
            HttpServer server = HttpServer.create(new InetSocketAddress(ipAddress, port), 0);

            OrderHandler newHandler = new OrderHandler();
            UserHandler newHandler2 = new UserHandler();
            UserPurchaseHandler newHandler3 = new UserPurchaseHandler();
            ProductHandler newHandler4 = new ProductHandler();
            server.setExecutor(Executors.newFixedThreadPool(10)); 
            
            server.createContext("/order", newHandler);
            server.createContext("/user", newHandler2);
            server.createContext("/user/purchased", newHandler3);
            server.createContext("/product", newHandler4);
            server.start();
            System.out.println("OrderService IP Address: " + ipAddress);
            System.out.println("OrderService Port: " + port);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Need shutdown in new requirements, this is a hook that shutdowns the server

    }

    static class OrderHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            requestCount++;
            JSONObject requestBody = getRequestBody(exchange);
            String command = requestBody.getString("command");
            if ("POST".equals((exchange.getRequestMethod()))) {
                if (requestCount == 1 && !command.equals("restart")) {
                    System.out.println("Creating new database");
                    createNewDatabase();
                }
                if ("place order".equals(command) == true) {
                    JSONObject responseToClient = new JSONObject();
                    try {
                        workRunning++;
                        if (requestBody.has("user_id") && requestBody.has("product_id") && requestBody.has("quantity")) {
                            // Check if productId exists
                            int productId = requestBody.getInt("product_id");
                            if (!doesProductIdExist(productId)) {
                                throw new SQLException("product does not exist");
                            }

                            // Check if userId exists
                            int userId = requestBody.getInt("user_id");
                            if (!doesUserIdExist(userId)) {
                                throw new SQLException("users does not exist");
                            }
                            int quantity_wanted = requestBody.getInt("quantity");

                            if (quantity_wanted <= 0) {
                                throw new IllegalArgumentException("Invalid quantity: Quantity must be a positive integer");
                            }

                            //Make sure the JSON is of correct format with users id product id and qunatity
                            Connection connection = DriverManager.getConnection(jdbcP, username, password);
                            String selectQuery = "SELECT * FROM product WHERE productId = ?";
                            PreparedStatement preparedStatement = connection.prepareStatement(selectQuery);
                            preparedStatement.setInt(1, productId);

                            ResultSet resultSet = preparedStatement.executeQuery();

                            if (resultSet.next()) {
                                System.out.println("test1");
                                int product_id = resultSet.getInt("productId");
                                System.out.println("test2");
                                Integer quantity_database = resultSet.getInt("quantity");
                                System.out.println("test5");
                                orderId += 1;

                                // Return 409 if the amount inside database is less than quantity asked
                                if (quantity_database < quantity_wanted) {
                                    // System.out.println("flag2");
                                    responseToClient
                                            .put("status", "Exceeded quantity limit");
                                    sendResponse(exchange, 400, responseToClient.toString());
                                } else {
                                    System.out.println("before creating product request");

                                    //Create a post request to product server so that it can decrease number of product in database by quantity
                                    System.out.println("product_id is " + product_id);
                                    System.out.println("quantity is " + (quantity_database - quantity_wanted));
 
                                    String jsonBody = String.format("{\"command\": \"update\", \"product_id\": %d, \"quantity\": %d}", product_id, quantity_database - quantity_wanted);
                                    // Update product database
                                    
                                    
                                    Connection connection2 = DriverManager.getConnection(jdbcUrl2, username, password);
                                    System.out.println("here11111");
                                    String insertQuery = "INSERT INTO orders (orderId, userId, productId, quantity) VALUES (?, ?, ?, ?)";
                                    try (PreparedStatement preparedStatementInsert = connection2.prepareStatement(insertQuery)) {
                                        preparedStatementInsert.setInt(2, userId);
                                        preparedStatementInsert.setInt(3, productId);
                                        preparedStatementInsert.setInt(4, quantity_wanted);
                                        preparedStatementInsert.executeUpdate();
                                    } catch(SQLException e){
                                        String response = sendPostRequest(ISCSURL + "/product", jsonBody);
                                        if (quantity_database < quantity_wanted) {
                                            // System.out.println("flag2");
                                            responseToClient
                                                    .put("status", "Exceeded quantity limit");
                                            sendResponse(exchange, 400, responseToClient.toString());
                                        }
                                        System.out.println("ERROR");
                                    }
                                    try {
                                        String response = sendPostRequest(ISCSURL + "/product", jsonBody);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }

                                    System.out.println("after creating product request");
                                    responseToClient
                                            // .put("id", orderId)
                                            .put("product_id", product_id)
                                            .put("user_id", userId)
                                            .put("quantity", quantity_wanted)
                                            .put("status", "Success");
                                    sendResponse(exchange, 200, responseToClient.toString());
                                }
                            }
                            //Close
                            resultSet.close();
                            preparedStatement.close();
                            connection.close();

                            // System.out.println("users ID: " + userId);
                            // System.out.println("Product ID: " + productId);
                            // System.out.println("Quantity: " + quantity);

                        } else {
                            responseToClient
                                    .put("status", "Invalid Request");
                            sendResponse(exchange, 400, responseToClient.toString());
                        }
                    } catch (SQLException e) {
                        //Either users id or product id cannot be found
                        responseToClient
                            .put("status", "Invalid Request");
                        sendResponse(exchange, 404, responseToClient.toString());
                    } catch(IllegalArgumentException e) {
                        //Either users id or product id cannot be found
                        responseToClient
                            .put("status", "Invalid Request");
                        sendResponse(exchange, 400, responseToClient.toString());
                    } catch (IOException e) {
                        responseToClient.put("status", "Invalid Request");
                        sendResponse(exchange, 400, responseToClient.toString());
                    }finally {
                        workRunning--;
                    }
                } else {
                    JSONObject responseToClient = new JSONObject();
                    responseToClient
                    .put("status", "Invalid Request");
                    sendResponse(exchange, 400, responseToClient.toString());
                }
            } else if (command.equals("shutdown")) {
                //Additional requirements
                JSONObject responseBody = new JSONObject();
                responseBody.put("command", command);
                sendResponse(exchange, 200, responseBody.toString());

                System.out.println("Product Server has been shut down gracefully.");
                shutdownServer();
            }
        }
    }

    static class UserHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals((exchange.getRequestMethod()))) {
                JSONObject requestBody = getRequestBody(exchange);
                String command = requestBody.getString("command");
                if ("create".equals(command) || "update".equals(command) || "delete".equals(command)) {
                    try {
                        String jsonAsString = requestBody.toString();
                        String response = sendPostRequest(ISCSURL + "/user", jsonAsString);
                        sendResponse(exchange, 200, "forward");
                    } catch (IOException e) {
                        JSONObject responseToClient = new JSONObject();
                        responseToClient.put("status", "Invalid Request");
                        sendResponse(exchange, 400, responseToClient.toString());
                    }
                }else{
                    JSONObject responseToClient = new JSONObject();
                    responseToClient.put("status", "Invalid Request");
                    sendResponse(exchange, 400, responseToClient.toString());
                }
            } else {
                try {
                    String[] pathSegments = exchange.getRequestURI().getPath().split("/");
                    Integer id_int = Integer.parseInt(pathSegments[pathSegments.length - 1]);
                    int code = forwardGetRequest(userURL + "/" + id_int.toString());
                    
                    JSONObject responseToClient = new JSONObject();
                    responseToClient.put("status", "Invalid Request");

                    sendResponse(exchange, code, "Success");
                } catch (IOException | InterruptedException e) {
                    JSONObject responseToClient = new JSONObject();
                    responseToClient.put("status", "Invalid Request");
                    sendResponse(exchange, 400, responseToClient.toString());
                }
            }
        }
    }

    static class ProductHandler implements HttpHandler {
        @Override
            public void handle(HttpExchange exchange) throws IOException {
                if ("POST".equals((exchange.getRequestMethod()))) {
                    JSONObject requestBody = getRequestBody(exchange);
                    String command = requestBody.getString("command");
                    if ("create".equals(command) || "update".equals(command) || "delete".equals(command)) {
                        try {
                            String jsonAsString = requestBody.toString();
                            String response = sendPostRequest(ISCSURL + "/product", jsonAsString);
                            sendResponse(exchange, 200, "forward");
                        } catch (IOException e) {
                            JSONObject responseToClient = new JSONObject();
                            responseToClient.put("status", "Invalid Request");
                            sendResponse(exchange, 400, responseToClient.toString());
                        }
                    }else{
                        JSONObject responseToClient = new JSONObject();
                        responseToClient.put("status", "Invalid Request");
                        sendResponse(exchange, 400, responseToClient.toString());
                    }
                } else {
                    try {
                        String[] pathSegments = exchange.getRequestURI().getPath().split("/");
                        Integer id_int = Integer.parseInt(pathSegments[pathSegments.length - 1]);
                        int code = forwardGetRequest(userURL + "/" + id_int.toString());
                        
                        JSONObject responseToClient = new JSONObject();
                        responseToClient.put("status", "Invalid Request");

                        sendResponse(exchange, code, "Success");
                    } catch (IOException | InterruptedException e) {
                        JSONObject responseToClient = new JSONObject();
                        responseToClient.put("status", "Invalid Request");
                        sendResponse(exchange, 400, responseToClient.toString());
                    }
                }
            }
        }

        static class UserPurchaseHandler implements HttpHandler {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String path = exchange.getRequestURI().getPath();
                String[] parts = path.split("/");

                if (parts.length > 2) {
                    int userId = Integer.parseInt(parts[3]);  
                    JSONObject responseJson = new JSONObject();

                    try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
                        String checkUserQuery = "SELECT COUNT(*) AS count FROM users WHERE user_id = ?";
                        try (PreparedStatement stmt = connection.prepareStatement(checkUserQuery)) {
                            stmt.setInt(1, userId); 
                            ResultSet rs = stmt.executeQuery();

                            if (rs.next() && rs.getInt("count") > 0) {
                                try (Connection connection2 = DriverManager.getConnection(jdbcUrl2, username, password)) {
                                    String selectQuery = "SELECT productId, SUM(quantity) as quantity FROM orders WHERE userId = ? GROUP BY productId";
                                    try (PreparedStatement stmt2 = connection2.prepareStatement(selectQuery)) {
                                        stmt2.setInt(1, userId);
                                        ResultSet rs2 = stmt2.executeQuery();

                                        JSONObject purchases = new JSONObject();
                                        while (rs2.next()) {
                                            int productId = rs2.getInt("productId");
                                            int quantity = rs2.getInt("quantity");
                                            purchases.put(String.valueOf(productId), quantity);
                                        }

                                        byte[] responseBytes;
                                        if (purchases.isEmpty()) {
                                             responseBytes = "{}".getBytes();
                                            exchange.sendResponseHeaders(200, responseBytes.length);
                                        } else {
                                            String jsonResponse = purchases.toString();
                                            responseBytes = jsonResponse.getBytes();
                                            exchange.sendResponseHeaders(200, responseBytes.length);
                                        }
                                        
                                        OutputStream os = exchange.getResponseBody();
                                        os.write(responseBytes); 
                                        os.close();
                                    }
                                } catch (SQLException e) {
                                    e.printStackTrace(); 
                                }
                            } else {
                                String response = "";
                                exchange.sendResponseHeaders(404, response.getBytes().length);
                                OutputStream os = exchange.getResponseBody();
                                os.write(response.getBytes());
                                os.close();
                            }
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                        String response = "Invalid Request";
                        exchange.sendResponseHeaders(500, response.getBytes().length);
                        OutputStream os = exchange.getResponseBody();
                        os.write(response.getBytes());
                        os.close();
                    }
                } else {
                    String response = "Invalid request";
                    exchange.sendResponseHeaders(400, response.getBytes().length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                }
            }
        }

    private static void createNewDatabase() {
        File databaseFile = new File("db/OrderDatabase.db");
        
            // Check if the database file exists
            if (databaseFile.exists()) {
                // Delete the existing database file
                if (databaseFile.delete()) {
                    System.out.println("Existing database deleted successfully.");
                } else {
                    System.out.println("Failed to delete the existing database.");
                }
            }

        try (Connection connection = DriverManager.getConnection(jdbcUrl2)) {
            // Create Purchases table
            String createPurchasesTableQuery = "CREATE TABLE IF NOT EXISTS orders ("
                    + "orderId INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "userId INTEGER NOT NULL,"
                    + "productId INTEGER NOT NULL,"
                    + "quantity INTEGER NOT NULL)";
            try (Statement statement = connection.createStatement()) {
                    // Execute the query to create the Product table
                    statement.executeUpdate(createPurchasesTableQuery);
                    System.out.println("Product table created successfully in a new database.");
            } catch (SQLException sqle) {
                System.out.println("Error creating Product table: " + sqle.getMessage());
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

    //If order is place go inside of this body.

    public static boolean isRunning(){
        return workRunning > 0;
    }

    //Get the json from request
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

    //Shutdown 
    private static void shutdownServer() {
        // Stop accepting new requests
        int timeout = 10;
        while (isRunning() && timeout > 0) {
            try{
                Thread.sleep(1000);
            }catch(InterruptedException e){
                System.out.println("wait");
            }
            timeout--;
        }
        server.removeContext("/");
        server.stop(0);
        httpThreadPool.shutdown();
        try {
            if (!httpThreadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                httpThreadPool.shutdownNow();
            }
        } catch (InterruptedException ex) {
            httpThreadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }

        System.out.println("Server shut down.");
        System.exit(0); // Exit the application
    }

    // Send POST Request to urlString with jsonInputString, return responses as string
    public static String sendPostRequest(String urlString, String jsonInputString) throws IOException {
        URL url = URI.create(urlString).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        // Send the request body
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        // Read the response
        int responseCode = connection.getResponseCode();
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                (responseCode < HttpURLConnection.HTTP_BAD_REQUEST) ? connection.getInputStream() : connection.getErrorStream(),
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        } finally {
            connection.disconnect();
        }
        return response.toString();
    }

    //GET forward
    public static int forwardGetRequest(String targetURL) throws IOException, InterruptedException {
        URL url = URI.create(targetURL).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        // Set the request method to GET
        connection.setRequestMethod("GET");

        // Get the response code
        int responseCode = connection.getResponseCode();

        connection.disconnect();
        return responseCode;
    }


    private static boolean doesProductIdExist(int productId) throws SQLException {
        String password = "password";
        String username = "postgres";
        String host = "172.17.0.2";
        String port = "5432";
        String url = String.format("jdbc:postgresql://%s:%s/product", host, port);
        boolean exists = false;
    
        try (Connection connection = DriverManager.getConnection(url, username, password);
             PreparedStatement preparedStatement = connection.prepareStatement(
                 "SELECT COUNT(*) FROM Product WHERE productId = ?")) {
            
            preparedStatement.setInt(1, productId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    exists = resultSet.getInt(1) > 0;
                }
            }
        }
        return exists;
    }

    private static boolean doesUserIdExist(int userId) throws SQLException {
        String password = "password";
        String username = "postgres";
        String host = "172.17.0.2";
        String port = "5432";
        String url = String.format("jdbc:postgresql://%s:%s/users", host, port);

        boolean exists = false;
        try (Connection connection = DriverManager.getConnection(url, username, password);
             PreparedStatement preparedStatement = connection.prepareStatement(
                 "SELECT COUNT(*) FROM users WHERE user_id = ?")) {
            
            preparedStatement.setInt(1, userId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    exists = resultSet.getInt(1) > 0;
                }
            }
        }
        return exists;
    }

}
