import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.Arrays;
import java.util.List;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
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

    public static void main(String[] args) throws IOException {
        try{            
            String configFilePath = "/a1/config.json";
            JSONObject orderServiceConfig = getIPAddressFromConfig(configFilePath);

            // Extract IP address and port
            String ipAddress = orderServiceConfig.getString("ip");
            int port = orderServiceConfig.getInt("port");

            System.out.println("OrderService IP Address: " + ipAddress);
            System.out.println("OrderService Port: " + port);
        }catch(IOException e) {
            e.printStackTrace();
        }
        //Create order hadnler
        OrderHandler newHandler = new OrderHandler();
        //HttpServer server = HttpServer.create(new InetSocketAddress(ipAddress, port), 0);
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.setExecutor(Executors.newFixedThreadPool(20));
        
        server.createContext("/order", newHandler);
        server.start();

        //Need shutdown in new requirements, this is a hook that shutdowns the server
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down server...");
            shutdownServer();
        }));
    }


    static class OrderHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if("POST".equals((exchange.getRequestMethod()))) {
                JSONObject requestBody = getRequestBody(exchange);
                
                System.out.println("request: " + requestBody);
                processOrder(requestBody, exchange);
                
                String response = "Order processed";
                exchange.sendResponseHeaders(200, requestBody.toString().getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } else {
                exchange.sendResponseHeaders(400, 0);
                exchange.close();
            }
        }
    }

    //If order is place go inside of this body. 
    public static void processOrder(JSONObject requestBody, HttpExchange exchange) throws IOException {
        System.out.println("json sent: " + requestBody.getString("command"));
        try{
            workRunning++;
        if(requestBody.getString("command") != null){
            String command = requestBody.getString("command");
            //Check if order is placed or return null
            if ("place".equals(command)) {
                //Make sure the JSON is of correct format with user id product id and qunatity
                if (requestBody.getString("user_id") != null && requestBody.getString("product_id") != null && requestBody.getString("quantity") != null) {
                    String userId = requestBody.getString("user_id");
                    int productId = requestBody.getInt("product_id");
                    int quantity = requestBody.getInt("quantity");
                    
                    try {
                            Connection connection = DriverManager.getConnection("jdbc:sqlite:ProductDatabase.db");
                            String selectQuery = "SELECT * FROM Product WHERE productId = ?";
                            PreparedStatement preparedStatement = connection.prepareStatement(selectQuery);
                            preparedStatement.setInt(1, productId);
            
                            ResultSet resultSet = preparedStatement.executeQuery();

                            if (resultSet.next()) {
                                String product_id = resultSet.getString("product_id");
                                String user_id = resultSet.getString("user_id");
                                Integer id = resultSet.getInt("id");
                                Integer count = resultSet.getInt("quantity");
                
                                // Return 409 if the amount inside database is less than quantity asked
                                if(count < quantity){
                                    JSONObject responseBody = new JSONObject()
                                    .put("id", id)
                                    .put("product_id", product_id)
                                    .put("user_id", user_id)
                                    .put("quantity", count)
                                    .put("status", "Exceeded quantity limit");
                                }else{
                                    //Else, if no sql error and count is bigger or equal, we can process the order
                                    JSONObject responseBody = new JSONObject()
                                        .put("id", id)
                                        .put("product_id", product_id)
                                        .put("user_id", user_id)
                                        .put("quantity", count)
                                        .put("status", "Success");
                                }
                                
                            }
                            //Close
                            resultSet.close();
                            preparedStatement.close();
                            connection.close();

                            System.out.println("User ID: " + userId);
                            System.out.println("Product ID: " + productId);
                            System.out.println("Quantity: " + quantity);
                    }catch (SQLException e) {
                        //Return 404, since sqlException either user_id or product_id not found
                        JSONObject responseBody = new JSONObject()
                        .put("status", "Invalid Reques");
                    }
                }
            }
        }
    }finally{
        workRunning--;
    }

        String response = "Order processed";
        exchange.sendResponseHeaders(200, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    public static boolean isRunning(){
        return workRunning > 0;
    }

    private void processProduct(JSONObject requestBody, HttpExchange exchange) throws IOException {
        HttpURLConnection connection = null;
        /* 
        try {
            URL url = new URL(targetURL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Read the response from the server
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                System.out.println("Response from User Service: " + response);
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        */
    }

    public static void processUser(String targetURL, String jsonInputString) throws IOException {
        HttpURLConnection connection = null;

        try {
            URL url = new URL(targetURL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Read the response from the server
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                System.out.println("Response from User Service: " + response);
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
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

    public static String createUserCommand(Map<String, String> userData) {
        return new JSONObject()
                .put("command", "create")
                .put("username", userData.get("username"))
                .put("email", userData.get("email"))
                .put("password", userData.get("password"))
                .toString();
    }

    private static JSONObject getIPAddressFromConfig(String configFilePath) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(configFilePath)), "UTF-8");
        JSONObject config = new JSONObject(content);
        System.out.println(config);
        return config;
    }

    public static List<String> parseWords(String input) {
        //Split the wordload by space
        String[] words = input.split(" ");
        return Arrays.asList(words);
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
    }



}