package src;
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
/*User Service:
This must be written in Java and must be started with the following command: "java UserService config.json"
Implement a user microservice responsible for user management.
Users should have attributes such as id, username, email, and password.
Provide endpoints for user creation, retrieval, updating, and deletion.
API endpoint: /user
POST Methods: 
{
    "command": "create",
    "id": 23823,
    "username": "username-32843hnksjn4398",
    "email": "foo@bar.com",
    "password": "34289nkjni3w4u"
}
==> Creates an entry in the DB for a user. 

{
    "command": "update",
    "id": 23823,
    "username": "username-32843hnksjn4398",
    "email": "foo@foobar.com",
    "password": "34289nkjni3w4u"
}
==> Updates user with id 23823
--> Updates all fields that are present (username, email, password); 
--> If fields are missing, only update the fields that are present. 
(e.g., if no "password" is transmitted, then only update the username and email).

{
    "command": "delete",
    "id": 23823,
    "username": "username-32843hnksjn4398",
    "email": "foo@bar.com",
    "password": "34289nkjni3w4u"
}
==> delete this user ONLY IFF all fields (username, email and password) correspond. 

GET Methods:
/user/23823
==> 
returns the following JSON in the response body. 
{
    "id": 23823,
    "username": "username-32843hnksjn4398",
    "email": "foo@bar.com",
}
*/
public class UserService {
    /*For Http request*/
    int port = 80;
    HttpServer UserServer = HttpServer.create(new InetSocketAddress(port), 0);
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
