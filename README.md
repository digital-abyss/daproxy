# Requirements


## Sample Requests

### Using cUrl

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

* A socket is an abstraction on top of a TCP connection.  While you don't need to know about handling TCP, you do need to know:
    *

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

# Thoughts on high level implementation algorithm

* This has been a fun assignment.  While seemingly simple question, there are many caveats and corner cases to implementing this "correctly".  Here are some design trade-offs I've made to try and implement within a reasonable timeframe.
        * Only CONNECT requests will be served.  All other requests will get a 400 ERROR response code.
        * The server will examine the incoming request looking for it to be compliant with https://httpwg.org/specs/rfc7231.html#CONNECT
        * The server will not support proxy authorization
        * The server is currently vulnerable to a DOS attack where clients send keep-alive headers in https and the target server keeps connections alive

* I've gone with a classic blocking IO design (and then benchmark to see how scalable it is). There does seem to be some contention in the community which is "more scalable":  While older, these authors have insights into server design [HTTP Server Design NIO v IO](http://beefchunk.com/documentation/network/programming/tymaPaulMultithreaded.pdf) and [C10K Problem](http://www.kegel.com/c10k.html#top) that do seem relevant still in the present.






# Resources

* [HTTP Connect on MDM](https://developer.mozilla.org/en-US/docs/Web/HTTP/Methods/CONNECT)

* [HTTP Server Design NIO v IO](http://beefchunk.com/documentation/network/programming/tymaPaulMultithreaded.pdf)
* [C10K Problem](http://www.kegel.com/c10k.html#top)