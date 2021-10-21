package daproxy.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RequestHandler {

    private static final int REQUEST_BUFFER_SIZE = 8092;

    private final Socket socket;

    public RequestHandler(Socket socket) {
        this.socket = socket;
    }

    public void handle() {
        SocketAddress remoteAddr = socket.getRemoteSocketAddress();
        SocketAddress localAddr = socket.getLocalSocketAddress();
        try {
            log.debug("accepted connection from " + remoteAddr + " with connection " + localAddr);

            ConnectRequest request = waitForConnect();

            // respondOK()

        } catch (IOException ex) { // AN IOException will occur if the read request is blocking and the socket is
                                   // closed by the thread pool.
            log.error("Client Socket " + remoteAddr + " closed due to inactivity");
        } finally {
            try {
                socket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public ConnectRequest waitForConnect() throws IOException {
        InputStream in = socket.getInputStream();
        boolean validConnectRequest = false;

        ByteBuffer buff = ByteBuffer.wrap(new byte[REQUEST_BUFFER_SIZE]);      
        while (!validConnectRequest) {
            byte[] tmp = in.readAllBytes();
            buff.put(tmp);
            

           // firstRequest.append(new String(buf, 0, recvBytes, "UTF-8"));
            validConnectRequest = ConnectRequest.isConnectRequest(buff.array(), buff.position()) ;//firstRequest.toString());
            // System.out.println("HEX Output: " + hex(buf, 0, recvBytes));
            // System.out.println("Processing Start of Request: reading " + recvBytes );
        }
        // System.out.println("Text = " + firstRequest.toString());
        // System.out.println("Response = " + response);

        // RFC2817 [Page 7]: Like any other pipelined HTTP/1.1 request, data to be
        // tunneled may be
        // sent immediately after the blank line. The usual caveats also apply:
        // data may be discarded if the eventual response is negative, and the
        // connection may be reset with no response if more than one TCP segment
        // is outstanding.
        // ==> you cannot discard any data after on the pipe, and should be forwarded to
        // the target proxy host.

        return new ConnectRequest(firstRequest.toString());
    }

}
