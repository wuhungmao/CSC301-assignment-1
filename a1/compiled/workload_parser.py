import requests
import json
import sys  # Import sys module to access command-line arguments
import socket

def get_order_service_port():
    try:
        with open("./config.json", 'r') as config_file:
            config_data = json.load(config_file)
            return config_data["OrderService"]["port"]
    except FileNotFoundError:
        print("Error: Config file 'config.json' not found.")
        sys.exit(1)
    except json.JSONDecodeError:
        print("Error: Unable to parse 'config.json'. Check its format.")
        sys.exit(1)

def get_order_service_ip():
    try:
        with open("./config.json", 'r') as config_file:
            config_data = json.load(config_file)
            return config_data["OrderService"]["ip"]
    except FileNotFoundError:
        print("Error: Config file 'config.json' not found.")
        sys.exit(1)
    except json.JSONDecodeError:
        print("Error: Unable to parse 'config.json'. Check its format.")
        sys.exit(1)

def process_line(line, order_service_url):
    # Parse information from the line
    # Construct the appropriate HTTP request
    # Send the request to the Order service
    # order_service_url contain ip address and port number to reach order service
    # order_service_url would be combined with different routes(/user, /order, /products)
    # Three handlers in order service will handle the request differently
    
    # Example:
    # Assuming each line is space-separated and the first word is the command
    print("inside process_line")
    parts = line.split()
    serviceType = parts[0]

    if serviceType == "USER":
        print("inside USER")
        command = parts[1] if len(parts) > 1 else None
        if command == "create": 
            id = parts[2] if len(parts) > 2 else None
            username = parts[3] if len(parts) > 3 else None
            email = parts[4] if len(parts) > 4 else None
            password = parts[5] if len(parts) > 5 else None
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
            id = parts[2] if len(parts) > 2 else None
            url = f"{order_service_url}/user/{id}"
            headers = {"Content-Type": "application/json"}

            response = requests.get(url, headers=headers)
            #still need to handle response

        elif command == "update":

            # Extract values without prefixes using split
            username_part = parts[3].split(":") if len(parts) > 3 else None
            email_part = parts[4].split(":") if len(parts) > 4 else None
            password_part = parts[5].split(":") if len(parts) > 5 else None
            
            # Assign the values if available, or None otherwise
            username = username_part[1] if username_part and len(username_part) > 1 else None
            email = email_part[1] if email_part and len(email_part) > 1 else None
            password = password_part[1] if password_part and len(password_part) > 1 else None
            
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
            id = parts[2] if len(parts) > 2 else None
            username = parts[3] if len(parts) > 3 else None
            email = parts[4] if len(parts) > 4 else None
            password = parts[5] if len(parts) > 5 else None
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
        print("inside PRODUCT")
        
        command = parts[1] if len(parts) > 1 else None
        if command == "create":
            id = parts[2] if len(parts) > 2 else None
            name = parts[3] if len(parts) > 3 else None
            description = parts[4] if len(parts) > 4 else None
            price = parts[5] if len(parts) > 5 else None
            quantity = parts[6] if len(parts) > 6 else None
            
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
            print("final url is " + url)
            response = requests.post(url, headers=headers, data=json.dumps(data))
            #still need to handle response

        elif command == "info":
            id = parts[2] if len(parts) > 2 else None
            url = f"{order_service_url}/product/{id}"
            headers = {"Content-Type": "application/json"}

            response = requests.get(url, headers=headers)
            #still need to handle response

            
        elif command == "update":
            id = parts[2] if len(parts) > 2 else None

            # Extract values without prefixes using split
            name_part = parts[3].split(":") if len(parts) > 3 else None
            description_part = parts[4].split(":") if len(parts) > 4 else None
            price_part = parts[5].split(":") if len(parts) > 5 else None
            quantity_part = parts[6].split(":") if len(parts) > 6 else None
            
            # Assign the values if available, or None otherwise
            name = name_part[1] if name_part and len(name_part) > 1 else None
            description = description_part[1] if description_part and len(description_part) > 1 else None
            price = price_part[1] if price_part and len(price_part) > 1 else None
            quantity = quantity_part[1] if quantity_part and len(quantity_part) > 1 else None
            
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
            id = parts[2] if len(parts) > 2 else None
            name = parts[3] if len(parts) > 3 else None
            price = parts[4] if len(parts) > 4 else None
            quantity = parts[5] if len(parts) > 5 else None
                         
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
        command = parts[1] if len(parts) > 1 else None
        product_id = parts[2] if len(parts) > 2 else None
        user_id = parts[3] if len(parts) > 3 else None
        quantity = parts[4] if len(parts) > 4 else None
                        
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

print("inside workload_parser")
workload_file = sys.argv[1]

# Get the IP address of the current machine
port_number = get_order_service_port()
ip_address = get_order_service_ip()

order_service_url = f"http://{ip_address}:{port_number}"

print("order service url is " + order_service_url)

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