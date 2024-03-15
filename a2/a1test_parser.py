
import json
import sys
import requests

ORDER_PATH = r"order_testcases.json"
ORDER_RES_PATH = r"order_responses.json"

USER_PATH = r"user_testcases.json"
USER_RES_PATH = r"user_responses.json"

PRODUCT_PATH = r"product_testcases.json"
PRODUCT_RES_PATH = r"product_responses.json"

CONFIG_PATH = r"config.json"

# current_dir = os.path.dirname(os.path.abspath(__file__))
# CONFIG_PATH = current_dir + "/a1_test_cases/order_testcases.json" 


def main(argv: list):
    config_data = None
    try:
        with open(CONFIG_PATH, 'r',  encoding='utf-8') as config:
            config_data = json.load(config)
    except FileNotFoundError:
        print(f"Error: File '{CONFIG_PATH}' not found.")
        return -1
    except json.JSONDecodeError:
        print(f"Error: JSON failed to parse {CONFIG_PATH}")
        return -1
    order_service_ip = "127.0.0.1"
    order_service_port = config_data["OrderService"].get("port")
    order_url = f"http://{order_service_ip}:{order_service_port}"

    user_req_data = None
    user_res_data = None
    try:
        with open(USER_PATH, 'r',  encoding='utf-8') as config:
            user_req_data = json.load(config)
    except FileNotFoundError:
        print(f"Error: File '{USER_PATH}' not found.")
        return -1
    except json.JSONDecodeError:
        print(f"Error: JSON failed to parse {USER_PATH}")
        return -1
    try:
        with open(USER_RES_PATH, 'r',  encoding='utf-8') as config:
            user_res_data = json.load(config)
    except FileNotFoundError:
        print(f"Error: File '{USER_RES_PATH}' not found.")
        return -1
    except json.JSONDecodeError:
        print(f"Error: JSON failed to parse {USER_RES_PATH}")
        return -1
    url=f"http://127.0.0.1:8081/user/"
    for key in user_req_data.keys():
        # print("key is ", key)
        # print("REQUEST :" + str(user_req_data[key]))
        if 'get' in str(key):
            response = requests.get(url=url + f"{user_req_data[key]['id']}", json=user_req_data[key])
            # res_content = response.json()
            try: 
                res_content = response.json().get('content')
            except Exception as e:
                res_content = response.json()
        else:
            headers = {'Content-Type' : 'application/json'}
            # print("url is ", url)
            # print("json is ", user_req_data[key])
            response = requests.post(url, json=user_req_data[key], headers=headers)
            try: 
                res_content = response.json().get('content')
            except Exception as e:
                # print("response is", response)
                # print("Error decoding JSON:", e)
                # print("key is ", key)
                res_content = response.json()
                print(res_content)

            #compare response content with expected
            res1 = json.dumps(res_content, sort_keys=True).lower() == json.dumps(user_res_data[key], sort_keys=True).lower()
            
            #compare response with expected
            res2 = json.dumps(response.json(), sort_keys=True).lower() == json.dumps(user_res_data[key], sort_keys=True).lower()
            res = res1 or res2 
            if not res:
                print("------------------------------------------------------------------------")
                print("Test Name:" + key)
                print("Request Body:" + str(user_req_data[key]))
                print("Expected :" + json.dumps(user_res_data[key], sort_keys=True))
                print("RESPONSE CONTENT:" + json.dumps(res_content, sort_keys=True))
                print("RESPONSE :" + json.dumps(response.json(), sort_keys=True))
                print("CODE :" + str(response.status_code))
                print("res :" ,res)
                print("------------------------------------------------------------------------")
    # print(len(user_req_data))
    print(len(user_res_data))

    product_req_data = None
    product_res_data = None
    try:
        with open(PRODUCT_PATH, 'r',  encoding='utf-8') as config:
            product_req_data = json.load(config)
    except FileNotFoundError:
        print(f"Error: File '{PRODUCT_PATH}' not found.")
        return -1
    except json.JSONDecodeError:
        print(f"Error: JSON failed to parse {PRODUCT_PATH}")
        return -1
    try:
        with open(PRODUCT_RES_PATH, 'r',  encoding='utf-8') as config:
            product_res_data = json.load(config)
    except FileNotFoundError:
        print(f"Error: File '{PRODUCT_RES_PATH}' not found.")
        return -1
    except json.JSONDecodeError:
        print(f"Error: JSON failed to parse {PRODUCT_RES_PATH}")
        return -1
    # print(len(product_req_data))
    # print(len(product_res_data))
    url = f"http://127.0.0.1:8080/product/"
    for key in product_req_data.keys():
        print("REQUEST :" + str(product_req_data[key]))
        if 'get' in str(key):
            response = requests.get(url=url + f"/product/{product_req_data[key]['id']}", json=product_req_data[key])
            try: 
                res_content = response.json().get('content')
            except Exception as e:
                res_content = response.json()
            # res_content = response.json()
        else:
            headers = {'Content-Type' : 'application/json'}
            response = requests.post(url, json=product_req_data[key], headers=headers)
            try: 
                res_content = response.json().get('content')
            except Exception as e:
                res_content = response.json()
        res = (json.dumps(res_content, sort_keys=True).lower() == json.dumps(product_res_data[key], sort_keys=True).lower()
               or json.dumps(response.json(), sort_keys=True).lower() == json.dumps(product_res_data[key], sort_keys=True).lower())
        if not res:
            print("------------------------------------------------------------------------")
            print("Test Name:" + key)
            print("Request Body:" + str(product_req_data[key]))
            print("Expected :" + json.dumps(product_res_data[key], sort_keys=True))
            print("RESPONSE CONTENT:" + json.dumps(res_content, sort_keys=True))
            print("RESPONSE :" + json.dumps(response.json(), sort_keys=True))
            print("CODE :" + str(response.status_code))
            print(res)
            print("------------------------------------------------------------------------")

    order_req_data = None
    order_res_data = None
    try:
        with open(ORDER_PATH, 'r',  encoding='utf-8') as config:
            order_req_data = json.load(config)
    except FileNotFoundError:
        print(f"Error: File '{ORDER_PATH}' not found.")
        return -1
    except json.JSONDecodeError:
        print(f"Error: JSON failed to parse {ORDER_PATH}")
        return -1
    try:
        with open(ORDER_RES_PATH, 'r',  encoding='utf-8') as config:
            order_res_data = json.load(config)
    except FileNotFoundError:
        print(f"Error: File '{ORDER_RES_PATH}' not found.")
        return -1
    except json.JSONDecodeError:
        print(f"Error: JSON failed to parse {ORDER_RES_PATH}")
        return -1
    for key in order_req_data.keys():
        # print("REQUEST :" + str(user_req_data[key]))
        headers = {'Content-Type' : 'application/json'}
        url = f"http://127.0.0.1:8082/order/"
        response = requests.post(url , json=order_req_data[key], headers=headers)
        try: 
                res_content = response.json().get('content')
        except Exception as e:
            res_content = response.json()
        res = (json.dumps(res_content, sort_keys=True).lower() == json.dumps(order_res_data[key], sort_keys=True).lower()
               or json.dumps(response.json(), sort_keys=True).lower() == json.dumps(order_res_data[key], sort_keys=True).lower())        
        if not res:
            print("------------------------------------------------------------------------")
            print("Test Name:" + key)
            print("Request Body:" + str(order_req_data[key]))
            print("Expected :" + json.dumps(order_res_data[key], sort_keys=True))
            print("RESPONSE CONTENT:" + json.dumps(res_content, sort_keys=True))
            print("RESPONSE :" + json.dumps(response.json(), sort_keys=True))
            print("CODE :" + str(response.status_code))
            print(res)
            print("------------------------------------------------------------------------")
    # print(len(order_req_data))
    # print(len(order_res_data))

if __name__ == "__main__":
    main(sys.argv)