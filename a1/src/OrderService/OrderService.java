package OrderService;

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

    public static void main(String[] args) throws IOException {
        
        // Read the JSON configuration file
        String configFile = args[0];
        try{
            String configContent = new String(Files.readAllBytes(Paths.get(configFile)));

            JSONObject config = new JSONObject(configContent);
            JSONObject orderServiceConfig = config.getJSONObject("OrderService");
            
            JSONObject productServiceConfig = config.getJSONObject("ProductService");
            String productServiceIP = productServiceConfig.getString("ip"); 
            String productPort = productServiceConfig.getString("port"); 

            JSONObject userServiceConfig = config.getJSONObject("UserService");
            String userServiceIP = userServiceConfig.getString("ip"); 
            String userPort = userServiceConfig.getString("port"); 

            productURL = "http://" + productServiceIP + ":" + productPort;
            userURL = "http://" + userServiceIP + ":" + userPort;

            // Extract IP address and port
            String ipAddress = orderServiceConfig.getString("ip");
            int port = orderServiceConfig.getInt("port");

            /*For Http request*/
            HttpServer userServer = HttpServer.create(new InetSocketAddress(ipAddress, port), 0);

            
            OrderHandler newHandler = new OrderHandler();
            //HttpServer server = HttpServer.create(new InetSocketAddress(ipAddress, port), 0);
            server.setExecutor(Executors.newFixedThreadPool(20));
            
            server.createContext("/order", newHandler);
            server.start();
            System.out.println("OrderService IP Address: " + ipAddress);
            System.out.println("OrderService Port: " + port);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Need shutdown in new requirements, this is a hook that shutdowns the server
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down server...");
            shutdownServer();
        }));
    }


    static class OrderHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            JSONObject requestBody = getRequestBody(exchange);
            String command = requestBody.getString("command");
            if("POST".equals((exchange.getRequestMethod()))) {
                if("place".equals(command) == true){
                    processOrder(exchange);
                }else if("create".equals(command) || "update".equals(command) || "delete".equals(command)){
                    try {
                        JSONObject forwardRequest = getRequestBody(exchange);
                        forwardRequest(userURL, forwardRequest);
                    }catch(IOException | InterruptedException | URISyntaxException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                try {
                    forwardGetRequest(userURL);
                }catch(IOException | InterruptedException | URISyntaxException e) {
                    e.printStackTrace();
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
    public static void processOrder(HttpExchange exchange) throws IOException {
        JSONObject responseToClient = new JSONObject();
        JSONObject requestBody = getRequestBody(exchange);
        String command = requestBody.getString("command");

        try {
            workRunning++;
            if ("place".equals(command) == true) {
                if(requestBody.has("user_id") && requestBody.has("product_id") && requestBody.has("quantity")) {
                    int userId = requestBody.getInt("user_id");
                    int productId = requestBody.getInt("product_id");
                    int quantity = requestBody.getInt("quantity");

                    //Make sure the JSON is of correct format with user id product id and qunatity

                    Connection connection = DriverManager.getConnection("jdbc:sqlite:ProductDatabase.db");
                    String selectQuery = "SELECT * FROM Product WHERE productId = ?";
                    PreparedStatement preparedStatement = connection.prepareStatement(selectQuery);
                    preparedStatement.setInt(1, productId);

                    ResultSet resultSet = preparedStatement.executeQuery();

                    if (resultSet.next()) {
                        System.out.println("flag1");
                        String product_id = resultSet.getString("product_id");
                        String user_id = resultSet.getString("user_id");
                        Integer id = resultSet.getInt("id");
                        Integer count = resultSet.getInt("quantity");

                        // Return 409 if the amount inside database is less than quantity asked
                        if (count < quantity) {
                            System.out.println("flag2");
                            responseToClient
                                    .put("id", id)
                                    .put("product_id", product_id)
                                    .put("user_id", user_id)
                                    .put("quantity", count)
                                    .put("status", "Exceeded quantity limit");
                        } else {
                            System.out.println("flag3");
                            //Else, if no sql error and count is bigger or equal, we can process the order
                            responseToClient
                                    .put("id", id)
                                    .put("product_id", product_id)
                                    .put("user_id", user_id)
                                    .put("quantity", count)
                                    .put("status", "Success");
                        }
                    }
                    System.out.println("flag4");
                    //Close
                    resultSet.close();
                    preparedStatement.close();
                    connection.close();

                    System.out.println("User ID: " + userId);
                    System.out.println("Product ID: " + productId);
                    System.out.println("Quantity: " + quantity);

                }else{
                    responseToClient
                            .put("status", "Invalid Request");

                    sendResponse(exchange, 400, responseToClient.toString());
                }
            }
        }catch (SQLException e) {
                System.out.println("flag4");
                //Return 404, since sqlException either user_id or product_id not found
                responseToClient
                        .put("status", "Invalid Request");
        }finally{
            workRunning--;
        }
        System.out.println("flag2");
        String response = responseToClient.toString();
        exchange.sendResponseHeaders(400, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

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

    public static String forwardRequest(String targetURL, JSONObject jsonData) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(targetURL))
            .POST(HttpRequest.BodyPublishers.ofString(jsonData.toString(), StandardCharsets.UTF_8))
            .header("Content-Type", "application/json; utf-8")
            .header("Accept", "application/json")
            .build();
    
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    
        return response.body();
    }

    public static String forwardGetRequest(String targetURL) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(targetURL))
            .GET()
            .header("Accept", "application/json")
            .build();
    
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    
        return response.body();
    }

}