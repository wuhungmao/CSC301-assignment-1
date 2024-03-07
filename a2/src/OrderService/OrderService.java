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
/user Provide endpoints for user functionality
/product Provide endpoints for product functionality

/order POST API (for A1)
{
    "command": "place order",
    "user_id": 1,
    "product_id": 2,
    "quantity": 3
}

==> if the product and the user exists and has a quantity of >= this quantity in stock, then quantity in DB is reduced
by this quantity and success is returned.

All communication from OrderService MUST go to ISCS which will route and load balance the requests.

Note: this means that to place an order, you may have to first make a GET request, then an "update order" POST request.
 */
public class OrderService {
    public static HttpServer server;
    public static ExecutorService httpThreadPool;
    public static String jdbcUrl = "jdbc:sqlite:UserDatabase.db";
    private static final String ISCS_ENDPOINT = "http://127.0.0.0.1/forward";

    //Shutdown Signals
    public static int workRunning = 0;
    public static String userURL = "";
    public static String productURL = "";
    public static int orderId = 0;

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

            JSONObject userServiceConfig = config.getJSONObject("UserService");
            String userServiceIP = userServiceConfig.getString("ip"); 
            Integer userPort = userServiceConfig.getInt("port"); 

            productURL = "http://" + productServiceIP + ":" + productPort + "/product";
            userURL = "http://" + userServiceIP + ":" + userPort + "/user";

            //System.out.println(productURL);
            //System.out.println(userURL);

            // Extract IP address and port
            String ipAddress = orderServiceConfig.getString("ip");
            int port = orderServiceConfig.getInt("port");

            /*For Http request*/
            HttpServer server = HttpServer.create(new InetSocketAddress(ipAddress, port), 0);

            OrderHandler newHandler = new OrderHandler();
            UserHandler newHandler2 = new UserHandler();
            ProductHandler newHandler3 = new ProductHandler();
            server.setExecutor(Executors.newFixedThreadPool(10)); 
            
            server.createContext("/order", newHandler);
            server.createContext("/user", newHandler2);
            server.createContext("/product", newHandler3);
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
            JSONObject requestBody = getRequestBody(exchange);
            String command = requestBody.getString("command");
            if ("POST".equals((exchange.getRequestMethod()))) {
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
                                throw new SQLException("user does not exist");
                            }
                            int quantity_wanted = requestBody.getInt("quantity");

                            if (quantity_wanted <= 0) {
                                throw new IllegalArgumentException("Invalid quantity: Quantity must be a positive integer");
                            }

                            //Make sure the JSON is of correct format with user id product id and qunatity
                            Connection connection = DriverManager.getConnection("jdbc:sqlite:src/ProductService/ProductDatabase.db");
                            String selectQuery = "SELECT * FROM Product WHERE productId = ?";
                            PreparedStatement preparedStatement = connection.prepareStatement(selectQuery);
                            preparedStatement.setInt(1, productId);

                            ResultSet resultSet = preparedStatement.executeQuery();

                            if (resultSet.next()) {
                                System.out.println("test1");
                                int product_id = resultSet.getInt("productId");
                                System.out.println("test2");
                                System.out.println("test4");
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
                                    //Else, if no sql error and count is bigger or equal, we can process the order

                                    //Create a post request to product server so that it can decrease number of product in database by quantity
                                    System.out.println("product_id is " + product_id);
                                    System.out.println("quantity is " + (quantity_database - quantity_wanted));
 
                                    JSONObject requestToProduct = new JSONObject();
                                    requestToProduct.put("command", "update")
                                                    .put("product_id", product_id)
                                                    .put("quantity", quantity_database - quantity_wanted);
                                    
                                    //need to update product database here, but it somehow fails
                                    forwardRequest("http://127.0.0.1:8080/product/", requestToProduct);

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

                            // System.out.println("User ID: " + userId);
                            // System.out.println("Product ID: " + productId);
                            // System.out.println("Quantity: " + quantity);

                        } else {
                            responseToClient
                                    .put("status", "Invalid Request");
                            sendResponse(exchange, 400, responseToClient.toString());
                        }
                    } catch (SQLException e) {
                        //Either user id or product id cannot be found
                        responseToClient
                            .put("status", "Invalid Request");
                        sendResponse(exchange, 404, responseToClient.toString());
                    } catch(IllegalArgumentException e) {
                        //Either user id or product id cannot be found
                        responseToClient
                            .put("status", "Invalid Request");
                        sendResponse(exchange, 400, responseToClient.toString());
                    } catch (IOException | InterruptedException e) {
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
                        forwardRequest(userURL, requestBody);
                        sendResponse(exchange, 200, "forward");
                    } catch (IOException | InterruptedException e) {
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
                            forwardRequest(productURL, requestBody);
                            sendResponse(exchange, 200, "forward");
                        } catch (IOException | InterruptedException e) {
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

    //POST forward
    public static String forwardRequest(String targetURL, JSONObject jsonData) throws IOException, InterruptedException {
        URL url = URI.create(targetURL).toURL();
        String body = jsonData.toString();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        // Set the request method to GET
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");

        // Get the response code
        try(DataOutputStream dos = new DataOutputStream(connection.getOutputStream())){
            dos.writeBytes(body);
        }
        connection.disconnect();
        return "response";
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
        String jdbcUrl = "jdbc:sqlite:src/ProductService/ProductDatabase.db";
        boolean exists = false;
    
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
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
        String jdbcUrl = "jdbc:sqlite:src/UserService/UserDatabase.db";
        boolean exists = false;
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             PreparedStatement preparedStatement = connection.prepareStatement(
                 "SELECT COUNT(*) FROM User WHERE user_id = ?")) {
            
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