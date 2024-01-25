import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;


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
    public static String jdbcUrl = "jdbc:sqlite:UserDatabase.db";

    public static void main(String[] args) throws IOException {

        int port = 8080;
        OrderHandler newHandler = new OrderHandler();
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newFixedThreadPool(20));
        server.createContext("/order", newHandler);
        server.start();

        System.out.println("Server started on port 8080");
    }


    static class OrderHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if("POST".equals((exchange.getRequestMethod()))) {
                JSONObject requestBody = getRequestBody(exchange);
                String command = requestBody.getString("command");

                String palceholder = requestBody.getString("USER create # john_doe john.doe@example.com password123";);
                Map<String, String> userData = parseUserCommand(palceholder);
                String jsonInputString = createJsonString(userData);
                if(requestBody.getString("command") != null){
                    if ("ORDER".equals(command)) {
                        jsonOrderRequest(requestBody, exchange);
                    }else if("PRODUCT".equals(command)){
                        jsonOrderRequest(requestBody, exchange);
                    }else if("USER".equals(command)){
                        postUser("http://localhost:80/user", jsonInputString);
                    }
                }
            } else {
                exchange.sendResponseHeaders(405, 0);
                exchange.close();
            }
        }
    }

    public static void jsonOrderRequest(JSONObject requestBody, HttpExchange exchange) throws IOException {
        System.out.println(requestBody);
        if(requestBody.getString("command") != null){
            String command = requestBody.getString("command");
            
            if ("place order".equals(command)) {
                if (requestBody.getString("user_id") != null && requestBody.getString("product_id") != null) {
                    int userId = requestBody.getInt("user_id");
                    int productId = requestBody.getInt("product_id");
                    int quantity = requestBody.getInt("quantity");

                    String checkUser = "SELECT * FROM user WHERE = '" + userId + "'";
                    String checkProduct = "SELECT * FROM product_id WHERE = '" + productId + "'";
                    String checkQuantity = "SELECT * FROM quantity WHERE product_id = '" + quantity + "'";

                    System.out.println("User ID: " + userId);
                    System.out.println("Product ID: " + productId);
                    System.out.println("Quantity: " + quantity);
                }
            }
        }

        String response = "Order processed";
        exchange.sendResponseHeaders(200, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    public static void postUser(String targetURL, String jsonInputString) throws IOException {
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

    public static String createJsonString(Map<String, String> userData) {
        return new JSONObject()
                .put("command", "create")
                .put("username", userData.get("username"))
                .put("email", userData.get("email"))
                .put("password", userData.get("password"))
                .toString();
    }

    public static Map<String, String> parseUserCommand(String command) {
    Map<String, String> userData = new HashMap<>();
    String[] parts = command.split(" ");
    
    if (parts.length >= 5 && "USER".equals(parts[0]) && "create".equals(parts[1])) {
        userData.put("username", parts[3]);
        userData.put("email", parts[4]);
        userData.put("password", parts[5]);
    }
    return userData;
}

}