# DAProxy - A privacy preserving HTTP Proxy

DAProxy is an HTTP Proxy server that responds to HTTP Connect requests and creates an HTTP tunnel between client and server, allowing for TLS through the tunneled connection.

DAProxy has basic configuration to allow individuals to configure an allow-list of downstream endpoints.

## Prerequisites 

* [Gradle 7.2](https://gradle.org/)
* Java 11 - consider using [sdkman](https://sdkman.io/) to install/manage your Java version.

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








# Architecture / Application Design

* This has been a fun assignment.  While seemingly simple question, there are many caveats and corner cases to implementing this "correctly".  Here are some design trade-offs I've made to try and implement within a reasonable timeframe.
        * Only CONNECT requests will be served.  All other requests will get a 400 ERROR response code.
        * The server will examine the incoming request looking for it to be compliant with https://httpwg.org/specs/rfc7231.html#CONNECT
        * The server will not support proxy authorization
        * The server is currently vulnerable to a DOS attack where clients send keep-alive headers in https and the target server keeps connections alive

* I've gone with a classic blocking IO design (and then benchmark to see how scalable it is). There does seem to be some contention in the community which is "more scalable":  While older, these authors have insights into server design [HTTP Server Design NIO v IO](http://beefchunk.com/documentation/network/programming/tymaPaulMultithreaded.pdf) and [C10K Problem](http://www.kegel.com/c10k.html#top) that do seem relevant still in the present.


# Resources

* [Detecting Socket Disconnect](https://stackoverflow.com/questions/12243765/java-handling-socket-disconnection/12244232#12244232)
* [HTTP Connect on MDM](https://developer.mozilla.org/en-US/docs/Web/HTTP/Methods/CONNECT)

* [HTTP Server Design NIO v IO](http://beefchunk.com/documentation/network/programming/tymaPaulMultithreaded.pdf)
* [C10K Problem](http://www.kegel.com/c10k.html#top)