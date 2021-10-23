package daproxy.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

import daproxy.http.exceptions.IncompleteRequestException;
import daproxy.http.exceptions.InvalidRequestException;
import daproxy.log.LogUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * A valid CONNECT request is as follows:
 * CONNECT SP <url> SP HTTP/1.1 CRLF
 * where SP is space, and CRLF is \r\n
 * See: https://datatracker.ietf.org/doc/html/rfc2616/ [page 16]
 */
@Slf4j
public class ConnectRequest implements Request {


    private static final String CONNECT_TOKEN = "CONNECT ";
    private static final String HTTP_TOKEN = "HTTP/1.1";
    private static final byte CR = (byte)'\r';
    private static final byte LF = (byte)'\n';

    /**
     * Returns true if a valid HTTP/1.1 CONNECT request.
     * See https://httpwg.org/specs/rfc7231.html#CONNECT, https://datatracker.ietf.org/doc/html/rfc2817, and https://httpwg.org/specs/rfc7231.html#CONNECT
     * for more details.
     * This implementation does not support request pipelining, and will not evaluate any requests if pipelined.
     * Any subsequent data on the socket will be discarded.
     * @param request - a String object containing a sequence of characters.
     * @return true if the sequence contains a valid connect request
     * 
     */
    public static ConnectRequest parseConnectRequest(byte[] buf, int dataReceived) throws InvalidRequestException, IncompleteRequestException, MalformedURLException {
        log.debug("Parsing Connect Request\n" + new String(buf,0, dataReceived, StandardCharsets.US_ASCII));
        if (dataReceived < CONNECT_TOKEN.length()) {
            throw new IncompleteRequestException("method not finished");
        }

        byte[] connectToken = CONNECT_TOKEN.getBytes(StandardCharsets.US_ASCII);

        int i = 0;

        for( ; i < connectToken.length; ++i) {
           
            if (connectToken[i] != buf[i]) {
                throw new InvalidRequestException("Invalid Request.  Server only handles CONNECT Requests");
            }
        }
        log.debug("Found Connect Token i = " + i);
        
        //parse url
        boolean foundUrlSpace = false;
        for( ; i < dataReceived; ++i) {
            if(buf[i] == ' ') {
                foundUrlSpace = true;
                break;
            }
        }
        //43 4f 4e 4e 45 43 54 20 62 6c 6f 67 2e 64 69 67 69 74 61 6c 61 62 79 73 73 2e 63 61 3a 34 34 33 20 48 54 54 50 2f 31 2e 31 0d 0a 48 6f 73 74 3a 20 62 6c 6f 67 2e 64 69 67 69 74 61 6c 61 62 79 73 73 2e 63 61 3a 34 34 33 0d 0a 55 73 65 72 2d 41 67 65 6e 74 3a 20 63 75 72 6c 2f 37 2e 37 34 2e 30 0d 0a 50 72 6f 78 79 2d 43 6f 6e 6e 65 63 74 69 6f 6e 3a 20 4b 65 65 70 2d 41 6c 69 76 65 0d 0a 0d 0a 
        //43 4f 4e 4e 45 43 54 20 62 6c 6f 67 2e 64 69 67 69 74 61 6c 61 62 79 73 73 2e 63 61 3a 34 34 33 20 48 54 54 50 2f 31 2e 31 0d 0a 48 6f 73 74 3a 20 62 6c 6f 67 2e 64 69 67 69 74 61 6c 61 62 79 73 73 2e 63 61 3a 34 34 33 0d 0a 55 73 65 72 2d 41 67 65 6e 74 3a 20 63 75 72 6c 2f 37 2e 37 34 2e 30 0d 0a 50 72 6f 78 79 2d 43 6f 6e 6e 65 63 74 69 6f 6e 3a 20 4b 65 65 70 2d 41 6c 69 76 65 0d 0a 0d 0a 
        //0  1  2  3  4  5  6  7  8  9  10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35
        //CONNECT blog.digitalabyss.ca:443 HTTP/1.1
        //0123456789012345678901234567890123456789
                                        //32

        if(!foundUrlSpace) {
            throw new IncompleteRequestException("url not finished");
        }
        String url = new String(buf, CONNECT_TOKEN.length(), i - CONNECT_TOKEN.length(), StandardCharsets.US_ASCII);
        log.debug("Found URL = " + url + " i = " + i);
        log.debug("buf = " + LogUtils.bytesToHex(buf, 0, dataReceived));
        //validate Protocol
        byte[] httpToken = HTTP_TOKEN.getBytes(StandardCharsets.US_ASCII);
        i++;
        log.debug("httpToken = " + LogUtils.bytesToHex(httpToken, 0, httpToken.length));
        for(int j = 0 ; i < dataReceived && j < HTTP_TOKEN.length(); ++i, ++j) {
            log.debug("i = " + i + " j = " + j + " buf[i] = " + buf[i] + " httpToken[j] = " + httpToken[j]);
            if (httpToken[j] != buf[i]) {
                throw new InvalidRequestException("Invalid HTTP Protocol. Server only handles HTTP/1.1");
            }
        }

        log.debug("Validated Protocol");
        //find first CRLF
        for ( ; i < dataReceived ; ++i) {
            if (buf[i] != ' ' && buf[i] != CR && buf[i] != LF) {
                throw new InvalidRequestException("Invalid characters after Protocol.");
            }

            if (buf[i] == LF && buf[i-1] == CR) {
                break;
            }
        }
        log.debug("Found first CRLF at " + i);
        //if I have back-to-back CRLF's (can have spaces in between), then CONNECT request is finished
        //need to implement a backtracking algorithm to find back-to-back CRLF's
        int headersStart = i++;
        
        boolean allCharsAreSpaces = true;
        for( ; i < dataReceived ; ++i) {
            if (buf[i] != ' ' && buf[i] != LF && buf[i] != CR) {
                allCharsAreSpaces = false;
            }
            if (buf[i] == LF && buf[i-1] == CR) {
                if (allCharsAreSpaces) {
                    break;  //found end of request
                } else {
                    allCharsAreSpaces = true;
                }
            }
        }
        if ( i >= dataReceived ) {
            throw new IncompleteRequestException("Headers are incomplete");
        }
        log.debug("Found complete request i = " + i + " dataReceived = " + dataReceived);
        byte[] remainingBytes = new byte[0];
        if ( i + 1 < dataReceived) {
            remainingBytes = new byte[dataReceived - i];
            for (int j = 9 ; j < dataReceived - i ; j++) {
                remainingBytes[j] = buf[i+j];
            }
        }

        String headersBlob = new String(buf, headersStart, i, StandardCharsets.US_ASCII);
        return new ConnectRequest(url, headersBlob, remainingBytes);
    }



