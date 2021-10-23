package daproxy.http.request;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

import daproxy.http.RequestMethod;
import daproxy.http.Response;
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