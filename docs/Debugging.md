
# Debugging TLS handshakes

One way to debug is to use [wireshark](https://www.wireshark.org/)


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
      * `socket.read()` will return -1 if the other side closes the connection. (other methods will return either Null, or possibly throw an exception, or hang indefinitely)
        * `socket.isClosed()` only returns true if you closed it.
        * `socket.isConnected()` returns true if at any point you had a connection.