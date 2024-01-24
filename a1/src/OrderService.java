package src;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import netscape.javascript.JSObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import src.JSONObject;

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
        JSONObject s = new JSONObject();
        //Replace this with a get request from database
        Map<String, String> map = new HashMap<String, String>();
        map.put("command", "place order");
        map.put("user_id", "");
        map.put("product_id", "2");
        map.put("quantity", "3");

        // Open a connection
        Connection connection = DriverManager.getConnection(jdbcUrl);

        //Break down the 'JSON'
        String user = s.getString("user_id");
        String productID = s.getString("product_id");
        String quantity = s.getString("quantity");

        try (Statement statement = connection.createStatement()) {
            // Execute the query to create the User table
            statement.executeUpdate(createTableQuery);
            System.out.println("User table created successfully.");
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }
        // Close the connection
        connection.close();

        /*For Http request*/
        int port = 80;
        HttpServer UserServer = HttpServer.create(new InetSocketAddress(port), 0);
        // Example: Set a custom executor with a fixed-size thread pool
        UserServer.setExecutor(Executors.newFixedThreadPool(1)); // Adjust the pool size as needed
        // Set up context for /test POST request
        UserServer.createContext("/user", new UserService.TestHandler());

        UserServer.setExecutor(null); // creates a default executor

        UserServer.start();

        System.out.println("User Server started on port " + port);
    }

    public static boolean handleOrder(String userID, String productID, String quantity){
        String checkUser = "SELECT * FROM user WHERE = '" + userID + "'";
        String checkProduct = "SELECT * FROM product_id WHERE = '" + productID + "'";
        String checkQuantity = "SELECT * FROM quantity WHERE product_id = '" + quantity + "'";
        
        
        return true;
    }

}