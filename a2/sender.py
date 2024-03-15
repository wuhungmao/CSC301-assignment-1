import requests
import time
from threading import Thread

# Function to send POST requests
def send_post_request(url, data):
    try:
        response = requests.post(url, data=data)
        print("POST Request Status Code:", response.status_code)
    except Exception as e:
        print("Error sending POST request:", e)

# Function to send GET requests
def send_get_request(url):
    try:
        response = requests.get(url)
        if response.status_code != 200:
          print("GET Request Status Code:", response.status_code)
          import sys
          sys.exit(32)
    except Exception as e:
        print("Error sending GET request:", e)

# Function to send requests at a specified rate
def send_requests(url, requests_per_second):
    count = 0
    while True:
        count += 1
        start_time = time.time()

        # Send POST request with slightly different data each time
        """post_data = {   
            "command": "update",
            "id": 2001,
            "name": "product2001-update",
            "description": "This is product 2001 version 2",
            "price": 199.99  + time.time(),
            "quantity": 100
        }
        send_post_request(url, post_data)"""

        # Send GET request
        send_get_request(url)

        # Calculate time taken for this iteration
        iteration_time = time.time() - start_time

        
        # Calculate time to sleep before next iteration to achieve desired requests per second
        time_to_sleep = max(1/requests_per_second, 1 / requests_per_second - iteration_time)
        time.sleep(time_to_sleep)
        
        # Calculate requests per second
        elapsed_time = time.time() - start_time
        actual_requests_per_second = 1 / elapsed_time
        if count % (5*requests_per_second) == 0:
          print("Requests per second:", actual_requests_per_second, "sleep time:", time_to_sleep)

# Main function
if __name__ == "__main__":
    url = "http://127.0.0.1:8082/user/2011"  # Replace with your URL
    requests_per_second = 100  # Replace with desired requests per second
    # Start a thread to send requests repeatedly
    t = Thread(target=send_requests, args=(url, requests_per_second))
    t.start()

