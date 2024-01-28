To compile

For linux
javac -classpath "./compiled/*" -d ./compiled src/ProductService.java 

java -classpath "./compiled/*"  src/ProductService.java config.json

javac -classpath "./compiled/*" -d ./compiled src/UserService.java

java -classpath "./compiled/*"  src/UserService.java config.json 


For product service

curl -X POST -H "Content-Type: application/json" -d '{
    "command": "create",
    "id": 1,
    "name": "Product 1",
    "description": "Description for Product 1",
    "price": 19.99,
    "quantity": 5
}' http://127.0.0.1:8080/product

curl -X POST -H "Content-Type: application/json" -d '{
    "command": "update",
    "id": 1,
    "name": "Updated Product 1",
    "description": "Updated description for Product 1",
    "price": 24.99,
    "quantity": 8
}' http://127.0.0.1:8080/product


curl -X POST -H "Content-Type: application/json" -d '{
    "command": "delete",
    "id": 1
}' http://127.0.0.1:8080/product

curl -X GET -H "Content-Type: application/json" -d '{"id": 1}' http://127.0.0.1:8080/product



For user service

curl -X POST -H "Content-Type: application/json" -d '{
    "command": "create",
    "id": 23823,
    "username": "username-32843hnksjn4398",
    "email": "foo@bar.com",
    "password": "34289nkjni3w4u"
}' http://127.0.0.1:8081/user

curl -X POST -H "Content-Type: application/json" -d '{
    "command": "update",
    "id": 23823,
    "username": "username-32843hnksjn4398",
    "email": "foo@foobar.com",
    "password": "34289nkjni3w4u"
}' http://127.0.0.1:8081/user

curl -X POST -H "Content-Type: application/json" -d '{
    "command": "delete",
    "id": 23823,
    "username": "username-32843hnksjn4398",
    "email": "foo@bar.com",
    "password": "34289nkjni3w4u"
}' http://127.0.0.1:8081/user

curl -X GET -H "Content-Type: application/json" -d '{
    "id": 23823
}' http://127.0.0.1:8081/user