    private final String url;
    private final String headersBlob;
    private final byte[] firstBytesToWrite;

    public ConnectRequest(String url, String headersBlob, byte[] firstBytesToWrite) {
        this.url = url;
        this.headersBlob = headersBlob;
        this.firstBytesToWrite = firstBytesToWrite;
    }

    @Override
    public RequestMethod getMethod() {
        return RequestMethod.CONNECT;
    }



    private void writeInputToOutput(InputStream in, OutputStream out) throws IOException {
        int recvBytes;
        byte[] buf = new byte[1024];

        while ((recvBytes = in.read(buf)) != -1) {
            System.out.println("Thread: Writing " + recvBytes + " to socket");
            
            System.out.println("HEX Output: " + LogUtils.bytesToHex(buf, 0, recvBytes));
            out.write(buf, 0, recvBytes);
        }

        if (recvBytes == -1) { //socket is closed
            throw new SocketException("Detected closed socket");
        }

    }

    private String getHost(String url) {
        return url.split(":")[0];
    }
    private int getPort(String url) {
        return Integer.parseInt(url.split(":")[1]);
    }

    @Override
    public Response handle(Socket socket) {
        System.out.println("Handling a Connect Request");
        try {
            Socket downstreamSocket = new Socket(getHost(url), getPort(url)); // extractPort());
            // if this connects, we can give a 200 OK back to client, which will then allow
            // it to initate further packet transfers.
            // BufferedWriter br = new BufferedWriter(new
            // OutputStreamWriter(socket.getOutputStream()));
            Response resp = new Response(200);

            socket.getOutputStream().write(resp.toString().getBytes());

            System.out.println("writing " + resp.toString() + " to client socket");
            // br.flush();
            socket.getOutputStream().flush();
            // br.close();

            // read from client, write to server. read from remote, write to client. repeat.

            new Thread(() -> {
                while (!socket.isClosed() && !downstreamSocket.isClosed()) {
                    try {
                        Thread.sleep(500);
                        System.out.println("Thread: Socket (bound, connected, closed, in avail): (" + socket.isBound()
                                + ", " + socket.isConnected() + ", " + socket.isClosed() + ", "
                                + socket.getInputStream().available() + ")");
                        System.out.println("Thread: Downstream Socket (bound, connected, closed, in avail): ("
                                + downstreamSocket.isBound() + ", " + downstreamSocket.isConnected() + ", "
                                + downstreamSocket.isClosed() + ", " + downstreamSocket.getInputStream().available()
                                + ")");

                        writeInputToOutput(socket.getInputStream(), downstreamSocket.getOutputStream());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        try {
                            socket.close();
                        } catch (Exception ex2) {
                            ex.printStackTrace();
                        }
                        try {
                            downstreamSocket.close();
                        } catch (Exception ex2) {
                            ex.printStackTrace();
                        }
                    }
                } //TODO: Close all sockets.

            }).start();

            while (!socket.isClosed() && !downstreamSocket.isClosed()) { // ugh, this is not what you want: https://stackoverflow.com/questions/10240694/java-socket-api-how-to-tell-if-a-connection-has-been-closed/10241044
                try {
                    Thread.sleep(500);
                    System.out.println("Main: Socket (bound, connected, closed, in avail): (" + socket.isBound() + ", "
                            + socket.isConnected() + ", " + socket.isClosed() + ", "
                            + socket.getInputStream().available() + ")");
                    System.out.println("Main: Downstream Socket (bound, connected, closed, in avail): ("
                            + downstreamSocket.isBound() + ", " + downstreamSocket.isConnected() + ", "
                            + downstreamSocket.isClosed() + ", " + downstreamSocket.getInputStream().available() + ")");

                    writeInputToOutput(downstreamSocket.getInputStream(), socket.getOutputStream());

                } catch (Exception ex) {
                    ex.printStackTrace();
                    try {
                        socket.close();
                    } catch (Exception ex2) {
                        ex.printStackTrace();
                    }
                    try {
                        downstreamSocket.close();
                    } catch (Exception ex2) {
                        ex.printStackTrace();
                    }
                }

            } //TODO: Close all sockets.
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return Response.CONNECTION_ESTABLISHED();
    }

    public String getUrl() {
        return url;
    }

}

// I believe the HTTP Layer bytes should start with 16 03 01 02   00 01 00 01 fc 03 --> this happens for all the Client hello requests out of curl (in both scenarios)
// For mitmproxy, this is what gets forwarded to the downstream.  

// new Thread(() -> {
// while (true) {
// try {
// Thread.sleep(500);
// System.out.println("Thread: Socket (bound, connected, closed, in avail): (" +
// socket.isBound() + ", "
// + socket.isConnected() + ", " + socket.isClosed() + ", " +
// socket.getInputStream().available() + ")");
// System.out.println(
// "Thread: Downstream Socket (bound, connected, closed, in avail) (" +
// downstreamSocket.isBound() + ", "
// + downstreamSocket.isConnected() + ", " + downstreamSocket.isClosed() + ", "
// + downstreamSocket.getInputStream().available() + ")");

// int recvBytes;
// byte[] downInData = new byte[1024];

// InputStream in = downstreamSocket.getInputStream();
// OutputStream out = socket.getOutputStream();
// while( (recvBytes = in.read(downInData)) != -1 ) {
// System.out.println("Thread: Writing " + recvBytes + " to socket");
// String str = new String(downInData, "UTF-8");
// System.out.println("socket writing: " + str);
// out.write(downInData, 0, recvBytes);
// }
// //socket.getInputStream().transferTo(downstreamSocket.getOutputStream());

// } catch (IOException | InterruptedException e) {
// // TODO Auto-generated catch block
// e.printStackTrace();

// }
// }
// }).start(); // connect output from client and writes to downstream

// while (true) {
// try {
// Thread.sleep(500);
// System.out.println("Main: Socket (bound, connected, closed, in avail): (" +
// socket.isBound() + ", "
// + socket.isConnected() + ", " + socket.isClosed() + ", " +
// socket.getInputStream().available() + ")");
// System.out.println(
// "Main: Downstream Socket (bound, connected, closed, in avail): (" +
// downstreamSocket.isBound() + ", "
// + downstreamSocket.isConnected() + ", " + downstreamSocket.isClosed() + ", "
// + downstreamSocket.getInputStream().available() + ")");

// int recvBytes;
// byte[] downInData = new byte[1024];

// InputStream in = socket.getInputStream();
// OutputStream out = downstreamSocket.getOutputStream();
// while( (recvBytes = in.read(downInData)) != -1 ) {
// System.out.println("Main: Writing " + recvBytes + " to downstream");
// String str = new String(downInData, "UTF-8");
// System.out.println("Downstream writing: " + str);
// out.write(downInData, 0, recvBytes);
// }

// // downstreamSocket.getInputStream().transferTo(socket.getOutputStream()); //
// connect output from
// // downstream and writes to
// // client

// } catch (IOException | InterruptedException e) {
// // TODO Auto-generated catch block
// e.printStackTrace();
// }
// }

// } catch (IndexOutOfBoundsException ex) {
// ex.printStackTrace();
// } catch (IOException ex) {
// ex.printStackTrace();
// }