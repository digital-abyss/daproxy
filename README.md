# Requirements


## Sample Requests
### Using curl
```
curl -v https://blog.digitalabyss.ca --proxy http:/127.0.0.1:8086
```
This will instruct curl to issue an HTTP CONNECT request.
### Using wget
1. edit `/etc/wgetrc` to include `https_proxy` and `http_proxy` to point to your local proxy.
2. Use wget as normal

```
wget https://blog.digitalabyss.ca
```

## Debugging TLS handshakes

One way to debug is to use [wireshark](https://www.wireshark.org/)

# Exploring existing solutions

One approach is to use an existing proxy solution.  For example [mitmproxy](https://mitmproxy.org/) can be configured to operate as a proxy.

1. Download/Install mitmproxy.
2. Start the proxy, with configuration to not "man in the middle" https traffic. This seems to have worked for me:
```
./mitmproxy --listen-port 8086 --tcp ".*" --ignore ".*"
```
3.  Send a proxy request as described in ##SampleRequests

# Java Socket programming

* Sockets are a form of *blocking* I/O, so to be able to handle many users, you have to multi-thread.
* You can also use the java NIO libraries, which are non-blocking.
## Sockets

A socket is an abstraction on top of a TCP connection.  While you don't need to know about handling TCP, you do need to know about the lifecycle, and when a socket can throw an exception.

### Socket Lifecycle for a socket spawned from a ServerSocket

* open `Socket aSocket = sSocket.accept();` --> at this point a TCP connection is established between the client and the server.
* sending/receiving data:
    `aSocket.getInputStream().read()` --> you get input from the client here.  (Read Data)
    `aSocket.getOutputStream().write()` --> you send output to the client here. (Write Data)
    * Note: all the read methods BLOCK. This means if there you've read all the data and call it again, you will wait forever for new data.
        * The client may not be sending new data.
        * For http1.1 and http2, the client may be sending multiple requests on the same TCP socket
        * A request may be split across multiple TCP packets, and therefore may be split across multiple `read()` requests

* closing:
    * A client may close the socket almost at any time.
        * For example: you send a response to the client, and it's decided that it's done (won't be sending more requests)
    * Or a client may hold the socket open for multiple requests.

    * A server may close the socket after an idle timeout period.  It should send "Connection: close" header to the client
        * See this good summary of the spec: https://www.jmarshall.com/easy/http/#http1.1s4

    * What is in the HTTP1.1 spec?
        * Pipelining: a feature where multiple requests are sent before you can get a response.  Only certain requests can be pipelined. (I don't think Connect is one of them)
        * Persistent Connection:  The TCP connection is re-used.  So you still have full request/response required before a second request is sent on the same connection, and this is managed by Connection header being set to keep-alive  and the Keep-Alive header.
        
        * Given that the traffic is actually HTTPS, there is *no* way to know when the server or client decide to end the connection.
            * So you will only be able to tell when the socket is dead.

    * In concrete terms:
      * `socket.read()` will return -1 if the other side closes the connection. (other methods will return either Null, or possibly throw an exception)
        * `socket.isClosed()` only returns true if you closed it.
        * `socket.isConnected()` returns true if at any point you had a connection.

# Thoughts on high level implementation algorithm

* This has been a fun assignment.  While seemingly simple question, there are many caveats and corner cases to implementing this "correctly".  Here are some design trade-offs I've made to try and implement within a reasonable timeframe.
        * Only CONNECT requests will be served.  All other requests will get a 400 ERROR response code.
        * The server will examine the incoming request looking for it to be compliant with https://httpwg.org/specs/rfc7231.html#CONNECT
        * The server will not support proxy authorization
        * The server is currently vulnerable to a DOS attack where clients send keep-alive headers in https and the target server keeps connections alive

* I've gone with a classic blocking IO design (and then benchmark to see how scalable it is). There does seem to be some contention in the community which is "more scalable":  While older, these authors have insights into server design [HTTP Server Design NIO v IO](http://beefchunk.com/documentation/network/programming/tymaPaulMultithreaded.pdf) and [C10K Problem](http://www.kegel.com/c10k.html#top) that do seem relevant still in the present.



# Benchmarking

## No Proxy

$ ./hey_linux_amd64 -disable-keepalive -m GET -n 200 -c 50 https://blog.digitalabyss.ca

Summary:
  Total:	0.5338 secs
  Slowest:	0.2015 secs
  Fastest:	0.0912 secs
  Average:	0.1213 secs
  Requests/sec:	374.6539
  

Response time histogram:
  0.091 [1]	|■
  0.102 [66]	|■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■
  0.113 [37]	|■■■■■■■■■■■■■■■■■■■■■■
  0.124 [12]	|■■■■■■■
  0.135 [33]	|■■■■■■■■■■■■■■■■■■■■
  0.146 [23]	|■■■■■■■■■■■■■■
  0.157 [5]	|■■■
  0.168 [6]	|■■■■
  0.179 [7]	|■■■■
  0.191 [1]	|■
  0.202 [9]	|■■■■■


Latency distribution:
  10% in 0.0952 secs
  25% in 0.0996 secs
  50% in 0.1106 secs
  75% in 0.1357 secs
  90% in 0.1648 secs
  95% in 0.1839 secs
  99% in 0.2010 secs

Details (average, fastest, slowest):
  DNS+dialup:	0.0969 secs, 0.0912 secs, 0.2015 secs
  DNS-lookup:	0.0068 secs, 0.0001 secs, 0.0226 secs
  req write:	0.0001 secs, 0.0000 secs, 0.0014 secs
  resp wait:	0.0238 secs, 0.0213 secs, 0.0322 secs
  resp read:	0.0004 secs, 0.0001 secs, 0.0028 secs

Status code distribution:
  [200]	200 responses

## Initial (Single Threaded Request Handler) Implementation


:~/src/bin$ ./hey_linux_amd64 -disable-keepalive -m GET -x http://127.0.0.1:8085 -n 200 -c 50 https://blog.digitalabyss.ca

Summary:
  Total:	19.0243 secs
  Slowest:	4.8438 secs
  Fastest:	0.1231 secs
  Average:	4.1719 secs
  Requests/sec:	10.5129
  

Response time histogram:
  0.123 [1]	|
  0.595 [4]	|■
  1.067 [5]	|■
  1.539 [5]	|■
  2.011 [5]	|■
  2.483 [5]	|■
  2.956 [4]	|■
  3.428 [5]	|■
  3.900 [5]	|■
  4.372 [5]	|■
  4.844 [156]	|■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■


Latency distribution:
  10% in 2.0731 secs
  25% in 4.6790 secs
  50% in 4.7136 secs
  75% in 4.7357 secs
  90% in 4.7680 secs
  95% in 4.7902 secs
  99% in 4.8132 secs

Details (average, fastest, slowest):
  DNS+dialup:	4.1496 secs, 0.1231 secs, 4.8438 secs
  DNS-lookup:	0.0000 secs, 0.0000 secs, 0.0000 secs
  req write:	0.0001 secs, 0.0000 secs, 0.0002 secs
  resp wait:	0.0220 secs, 0.0214 secs, 0.0237 secs
  resp read:	0.0002 secs, 0.0001 secs, 0.0012 secs

Status code distribution:
  [200]	200 responses


## Single Threaded, Log level turned to warn (instead of debug)

:~/src/bin$ ./hey_linux_amd64 -disable-keepalive -m GET -x http://127.0.0.1:8085 -n 200 -c 50 https://blog.digitalabyss.ca

Summary:
  Total:	19.1573 secs
  Slowest:	5.0552 secs
  Fastest:	0.3788 secs
  Average:	4.2139 secs
  Requests/sec:	10.4399
  

Response time histogram:
  0.379 [1]	|
  0.846 [4]	|■
  1.314 [5]	|■
  1.782 [5]	|■
  2.249 [5]	|■
  2.717 [5]	|■
  3.185 [5]	|■
  3.652 [5]	|■
  4.120 [5]	|■
  4.588 [5]	|■
  5.055 [155]	|■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■


Latency distribution:
  10% in 2.3182 secs
  25% in 4.6680 secs
  50% in 4.6980 secs
  75% in 4.7154 secs
  90% in 4.7340 secs
  95% in 4.7367 secs
  99% in 4.9607 secs

Details (average, fastest, slowest):
  DNS+dialup:	4.1914 secs, 0.3788 secs, 5.0552 secs
  DNS-lookup:	0.0000 secs, 0.0000 secs, 0.0000 secs
  req write:	0.0001 secs, 0.0000 secs, 0.0007 secs
  resp wait:	0.0221 secs, 0.0215 secs, 0.0281 secs
  resp read:	0.0003 secs, 0.0001 secs, 0.0017 secs

Status code distribution:
  [200]	200 responses

# With a ThreadPoolExecutor

$ ./hey_linux_amd64 -disable-keepalive -m GET -x http://127.0.0.1:8085 -n 200 -c 50 https://blog.digitalabyss.ca

Summary:
  Total:	2.6430 secs
  Slowest:	1.4259 secs
  Fastest:	0.0918 secs
  Average:	0.4710 secs
  Requests/sec:	75.6728
  

Response time histogram:
  0.092 [1]	|
  0.225 [0]	|
  0.359 [12]	|■■■■
  0.492 [130]	|■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■
  0.625 [30]	|■■■■■■■■■
  0.759 [23]	|■■■■■■■
  0.892 [2]	|■
  1.026 [0]	|
  1.159 [0]	|
  1.293 [0]	|
  1.426 [2]	|■


Latency distribution:
  10% in 0.3761 secs
  25% in 0.3887 secs
  50% in 0.4215 secs
  75% in 0.5022 secs
  90% in 0.6495 secs
  95% in 0.7380 secs
  99% in 1.3884 secs

Details (average, fastest, slowest):
  DNS+dialup:	0.4483 secs, 0.0918 secs, 1.4259 secs
  DNS-lookup:	0.0000 secs, 0.0000 secs, 0.0000 secs
  req write:	0.0001 secs, 0.0000 secs, 0.0015 secs
  resp wait:	0.0223 secs, 0.0212 secs, 0.0317 secs
  resp read:	0.0003 secs, 0.0000 secs, 0.0046 secs

Status code distribution:
  [200]	200 responses


## mitmproxy performance

$ ./hey_linux_amd64 -disable-keepalive -m GET -x http://127.0.0.1:8086 -n 200 -c 50 https://blog.digitalabyss.ca

Summary:
  Total:	1.1205 secs
  Slowest:	0.4541 secs
  Fastest:	0.2022 secs
  Average:	0.2695 secs
  Requests/sec:	178.4888
  

Response time histogram:
  0.202 [1]	|■
  0.227 [42]	|■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■
  0.253 [35]	|■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■
  0.278 [44]	|■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■
  0.303 [24]	|■■■■■■■■■■■■■■■■■■■■■■
  0.328 [38]	|■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■
  0.353 [13]	|■■■■■■■■■■■■
  0.379 [1]	|■
  0.404 [1]	|■
  0.429 [0]	|
  0.454 [1]	|■


Latency distribution:
  10% in 0.2174 secs
  25% in 0.2342 secs
  50% in 0.2600 secs
  75% in 0.3088 secs
  90% in 0.3238 secs
  95% in 0.3396 secs
  99% in 0.3808 secs

Details (average, fastest, slowest):
  DNS+dialup:	0.2319 secs, 0.2022 secs, 0.4541 secs
  DNS-lookup:	0.0000 secs, 0.0000 secs, 0.0000 secs
  req write:	0.0000 secs, 0.0000 secs, 0.0003 secs
  resp wait:	0.0375 secs, 0.0216 secs, 0.0530 secs
  resp read:	0.0001 secs, 0.0000 secs, 0.0007 secs

Status code distribution:
  [200]	200 responses


# Resources

* [Detecting Socket Disconnect](https://stackoverflow.com/questions/12243765/java-handling-socket-disconnection/12244232#12244232)
* [HTTP Connect on MDM](https://developer.mozilla.org/en-US/docs/Web/HTTP/Methods/CONNECT)

* [HTTP Server Design NIO v IO](http://beefchunk.com/documentation/network/programming/tymaPaulMultithreaded.pdf)
* [C10K Problem](http://www.kegel.com/c10k.html#top)