package daproxy.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import daproxy.log.LogUtils;
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

            Request request = waitForConnect();

            request.handle(socket);
            // respondOK()

        } catch (InvalidRequestException ex) {
            log.error("Invalid Request Received", ex);
            try {
                socket.getOutputStream().write(Response.BAD_REQUEST().toString().getBytes(StandardCharsets.US_ASCII));
            } catch (IOException ioEX) {
                log.error("Error writing BAD Request to client", ioEX);

            }
        } catch (IOException ex) { // AN IOException will occur if the read request is blocking and the socket is
            // closed by the thread pool.
            log.error("Client Socket " + remoteAddr + " closed due to inactivity", ex);
        } finally {
            try {
                socket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public ConnectRequest waitForConnect() throws IOException, InvalidRequestException {
        InputStream in = socket.getInputStream();
        boolean validConnectRequest = false;

        ByteBuffer buff = ByteBuffer.wrap(new byte[REQUEST_BUFFER_SIZE]);
        while (!validConnectRequest) {
            byte[] tmp = new byte[REQUEST_BUFFER_SIZE];
            int numBytes = in.read(tmp); // whenever I try to use readAllBytes, it just hangs indefintely.
            buff.put(tmp, 0, numBytes);

            log.debug("Received new bytes: " + LogUtils.bytesToHex(tmp, 0, numBytes));
            log.debug("Request Buffer: " + LogUtils.bytesToHex(buff.array(), 0, buff.position()));

            try {
                return ConnectRequest.parseConnectRequest(buff.array(), buff.position());// firstRequest.toString());
            } catch (IncompleteRequestException ex) {
                log.debug("Partially formed request. Waiting for more");
            }

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

        return null;
    }

}
