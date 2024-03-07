import json
import sys
import os
import requests

ORDER_PATH = "order_testcases.json"
ORDER_RES_PATH = "order_responses.json"
USER_PATH = "user_testcases.json"
USER_RES_PATH = "user_responses.json"
PRODUCT_PATH = "product_testcases.json"
PRODUCT_RES_PATH = "product_responses.json"
CONFIG_PATH = "config.json"

def handle_response(response, expected_response):
    # Check if response is successful and content type is JSON
    if response.status_code == 200 and response.headers.get('Content-Type') == 'application/json':
        try:
            res_content = response.json()
        except json.JSONDecodeError:
            print("Received non-JSON response even though JSON was expected")
            res_content = None
    else:
        print(f"Received response with status code: {response.status_code}")
        res_content = None  # Or set to response.text to handle non-JSON body

    # Compare the actual response with the expected response
    res = (json.dumps(res_content, sort_keys=True).lower() == json.dumps(expected_response, sort_keys=True).lower())
    return res, res_content

def main(argv: list):
    # Load configuration data
    try:
        with open(CONFIG_PATH, 'r', encoding='utf-8') as config:
            config_data = json.load(config)
    except Exception as e:
        print(f"Error loading {CONFIG_PATH}: {str(e)}")
        return -1

    # Setup service URL
    order_service_ip = "127.0.0.1"
    order_service_port = config_data["OrderService"].get("port")
    url = f"http://{order_service_ip}:{order_service_port}"

    # Load request and expected response data
    test_data = [
        (USER_PATH, USER_RES_PATH, '/user'),
        (PRODUCT_PATH, PRODUCT_RES_PATH, '/product'),
        (ORDER_PATH, ORDER_RES_PATH, '/order'),
    ]

    for req_path, res_path, endpoint in test_data:
        try:
            with open(req_path, 'r', encoding='utf-8') as f_req, open(res_path, 'r', encoding='utf-8') as f_res:
                req_data = json.load(f_req)
                res_data = json.load(f_res)
        except Exception as e:
            print(f"Error loading test data: {str(e)}")
            continue

        for key in req_data.keys():
            if 'get' in key:
                response = requests.get(url=url + f"{endpoint}/{req_data[key]['id']}", json=req_data[key])
            else:
                headers = {'Content-Type': 'application/json'}
                response = requests.post(url=url + endpoint, json=req_data[key], headers=headers)
            
            res, res_content = handle_response(response, res_data[key])

            if not res:
                print("------------------------------------------------------------------------")
                print(f"Test Name: {key}")
                print(f"Request Body: {req_data[key]}")
                print(f"Expected: {json.dumps(res_data[key], sort_keys=True)}")
                print(f"RESPONSE CONTENT: {json.dumps(res_content, sort_keys=True) if res_content else 'N/A'}")
                print(f"RESPONSE: {json.dumps(response.json(), sort_keys=True) if res_content else response.text}")
                print(f"CODE: {response.status_code}")
                print("------------------------------------------------------------------------")

if __name__ == "__main__":
    main(sys.argv)