package daproxy.http;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

public class ConnectRequest implements Request {

    public static final String MATCHER = ".*"; // "CONNECT [-a-zA-Z0-9+&@#/%?=~_|!:,.;]* HTTP/1\\.1$";

    private final String connectString;

    public ConnectRequest(String firstLine) {
        connectString = firstLine;
    }

    @Override
    public RequestMethod getMethod() {
        return RequestMethod.CONNECT;
    }

    private String hex(byte[] bytes, int start, int finish) {
        StringBuilder result = new StringBuilder();
        for (int i = start; i < finish; i++) {
            byte aByte = bytes[i];
            result.append(String.format("%02x ", aByte));
            // upper case
            // result.append(String.format("%02X", aByte));
        }
        return result.toString();
    }

    private void writeInputToOutput(InputStream in, OutputStream out) throws IOException {
        int recvBytes;
        byte[] buf = new byte[1024];

        while ((recvBytes = in.read(buf)) != -1) {
            System.out.println("Thread: Writing " + recvBytes + " to socket");
            
            System.out.println("HEX Output: " + hex(buf, 0, recvBytes));
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