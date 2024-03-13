#!/bin/bash

docker pull postgres

# Run PostgreSQL container with custom configuration
docker run -d \
    --name postgres-container \
    -e POSTGRES_PASSWORD=password \
    -p 5432:5432 \
    postgres

# Wait for PostgreSQL to initialize
sleep 9

docker exec -it postgres-container sed -i "s/#listen_addresses = 'localhost'/listen_addresses = '*'/g" /var/lib/postgresql/data/postgresql.conf

docker exec -it postgres-container service postgresql restart

docker exec -it postgres-container psql -U postgres -c "CREATE DATABASE users;"
docker exec -it postgres-container psql -U postgres -c "CREATE DATABASE product;"
docker exec -it postgres-container psql -U postgres -c "CREATE DATABASE orders;"

sleep 5

docker exec -it postgres-container psql -U postgres -d users -c "CREATE TABLE IF NOT EXISTS users (user_id INTEGER PRIMARY KEY, username TEXT NOT NULL, email TEXT NOT NULL UNIQUE, password TEXT NOT NULL);"
docker exec -it postgres-container psql -U postgres -d product -c "CREATE TABLE products (productId INTEGER PRIMARY KEY, productName TEXT NOT NULL, description TEXT NOT NULL, price DOUBLE PRECISION NOT NULL, quantity INTEGER NOT NULL);"
docker exec -it postgres-container psql -U postgres -d orders -c "CREATE TABLE IF NOT EXISTS orders (orderId SERIAL PRIMARY KEY, userId INTEGER NOT NULL, productId INTEGER NOT NULL, quantity INTEGER NOT NULL);"

