#!/bin/bash

compile_code() {
    javac -cp ".:lib/sqlite-jdbc-3.45.0.jar" ProductService.java
    javac -cp ".:lib/sqlite-jdbc-3.45.0.jar" UserService.java
    javac -cp ".:lib/sqlite-jdbc-3.45.0.jar" OrderService.java
}

run_code() {
    java -cp ".:lib/sqlite-jdbc-3.45.0.jar" OrderService
    java -cp ".:lib/sqlite-jdbc-3.45.0.jar" ProductService
    java -cp ".:lib/sqlite-jdbc-3.45.0.jar" UserService
}

# Check if the first argument is "-c"
if [ "$1" == "-c" ]; then
    compile_code
if [ "$1" == "-u" ]; then
    java -cp ".:lib/sqlite-jdbc-3.45.0.jar" UserService
if [ "$1" == "-p" ]; then
    java -cp ".:lib/sqlite-jdbc-3.45.0.jar" ProductService
if [ "$1" == "-o" ]; then
    java -cp ".:lib/sqlite-jdbc-3.45.0.jar" OrderService
else
    echo "not a flag"
fi
