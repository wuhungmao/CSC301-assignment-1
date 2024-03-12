#!/bin/bash

docker pull postgres

#Copied from docker postgres documentation. Just change password to password
docker run -d \
    --name postgres-container \
    -e POSTGRES_PASSWORD=password \
    -p 5432:5432 \
    postgres

# Wait or error
sleep 10

# Create databases, change name User, Product to avoid special postgres word errors
docker exec -it postgres-container psql -U postgres -c "CREATE DATABASE UserService;"
docker exec -it postgres-container psql -U postgres -c "CREATE DATABASE ProductService;"
docker exec -it postgres-container psql -U postgres -c "CREATE DATABASE OrderService;"

echo "PostgreSQL setup complete."


