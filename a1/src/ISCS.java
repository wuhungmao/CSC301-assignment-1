/*Inter-service Communication Service:
This can be written in any language.
Use RESTful APIs for communication between microservices.
Implement error handling and appropriate status codes for API responses.
Utilize appropriate data formats for communication (e.g., JSON).
Provide configuration settings for microservices to communicate within the same LAN.
Implement load balancing for distributing requests among instances of microservices.
Why is this service needed? 
To pre-plan the ability to scale up for A2 and to pre-plan for additional features.
Think of this service as something that the order service sends data to; then this
ISCS, inspects the payload (or has its own API endpoints) and just constructs/forwards
the payload to the correct microservice.
(Thought: Could you use this for caching too to speed up and increase
 your system's transactions per second?) */