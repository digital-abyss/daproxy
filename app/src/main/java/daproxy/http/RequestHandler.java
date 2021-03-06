package daproxy.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import daproxy.http.exceptions.IncompleteRequestException;
import daproxy.http.exceptions.InvalidRequestException;
import daproxy.http.exceptions.NotYetImplementedException;
import daproxy.http.parsers.ParserMap;
import daproxy.http.parsers.RequestParser;
import daproxy.http.request.Request;
import daproxy.log.LogUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RequestHandler implements Runnable{

    private static final int REQUEST_BUFFER_SIZE = 8092;

    private final Socket socket;

    public RequestHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        SocketAddress remoteAddr = socket.getRemoteSocketAddress();
        SocketAddress localAddr = socket.getLocalSocketAddress();
        try {
            log.debug("accepted connection from {} with connection {} ", remoteAddr, localAddr);

            Request request = waitForConnect();

            request.handle(socket);
            // respondOK()

        } catch (InvalidRequestException | NotYetImplementedException ex) {
            log.error("Invalid Request Received", ex);
            try {
                socket.getOutputStream().write(Response.BAD_REQUEST.toString().getBytes(StandardCharsets.US_ASCII));
            } catch (IOException ioEX) {
                log.error("Error writing BAD Request to client", ioEX);
            }
        } catch (IOException ex) { // AN IOException will occur if the read request is blocking and the socket is
            // closed by the thread pool.
            log.error("Client Socket " + remoteAddr + " closed.", ex);
        } finally {
            try {
                socket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public RequestMethod parseHTTPMethod(byte[] request, int length) throws InvalidRequestException {
        if (length < RequestMethod.shortest()) {
            throw new InvalidRequestException("Incoming request too short");
        }
        log.debug("Request =  {}", request);

        for(RequestMethod m : RequestMethod.values()) {
            byte[] mB = m.toString().getBytes(StandardCharsets.US_ASCII);

            if(length < mB.length + 1) { // +1 for checking for a space after
                continue;
            }
            log.debug("Considering  {} mb.length = {} ", m, mB.length);
            int i = 0;
            for( ; i < length && i < mB.length; i++) {
                if (mB[i] != request[i]) {
                    log.debug("Considering {} breaking at i = {} ", m, i);
                    break;

                }
            }
            log.debug("Considering {} i = {}", m, i);
            if (i < mB.length) {
                continue;
            }

            if(request[i] != ' ') {
                log.debug("request[{}] = {}", i, request[i] );
                continue;
            }
            return m;
        }

        throw new InvalidRequestException("Did not find valid request method");
    }

    public Request waitForConnect() throws IOException, InvalidRequestException {
        InputStream in = socket.getInputStream();

        ByteBuffer buff = ByteBuffer.wrap(new byte[REQUEST_BUFFER_SIZE]);
        while (true) {
            byte[] tmp = new byte[REQUEST_BUFFER_SIZE];
            int numBytes = in.read(tmp); // whenever I try to use readAllBytes, it just hangs indefintely.

            if (numBytes == -1) {
                throw new SocketException("Detected closed socket");
            }

            buff.put(tmp, 0, numBytes);

            // log.debug("Received new bytes: " + LogUtils.bytesToHex(tmp, 0, numBytes));
            // log.debug("Request Buffer: " + LogUtils.bytesToHex(buff.array(), 0, buff.position()));

            try {
                RequestMethod rm = parseHTTPMethod(buff.array(), buff.position());
                RequestParser parser = ParserMap.get(rm);
                return parser.parse(buff.array(), buff.position());// firstRequest.toString());
            } catch (IncompleteRequestException ex) {
                log.debug("Partially formed request. Waiting for more");
            }
        }

        // RFC2817 [Page 7]: Like any other pipelined HTTP/1.1 request, data to be
        // tunneled may be
        // sent immediately after the blank line. The usual caveats also apply:
        // data may be discarded if the eventual response is negative, and the
        // connection may be reset with no response if more than one TCP segment
        // is outstanding.
        // ==> you cannot discard any data after on the pipe, and should be forwarded to
        // the target proxy host.
    }

    /**
     * Rejects a request due to too many active connections on the server.  Tries to return a HTTP 503 to client, and closes the socket.
     */
    public void reject() {
        try {
            socket.getOutputStream().write(Response.SERVICE_UNAVAILABLE.toString().getBytes(StandardCharsets.US_ASCII));
        } catch (IOException ioEX) {
            log.error("Error writing Service Unavailable to client", ioEX);
        } finally {
            terminate();
        }
    }

    /**
     * This method will close the socket, and rely on TCP to properly inform the client
     */
    public void terminate() {
        try {
            socket.close();
        } catch (IOException ioEX) {
            log.error("Error trying to close socket", ioEX);
        }   
    }
}
