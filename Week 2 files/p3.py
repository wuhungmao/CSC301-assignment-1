import requests


def make_post_request(url, data):
  try:
    headers = {'Content-Type' : 'application/json', 'Authorization' : 'Bearer your_token'}
    response = requests.post(url, data=data, headers=headers)
    if response.status_code == 200:
      print(f"POST request did work: {response.status_code}")  
      print("Response: ", response.text)
    
    else:
      print(f"POST request did not work: {response.status_code}")  
      print("Response: ", response.text)
  except Exception as e:
    print(e)
if __name__ == "__main__":
  url = 'http://127.0.0.1:8081/test2'

  data = {'key1' : 'value1', 'key2': 'value2'} 

  make_post_request(url, data)
