#!/bin/bash

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

echo "PostgreSQL setup complete."
