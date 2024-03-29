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
user_service_url = f"http://{config_data['UserService']['ip']}:{config_data['UserService']['port']}"
product_service_url = f"http://{config_data['ProductService']['ip']}:{config_data['ProductService']['port']}"

@app.route('/product/<int:product_id>', methods=['GET'])
def get_product(product_id):
    # Forward the GET request to the Product service
    product_service_response = requests.get(f"{product_service_url}/product/{product_id}")
    try:
        return jsonify(product_service_response.json())
    except requests.exceptions.RequestException as e:
        print("BROKEN ON INSIDE NOT SERVER")
        return jsonify({"error": "Internal server error."}), 500

@app.route('/product', methods=['POST'])
def product_endpoint():
    data = request.json

    try:
        # Forward the request as received to the Product service
        product_service_response = requests.post(f"{product_service_url}/product", json=data)

        if(product_service_response.status_code != 200):
            return jsonify(product_service_response.json()), product_service_response.status_code

        # Return a simplified response or customize as needed
        return product_service_response.json()
    except requests.exceptions.RequestException as e:
        print("BROKEN ON INSIDE NOT SERVER")
        return jsonify({"error": "Internal server error."}), 500

@app.route('/user/<int:user_id>', methods=['GET'])
def get_user(user_id):
    # Forward the GET request to the User service
    user_service_response = requests.get(f"{user_service_url}/user/{user_id}")

    try:
        return jsonify(user_service_response.json())
    except requests.exceptions.RequestException as e:
        print("BROKEN ON INSIDE NOT SERVER")
        return jsonify({"error": "Internal server error."}), 500

@app.route('/user', methods=['POST'])
def user_endpoint():
    data = request.json

    try:
        # Forward the request as received to the User service
        user_service_response = requests.post(f"{user_service_url}/user", json=data)
        print(user_service_response)

        if(user_service_response.status_code != 200):
            return jsonify(user_service_response.json()), user_service_response.status_code

        # Return a simplified response or customize as needed
        return user_service_response.json()
    
    except requests.exceptions.RequestException as e:
        print("BROKEN ON INSIDE NOT SERVER")
        return jsonify({"error": "Internal server error."}), 500

if __name__ == '__main__':
    app.run(host=iscs_host, debug=True, port=iscs_port, threaded=True, use_reloader=False)