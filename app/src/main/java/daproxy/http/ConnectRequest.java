package daproxy.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import daproxy.log.LogUtils;

/**
 * A valid CONNECT request is as follows:
 * CONNECT SP <url> SP HTTP/1.1 CRLF
 * where SP is space, and CRLF is \r\n
 * See: https://datatracker.ietf.org/doc/html/rfc2616/ [page 16]
 */
public class ConnectRequest implements Request {


    private static final String CONNECT_TOKEN = "CONNECT ";
    private static final String HTTP_TOKEN = "HTTP/1.1";
    private static final byte CR = (byte)'\r';
    private static final byte LF = (bute)'\n';

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
    public static ConnectRequest parseConnectRequest(byte[] buf, int dataReceived) throws InvalidRequestException, IncompleteRequestException {

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
        
        //parse url
        boolean foundUrlSpace = false;
        for( ; i < dataReceived; ++i) {
            if(buf[i] == ' ') {
                foundUrlSpace = true;
                break;
            }
        }
        if(!foundUrlSpace) {
            throw new IncompleteRequestException("url not finished");
        }
        String url = new String(buf, CONNECT_TOKEN.length(), i, StandardCharsets.US_ASCII);

        //validate Protocol
        byte[] httpToken = HTTP_TOKEN.getBytes(StandardCharsets.US_ASCII);
        for(int j = 0 ; i < dataReceived && j < HTTP_TOKEN.length(); ++i, ++j) {
            if (httpToken[j] != buf[i]) {
                throw new InvalidRequestException("Invalid HTTP Protocol. Server only handles HTTP/1.1");
            }
        }

        //find first CRLF
        for ( ; i < dataReceived ; ++i) {
            if (buf[i] != ' ' && buf[i] != CR && buf[i] != LF) {
                throw new InvalidRequestException("Invalid characters after Protocol.");
            }

            if (buf[i] == LF && buf[i-1] == CR) {
                break;
            }
        }

        //if I have back-to-back CRLF's (can have spaces in between), then CONNECT request is finished
        //need to implement a backtracking algorithm to find back-to-back CRLF's
        i--;
        boolean isSecondCRLF = false;
        while( i < dataReceived && buf[i] == ' ') {
            i++;
        }

        //TODO: parse other data.  For now, stick it in to a buffer.

    }




    private final String connectString;

    public ConnectRequest(String firstLine) {
        connectString = firstLine;
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

    }

    @Override
    public Response handle(Socket socket) {
        System.out.println("Handling a Connect Request");
        try {
            Socket downstreamSocket = new Socket(extractUrl(), 443); // extractPort());
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
                while (true) {
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
                    }
                }

            }).start();

            while (true) {
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
                }

            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return Response.OK();
    }

    public String extractUrl() {
        return connectString.split(" ")[1].split(":")[0];
        // return connectString.split("/")[2];
    }

    public int extractPort() {
        String[] portString = connectString.split(" ")[1].split(":");
        if (portString.length >= 1) {
            return Integer.parseInt(portString[1]);
        }
        // TODO -- is this correct?
        return 80;
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