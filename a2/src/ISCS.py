from flask import Flask, request, jsonify
import requests
import json

app = Flask(__name__)

# load the config
with open('config.json') as config_file:
    config_data = json.load(config_file)

user_api_endpoint = config_data['UserAPI']['endpoint']
product_api_endpoint = config_data['ProductAPI']['endpoint']

user_purchases = {}

@app.route('/order', methods=['POST'])
def order_endpoint():
    data = request.json
    user_id = data.get('user_id')
    product_id = data.get('product_id')

    if not user_id or not product_id:
        return jsonify({'error': 'user_id and product_id are required'}), 400

    if user_id not in user_purchases:
        user_purchases[user_id] = []
    user_purchases[user_id].append(product_id)

    # send confirmation to the user service that a product has been bought
    user_response = requests.post(f"{user_api_endpoint}/user/{user_id}/purchase", json={'product_id': product_id})

    return jsonify({
        'user_id': user_id,
        'product_id': product_id,
        'purchase_status': 'Completed',
        'user_service_response': user_response.json()
    })

@app.route('/user/<int:user_id>/purchases', methods=['GET'])
def get_user_purchases(user_id):
    # retrieve the list of products purchased by a user
    purchases = user_purchases.get(user_id, [])
    return jsonify({
        'user_id': user_id,
        'purchases': purchases
    })

if __name__ == '__main__':
    app.run(debug=True)