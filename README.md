# DAProxy - A privacy preserving HTTP Proxy

DAProxy is an HTTP Proxy server that responds to HTTP Connect requests and creates an HTTP tunnel between client and server, allowing for TLS through the tunneled connection.

DAProxy has basic configuration to allow individuals to configure an allow-list of downstream endpoints.

## Prerequisites 

* [Gradle 7.2](https://gradle.org/)
* Java 11 - This was developed using [sdkman](https://sdkman.io/) to install/manage your Java version.

## Building

`gradle clean build` will generate a 'fat jar' in the `app/build/libs/` folder.

## Running

`java -jar app/build/libs/app-all.jar -config <path-to-config>` will start the server.

## Sample Configuration

The allow list is defined in a yaml file and specified when running the application. 
Here is a sample:

```yaml
listenPort: 8085
allowList:
  - blog.digitalabyss.ca
```

Note: 
* There are many outstanding "TODOs" in regards to configuration.  Currently, many parameters that would traditionally be configurable are hard coded as constants in their respective class.
* Similarly, logging configuration is specified seperately in `app/main/resources/logback.xml` and should eventually be moved to the configuration file.


## Using DAProxy
### Example: curl
```
curl -v <your https url> --proxy http:/127.0.0.1:8085
```
This will instruct curl to issue an HTTP CONNECT request.
### Example: wget
1. edit `/etc/wgetrc` to include `https_proxy` and `http_proxy` to point to your local proxy.
2. Use wget as normal

```
wget https://blog.digitalabyss.ca
```


## Benchmarking

* For Full Details see [Benchmarking](docs/Benchmarking.md)
* Benchmarking was done using [hey](https://github.com/rakyll/hey) for generating load.

### Results

* 200 requests with 50 concurrent requests running locally (making these not so scientific ;) ):

| run | average response time (seconds) | throughput (req/sec) |
| --- | ------------------------------ | -------------------- |
| no proxy | 0.1213 | 374.6539 |
| mitmproxy | 0.2695 | 178.4888 |
| DAProxy | 0.3819 | 116.4780 |

The results indicate that proxies, in general, may have a significant relative performance impact compared to no proxies.  The DAProxy implementation has some significant room for growth to be able to match exisiting 'out of the box' proxies, but for a completely untuned  first attempt (including JVM and GC configuration options), it could be worse :)

The service was not tested at saturation (when request volume exceeds the server's ability to handle), so it is not possible to comment on how gracefully performance degrades.  This is a future TODO.

# Testing

The gradle build runs unit tests and generates a code coverage report in `/app/build/reports/jacoco/test/html`

For correctness, a combination of unit tests and end to end tests (eg. curl and hey) were used to verify accuracy.  Given the time constrained nature of the request, functional code was favoured over code coverage at the unit test level for areas of the application that would be tested by end to end tests.  To improve test coverage, some areas would require a bit of refactoring to be able to inject mocks (vs usage of 'new' in a few areas). Other forms of testing (e.g. security/vulnerability scanning) were also left as a 'TODO'.  I would also normally consider writing some integration tests that would use a misbehaving downstream (e.g. one that holds on to socket connections, introduces lag, unexpectedly hangs up, etc.) to test some of the code that is harder to test at the unit level, as well as integrate a monitoring tool to allow for production monitoring and observability.


# Architecture / Application Design

* This has been a fun assignment.  Though a seemingly simple question, the restrictions placed on the design (e.g. handle threading and http connections without resorting to ane xisting library) means that there were many caveats and corner cases to implementing this "correctly".  Here are some design trade-offs I've made to try and implement within a reasonable timeframe.
        * Only CONNECT requests are served.  All other requests will get a 400 ERROR response code.
        * The server will examine the incoming request looking for it to be compliant with https://httpwg.org/specs/rfc7231.html#CONNECT
        * The server will does not support proxy authorization
        * The server does not support HTTP Pipelining.
        * Most configuration is hard coded as static final (and should be extracted into the configuration)
        

* I've gone with a classic blocking IO design versus using NIO. There does seem to be some contention in the Java community which is more scalable:  While older, these authors share insights into server design [HTTP Server Design NIO v IO](http://beefchunk.com/documentation/network/programming/tymaPaulMultithreaded.pdf) and [C10K Problem](http://www.kegel.com/c10k.html#top) that still seem relevant today.  It would be fun to write a competing NIO implementation to compare/contrast the performance characteristics of each approach.


# Resources

* [Detecting Socket Disconnect](https://stackoverflow.com/questions/12243765/java-handling-socket-disconnection/12244232#12244232)
* [HTTP Connect on MDM](https://developer.mozilla.org/en-US/docs/Web/HTTP/Methods/CONNECT)

* [HTTP Server Design NIO v IO](http://beefchunk.com/documentation/network/programming/tymaPaulMultithreaded.pdf)
* [C10K Problem](http://www.kegel.com/c10k.html#top)