
# -- ./runme.sh -c  --> which when run by the TAs, compiles all your code. 
# javac -cp ".:lib/sqlite-jdbc-x.x.x.jar" YourClass.java
# java -cp ".:lib/sqlite-jdbc-x.x.x.jar" YourClass

# -- ./runme.sh -u  --> starts the User service

# -- ./runme.sh -p  --> starts the Product service

# -- ./runme.sh -i  --> starts the ISCS

# -- ./runme.sh -o  --> starts the Order service

# -- ./runme.sh -w workloadfile --> starts the workload parser on the same machine as the order service

# 1) java ISCS config.json (or python3 ISCS.py config.json, etc.)
# 2) java UserService config.json
# 3) java ProductService config.json
# 4) java OrderService config.json

# Sample ConfigFile:
# {
#   "UserService": {
#         "port": 14001,
#         "ip": "127.0.0.1"
#     }   ,
#     "OrderService": {
#         "port": 14000,
#         "ip": "142.1.46.48"
#     }   ,
#     "ProductService": {
#         "port": 15000,
#         "ip": "142.1.46.49"
#     }   ,
#     "InterServiceCommunication": {
#         "port": 14000,
#         "ip": "127.0.0.1"
#     }   
# }
#!/bin/bash

case "$1" in
  -c)
    # Compile code, download flask and docker

    docker pull postgres

    # Run PostgreSQL container with custom configuration
    docker run -d \
        --name postgres-container \
        -e POSTGRES_PASSWORD=password \
        -p 5432:5432 \
        postgres

    # Wait for PostgreSQL to initialize
    sleep 10

    docker exec -it postgres-container sed -i "s/#listen_addresses = 'localhost'/listen_addresses = '*'/g" /var/lib/postgresql/data/postgresql.conf

    docker exec -it postgres-container service postgresql restart

    # Create databases
    docker exec -it postgres-container psql -U postgres -c "CREATE DATABASE Users;"
    docker exec -it postgres-container psql -U postgres -c "CREATE DATABASE Product"
    docker exec -it postgres-container psql -U postgres -c "CREATE DATABASE Orders;"

    javac -classpath "./compiled/*" -d ./compiled src/UserService/UserService.java
    javac -classpath "./compiled/*" -d ./compiled src/ProductService/ProductService.java
    pip3 install Flask
    # Compile OrderService.java (Adjust the path accordingly)
    #javac -cp "$classpath" src/OrderService.java
    javac -classpath "./compiled/*" -d ./compiled src/OrderService/OrderService.java
    ;;
  -u)
    # Start User service
    java -classpath "./compiled/*" src/UserService/UserService.java config.json 
    ;;
  -p)
    # Start Product service
    java -classpath "./compiled/*" src/ProductService/ProductService.java config.json
    ;;
  -i)
    # Start ISCS
    python3 ./src/ISCS/ISCS.py
    ;;
  -o)
    # Start Order service
    java -classpath "./compiled/*" src/OrderService/OrderService.java config.json
    ;;
  -w)
    # Start Workload parser with specified workload file
    # Assuming workload file is provided as the second argument
    python3 ./compiled/workload_parser.py $2
    ;;
  *)
    echo "Invalid option. Usage: $0 -c|-u|-p|-i|-o|-w workloadfile"
    exit 1
    ;;
esac


