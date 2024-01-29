To compile

For linux
javac -classpath "./compiled/*" -d ./compiled src/ProductService/ProductService.java

java -classpath "./compiled/*"  src/ProductService/ProductService.java config.json

javac -classpath "./compiled/*" -d ./compiled src/UserService/UserService.java

java -classpath "./compiled/*"  src/UserService/UserService.java config.json

For product service
curl -X POST -H "Content-Type: application/json" -d '{"command":"create","id":1,"name":"SampleProduct","description":"SampleDescription","price":20.5,"quantity":50}' http://127.0.0.1:8080/product

curl -X POST -H "Content-Type: application/json" -d '{"command":"update","id":1,"name":"UpdatedProduct","description":"UpdatedDescription","price":25.5,"quantity":60}' http://127.0.0.1:8080/product

curl -X POST -H "Content-Type: application/json" -d '{"command":"delete","id":1,"name":"UpdatedProduct","description":"UpdatedDescription","price":25.5,"quantity":60}' http://127.0.0.1:8080/product

curl -X GET http://127.0.0.1:8080/product/1


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

curl -X GET http://127.0.0.1:8081/user/23823

