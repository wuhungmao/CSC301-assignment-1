import requests
import json
import sys  # Import sys module to access command-line arguments

def process_line(line, order_service_url):
    # Parse information from the line
    # Construct the appropriate HTTP request
    # Send the request to the Order service
    # order_service_url contain ip address and port number to reach order service
    # order_service_url would be combined with different routes(/user, /order, /products)
    # Three handlers in order service will handle the request differently
    
    # Example:
    # Assuming each line is space-separated and the first word is the command
    parts = line.split()
    serviceType = parts[0]

    if serviceType == "USER":
        command = parts[1]
        if command == "create":
            id = parts[2]
            username = parts[3]
            email = parts[4]
            password = parts[5]
            # Construct user-related HTTP request
            url = f"{order_service_url}/user"
            headers = {"Content-Type": "application/json"}

            data = {
                "command": command,
                "id": id,
                "username": username,
                "email": email,
                "password": password
            }

            response = requests.post(url, headers=headers, data=json.dumps(data))
            #still need to handle response

        elif command == "get":
            id = parts[2]
            url = f"{order_service_url}/user"
            headers = {"Content-Type": "application/json"}

            data = {
                "id": id,
            }

            response = requests.post(url, headers=headers, data=json.dumps(data))
            #still need to handle response

        elif command == "update":
            id = parts[2]
            username = parts[3]
            email = parts[4]
            password = parts[5]
            url = f"{order_service_url}/user"
            headers = {"Content-Type": "application/json"}

            data = {
                "command": command,
                "id": id,
                "username": username,
                "email": email,
                "password": password
            }

            response = requests.post(url, headers=headers, data=json.dumps(data))
            #still need to handle response

        elif command == "delete":
            id = parts[2]
            username = parts[3]
            email = parts[4]
            password = parts[5]
            url = f"{order_service_url}/user"
            headers = {"Content-Type": "application/json"}

            data = {
                "command": command,
                "id": id,
                "username": username,
                "email": email,
                "password": password
            }

            response = requests.post(url, headers=headers, data=json.dumps(data))
            #still need to handle response

        else:
            print("Unknown User command type")
    elif serviceType == "PRODUCT":
        command = parts[1]
        if command == "create":
            id = parts[2]
            name = parts[3]
            description = parts[4]
            price = parts[5]
            quantity = parts[6]
            
            url = f"{order_service_url}/product"
            headers = {"Content-Type": "application/json"}

            data = {
                "command": command,
                "id": id,
                "name": name,
                "description": description,
                "price": price,
                "quantity": quantity
            }

            response = requests.post(url, headers=headers, data=json.dumps(data))
            #still need to handle response

        elif command == "info":
            id = parts[2]
            url = f"{order_service_url}/product"
            headers = {"Content-Type": "application/json"}

            data = {
                "id": id,
            }

            response = requests.post(url, headers=headers, data=json.dumps(data))
            #still need to handle response

            
        elif command == "update":
            id = parts[2]
            name = parts[3]
            description = parts[4]
            price = parts[5]
            quantity = parts[6]
                        
            url = f"{order_service_url}/product"
            headers = {"Content-Type": "application/json"}

            data = {
                "command": command,
                "id": id,
                "name": name,
                "description": description,
                "price": price,
                "quantity": quantity
            }

            response = requests.post(url, headers=headers, data=json.dumps(data))
            #still need to handle response

            
        elif command == "delete":
            id = parts[2]
            name = parts[3]
            price = parts[5]
            quantity = parts[6]
                         
            url = f"{order_service_url}/product"
            headers = {"Content-Type": "application/json"}

            data = {
                "command": command,
                "id": id,
                "name": name,
                "price": price,
                "quantity": quantity
            }

            response = requests.post(url, headers=headers, data=json.dumps(data))
            #still need to handle response

        else:
            print("Unknown PRODUCT command type")
    elif serviceType == "ORDER":
        command = parts[1]
        product_id = parts[2]
        user_id = parts[3]
        quantity = parts[4]
                        
        url = f"{order_service_url}/order"
        headers = {"Content-Type": "application/json"}

        data = {
            "command": command,
            "product_id": product_id,
            "user_id": user_id,
            "quantity": quantity
        }

        response = requests.post(url, headers=headers, data=json.dumps(data))
        #still need to handle response

    else:
        print("Invalid service:", serviceType)

if len(sys.argv) != 4:
    print("Usage: python script.py <IP_address_of_Order_service> <port_number_of_Order_service> <workload_file_name>")
    sys.exit(1)

ip_address = sys.argv[1]
port_number = sys.argv[2]
workload_file = sys.argv[3]

order_service_url = f"http://{ip_address}:{port_number}"

try:
    with open(workload_file, 'r') as file:
        # Process each line
        for line in file:
            process_line(line.strip(), order_service_url)  # Strip removes leading/trailing whitespaces
except FileNotFoundError:
    print(f"Error: File '{workload_file}' not found.")
except requests.RequestException as e:
    print(f"HTTP Request Error: {e}")
except Exception as e:
    print(f"An unexpected error occurred: {e}")