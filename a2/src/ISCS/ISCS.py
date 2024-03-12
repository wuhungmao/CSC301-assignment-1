from flask import Flask, request, jsonify
import requests
import json

app = Flask(__name__)

# Load the config
def load_config():
    with open('config.json') as config_file:
        return json.load(config_file)
    
    
config_data = load_config()

# Retrieve host and port for ISCS from the config
iscs_host = config_data['InterServiceCommunication']['ip']
iscs_port = config_data['InterServiceCommunication']['port']

# Retrieve endpoints for user and product services from the config
user_api_endpoint_format = f"http://{config_data['UserService']['ip']}:"
product_api_endpoint_format = f"http://{config_data['ProductService']['ip']}:"


user_api_ports = config_data["UserService"]["ports"]
product_api_ports = config_data["ProductService"]["ports"]

user_api_endpoints = []
product_api_endpoints = []

# There may be more ports for user than product, or vice versa
for i in range(len(user_api_ports)):
    user_api_endpoints.append(user_api_endpoint_format + str(user_api_ports[i]))

for i in range(len(product_api_ports)):
    product_api_endpoints.append(product_api_endpoint_format + str(product_api_ports[i]))

print(user_api_endpoints, product_api_endpoints)
userIndex = 0
productIndex = 0

@app.route('/product', methods=['POST'])
def product_endpoint():
    data = request.json

    # Forward the request as received to the Product service
    product_service_response = requests.post(f"{product_api_endpoints[productIndex]}", json=data)
    if productIndex >= len(product_api_ports) - 1:
        productIndex = 0
    else:
        productIndex += 1

    if product_service_response.status_code != 200:
        return jsonify(product_service_response.json()), product_service_response.status_code

    # Return a simplified response or customize as needed
    return product_service_response.json()

@app.route('/user', methods=['POST'])
def user_endpoint():
    data = request.json

    # Forward the request as received to the User service
    user_service_response = requests.post(f"{user_api_endpoints[userIndex]}", json=data)
    if userIndex >= len(user_api_ports) - 1:
        userIndex = 0
    else:
        userIndex += 1

    if user_service_response.status_code != 200:
        return jsonify(user_service_response.json()), user_service_response.status_code

    # Return a simplified response or customize as needed
    return user_service_response.json()

if __name__ == '__main__':
    app.run(host=iscs_host, port=iscs_port)