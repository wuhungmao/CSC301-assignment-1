
Review: https://piazza.com/class/lrex0yhb7fu7ib/post/216

Same instructions as for A1 in terms of what to submit and supply for marking.
(i.e, same directory structure, same idea of supplying compiled and working code). 


Learning outcomes:
- scaling services horizontally
- deployment of distributed services (no need for Chef/Puppet/etc., simple bash with ssh would likely suffice).

Due dates:
Component 1 is due March 8th, 9pm. (plus grace tokens)
Component 2 is due March 5th, 9pm, but submissions will be accepted without penalty and including all grace tokens until March 12th 9pm.
You must book a demo before the final submission of Component 2. Demos will happen on March 13th, March 14th and March 15 (location to be specified), each demo will take at most 15 minutes. ASt least one person from your group must attend the demo, but more is probably better to start up services.


Component 1)
In real life, requirements change, products are updated with new features and functionalty. 
This is now happening to the product you built for A1. 

Management liked all the features specified in A1, meaning they all must work for A2 as well.
Management also decided that it would be a good idea to track which user has bought which product. 

To achieve this, one could:
Update the Users microservice and the ISCS to track which user bought which products.
One could also:
Update products to keep track of which users bought how much of each product
One could also update both microservices. However, this would be a bad idea to duplicate information.
Ultimately, how you update this change is up to you.

Thus the only API change affects the OrderService.
On the OrderService there will be a new route /user/purchased/22222 
This should return a JSON of all items purchased by this user (i.e., user id 22222)
for example user id 22222 could have purchased items {2:3, 4:1} User 22222 purchased
3 units of product id 2 and purchased 1 unit of product id 4. 
This new route is added to the Order Service, we will query the order service only for this route!
==> This is the only "conceptual change" in terms of APIs we will test
- If the user id does not exist, return a 404 status code
- If the user has purchased nothing, return a 200 and empty json {} in the response body. 
- If the user exists and has purchased something, return 200 and a json as shown above.
All previous API calls will be tested as in A1; thus also directly testing User and Product just like A1.

Note: this must be submitted on March 5th as outlined above in the A2 component 1 MarkUs repo.

Component 2)
Further changes that you'll encounter in real life are due to "success". 
Since the inception of this product/application, it has been a huge success and you've been asked to prepare a roll out for increased customer numbers.
Your service now must be able to successfully and quickly service multiple customers simultaneously. (See below).
- For this, your API has to work correctly as well, we will use simple,straight-forward, well formed correct POST requests (i.e., all fields are present and correct), we will use "incorrect" post requests (i.e., fields are missing, and/or values of correct fields are incorrect (e.g., "placeeee oooorder" would be an incorrect value)), for get requests we will query routes that exist with data that should exist (or should not exist); and/or routes that do not exist....but no "tricky" edge cases, in the demo, this is about scalability and throughput.
- Persistence: will we issue a shutdown/restart command? Yes for Component 1, no for component 2. For component 2, we will check persistence manually, during the demo, I will ask you to connect to the DB via cli/GUI (whichever you prefer) and I will ask you to e.g., "Show me the data on user 2, does user 2 exist in your DB, there was a request to create user 2 half a minute ago, it should be persisted now. etc. 

Marking of Component 1 and Component 2
20% documentation and justification. (4 marks)
80% code functionality as below (24% from Component 1 API tests (4.8 marks); 56% from Component 2 demo (11.2 marks))


What we will test: 
Component 1:
- 4.8 marks: Same tests as for A1 + a new API test
Component 2:
- 0.2 marks: 2 requests handled successfully within 1 second
- 1 mark 5 requests handled successfully within 1 second
- 1 mark 15 requests handled successfully within 1 second 
- 1 mark 30 requests handled successfully within 1 second
- 1 mark 50 requests handled successfully within 1 second
- 1 mark 100 requests handled successfully within 1 second
- 1 mark 200 requests handled successfully within 1 second
- 1 mark 250 requests handled successfully whtin 1 second
- 1 mark 500 requests handled successfully whtin 1 second
- 1 mark 750 requests handled successfully whtin 1 second
- 1 mark 1000 requests handled successfully within 1 second
- 1 mark 2000 requests handled successfully within 1 second
1 bonus mark for the highest throughput regardless of actual number.

Logistics: 
- You can now work in groups of size 1-5. 
- Submit a PDF explaining your architecture AND justification using profiling results (tables, charts, tools used, changes that were implemented because of profiling results).
-- E.g., 
---profile your base system's throughput, memory usage, etc.; then optimize and profile again; optimize further; profile again, etc... each time collecting numbers
--- then put those numbers into tables/charts, explaining what you did and why it had this impact.
--- if an optimization results in worse performance, that should go into tables/charts as well.
--- explain the tables/charts.
- Book a demo with me, or TAs (details to follow). 
- During the demo:
-- you are asked to start up your services, we will send all payloads to a single IP, PORT which we will tell you during the demo (it is recommended to launch a "load balancer" at this IP/PORT). 
-- we will begin with the smallest workload (or upon request the largest workload you believe your service can handle), we will then run the test for 5-300 seconds. 
-- we will then continue the tests, after each test, we will ask you to query the DB(s) manually and verify that certain entries have been successfully stored. 
-- we will also send specific GET requests to the IP/PORT to check for both correct implementation of new features and data retention at high sustained workloads. 



