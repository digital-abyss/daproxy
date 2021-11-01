package daproxy.http.request;

import java.io.IOException;
import java.net.Socket;

import daproxy.conf.Config;
import daproxy.http.IOHelper;
import daproxy.http.RequestMethod;
import daproxy.http.Response;
import daproxy.http.Url;
import daproxy.http.exceptions.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;

/**
 * A valid CONNECT request is as follows: CONNECT SP <url> SP HTTP/1.1 CRLF
 * where SP is space, and CRLF is \r\n See:
 * https://datatracker.ietf.org/doc/html/rfc2616/ [page 16]
 */
@Slf4j
public class ConnectRequest implements Request {

    private final Url url;
    private final String headersBlob;
    private final byte[] firstBytesToWrite;

    public ConnectRequest(String url, String headersBlob, byte[] firstBytesToWrite) {
        this.url = new Url(url);
        this.headersBlob = headersBlob;
        this.firstBytesToWrite = firstBytesToWrite;
    }

    @Override
    public RequestMethod getMethod() {
        return RequestMethod.CONNECT;
    }

    public Url getUrl() {
        return url;
    }

    @Override
    public Response handle(Socket socket) throws InvalidRequestException {
        log.debug("Handling a Connect Request to url: {}", url);
        log.debug("Did not handle headers: {}", headersBlob);
        log.debug("Did not write possible first bytes: {} ", firstBytesToWrite);
        try {
            boolean matches = false;
            for(String allowedUrl : Config.getConfig().getAllowList()) {
                if (getUrl().getHost().matches(allowedUrl)) {
                    matches = true;
                    break;
                }
            }
            if (!matches) {
                throw new InvalidRequestException("host " + getUrl().getHost() + " is not in allowList of" + Config.getConfig().getAllowList());
            }


            Socket downstreamSocket = new Socket(url.getHost(), url.getPort()); // extractPort());
            // if this connects, we can give a 200 OK back to client, which will then allow
            // it to initate further packet transfers.
            // BufferedWriter br = new BufferedWriter(new
            // OutputStreamWriter(socket.getOutputStream()));
            Response resp = Response.CONNECTION_ESTABLISHED;

            socket.getOutputStream().write(resp.toString().getBytes());
            socket.getOutputStream().flush();
            log.debug("Wrote  {} to client socket to indicate successful connect to downstream", resp);

            // read from client, write to server. read from remote, write to client. repeat.

            new Thread(() -> {
                writeAllData(socket, downstreamSocket);

            }).start();

            writeAllData(downstreamSocket, socket);

        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return Response.CONNECTION_ESTABLISHED;
    }


    public void writeAllData(Socket input, Socket output) {
        while (!input.isClosed() && !output.isClosed()) {
            try {
                // log.debug("Thread: Socket (bound, connected, closed, in avail): (" + input.isBound()
                //         + ", " + input.isConnected() + ", " + input.isClosed() + ", "
                //         + input.getInputStream().available() + ")");
                // log.debug("Thread: Downstream Socket (bound, connected, closed, in avail): ("
                //         + output.isBound() + ", " + output.isConnected() + ", "
                //         + output.isClosed() + ", " + output.getInputStream().available()
                //         + ")");

                IOHelper.writeInputToOutput(input.getInputStream(), output.getOutputStream());
            } catch (IOException ex) {
                log.debug("Caught IOException - usually means connection is closed", ex);
                try {
                    input.close();
                } catch (Exception ex2) {
                    log.error("Error trying to close input socket", ex2);
                }
                try {
                    output.close();
                } catch (Exception ex2) {
                    log.error("Error trying to close output socket", ex2);
                }
            }
        } 
    }
}