
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class Main {

    public static void main(String[] args) throws IOException {
        int port = 8081;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        // Example: Set a custom executor with a fixed-size thread pool
        server.setExecutor(Executors.newFixedThreadPool(20)); // Adjust the pool size as needed
        // Set up context for /test POST request
        server.createContext("/test", new TestHandler());

        // Set up context for /test2 GET request
        server.createContext("/test2", new Test2Handler());


        server.setExecutor(null); // creates a default executor

        server.start();

        System.out.println("Server started on port " + port);
    }

    static class TestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Handle POST request for /test
            if ("POST".equals(exchange.getRequestMethod())) {
                String response = "Lecture foobar foobar Received POST request for /test";

                String clientAddress = exchange.getRemoteAddress().getAddress().toString();
                String requestMethod = exchange.getRequestMethod();
                String requestURI = exchange.getRequestURI().toString();
                Map<String, List<String>> requestHeaders = exchange.getRequestHeaders();

                System.out.println("Client Address: " + clientAddress);
                System.out.println("Request Method: " + requestMethod);
                System.out.println("Request URI: " + requestURI);
                System.out.println("Request Headers: " + requestHeaders);
                // Print all request headers
                for (Map.Entry<String, List<String>> header : requestHeaders.entrySet()) {
                    System.out.println(header.getKey() + ": " + header.getValue().getFirst());
                }

                System.out.println("Request Body: "+ getRequestBody(exchange));

                sendResponse(exchange, response);

            } else {
                // Send a 405 Method Not Allowed response for non-POST requests
                exchange.sendResponseHeaders(405, 0);
                exchange.close();
            }
        }
        //Get request message if it is a post request
        private static String getRequestBody(HttpExchange exchange) throws IOException {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
                StringBuilder requestBody = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    requestBody.append(line);
                }
                return requestBody.toString();
            }
        }

    }

    static class Test2Handler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Handle GET request for /test2
            // TODO let's do this in class.
            if ("GET".equals(exchange.getRequestMethod())) {
                String requestMethod = exchange.getRequestMethod();
                String clientAddress = exchange.getRemoteAddress().getAddress().toString();
                String requestURI = exchange.getRequestURI().toString();

                System.out.println("Request method: " + requestMethod);
                System.out.println("Client Address: " + clientAddress);
                System.out.println("Request URI: " + requestURI);

                String response = "Received GET for /test2 lecture foo W.";
                sendResponse(exchange, response);
                
            } else {
                exchange.sendResponseHeaders(405,0);
                exchange.close();
            }

        }
    }

    static class TestUserHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Handle GET request for /test2
            // TODO let's do this in class.
        }
    }

    private static void sendResponse(HttpExchange exchange, String response) throws IOException {
        exchange.sendResponseHeaders(200, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes(StandardCharsets.UTF_8));
        os.close();
    }
}

