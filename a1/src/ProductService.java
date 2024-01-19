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
    
}
