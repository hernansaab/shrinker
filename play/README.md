# starting this app

start redis from docker docker-compose.yml file in /docker directory
```
cd docker
docker-compose up -d
```
This will start a redis instance on **localhost** (if you are not running docker machines) and port 5268.

This address matches configuration in **/play/conf/application.conf** so if you have your own redis feel free to change that settings at the bottom of this file.

Now you can start play
```
cd play
sbt run -Dhttp.port=8080
```
This command will start a server running play listening on port 8080. **If you don't pass the http.port command 
this application will not run well.**



## How to use this application

It should be simple. Go to **http://localhost:8080** and enter an address.


## On the code....

This application was coded on top of play 2.6 on scala.
There are three controllers under **/app/controllers**

* **controllers.ErrorController** is simple and just displays an error page when tiny address does not exist.
* **controllers.HomeController** is the page that renders the main page and calculates the tiny address.
* **controllers.RouterAddress** is the page that routes a tiny url to the actual external address.

There are also 3 views corresponding to each of the controllers under **/app/views**

For the service and the model we use dependency injection. 

The key service interface (trait) is called Shriner. Its implementations are called **services.ShrinkerImplementation.** 

They nosql model interface (trait) is called **db.nosql.NoSQL** and its implementation is called db.nosql.RedisNoSQL

The binding of interface to classes for Shrinker and NoSQL is done in file **/app/Module**


## How I address thread safety issues
The important code is in method services.ShrinkerImplementation.getOrGenerateTiny
```
        val number = incrementor.get() * getThreadPoolSize() + getCurrentThreadPosition()
```

value "numericKey" is basically the key number before it gets encoded. This simple equation guarantees that each thread 
in play generates a unique key that cannot possible be stomped by another thread in play.

Also I increment a unique index atomically using the synchronize method.



## The costs of thread safety
I found that my play server was using 27 different threads in the thread pool. Since each thread is guaranteed a unique key number to write, in the case of 27 threads it means we lose about
6 bits of data (about half the information in one character only). This is not too terrible because the increment number is a long 64 bits and as the number grows the impact is negligible.


The other cost is that increment is done atomically by function **incrementAndSaveCounter()**. In this application, this is a real bottleneck 
because for each increment, the index is updated to redis.

However, we can address this issue by saving the increment value only 100 increments at a time, for example. 
For this setup to guarantee to work, we must add 100 to increment everytime we start the server to eliminate any possibility or overriding a previously generated tiny.

I did not implement this simple solution because performance optimizations may make code a bit harder to understand but I would be more than glad to talk about it.

## performance based on redis requests
Using the increment key means I don't have to check if a key has been used which is a good thing 
1. A generation of a tiny that does not exist requires 2 redis transactions and 1 increment update to redis = 3 transactions
1. A retrieval of a tiny requires transaction and 1 increment update to redis = 2 transactions
1. A retrieval of a tiny to tiny requires =  1 transaction  only.

However, as mentioned in the section above we can reduce the cost of increments to a fraction of the cost. Making (1) and (2) cost 2.01 and 1.01 transactions respectively if increment key gets saved 100 transactions at a time.

## Test cases
**services.ShrinkerImplementation** is tested with 90%+ coverage with exception of methods that require play to work and may not produce consistent 
results every time a test case is run, namely  **getCurrentThreadPosition and getThreadPoolSize.**


**db.nosql.RedisNOSQL** was tested 100% and it is an example that shows usage of mockito.


A factored out method in **HomeController** was tested namely **HomeController.renderData**. 

However, I haven't figure out how to test the action of a controller yet for all actions. This is why I kept actions thin.
  
 
Normally, I would only test the ShrinkerImplementation as the controllers and redisNoSQL have barely any logic in them,  but I wanted to prove how much coverage I can produce.


## Limitations
This application generates text encoded keys from a long key. THis means there are about 2^55 number of keys (substract the 6 bits to guarantee thread safety).
That amounts to about **few quatrillion addresses**,  I believe.

Also,  we need to think a bit harder if we want an implementation that may need separate servers running behind a load balancer instead of a monolitic application like this one.


## Full Disclosure On Time Budget

There is no way I can have an application done in 4 or 5 hours with 95%+ test case coverage and documentation included.
I could have done this much faster with spring boot since this is what I am currently using, but since I am applying as a scala developer I decided to go with play.

* There was a cost on setting up play with intellij the first time. I found a few integration issues. **Cost 1 hour**.
* Cost of writing code with no test cases with latest play V2.6.X (about 250 lines of code plus comments plus learning the latest play 2.6.X changes since I last used it): **about 5 hours**.
* Cost of writing test cases with 90%+ coverage (about 300 lines of code): **about 2 hours**
* Cost of writing documentation: **about 1.5 hours**
