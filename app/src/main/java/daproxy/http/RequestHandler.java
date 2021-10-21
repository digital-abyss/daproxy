package daproxy.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketAddress;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RequestHandler {

    private static final int REQUEST_LINE_SIZE = 80;

    private final Socket socket;

    public RequestHandler(Socket socket) {
        this.socket = socket;
    }

    public void handle() {
        try {
            InputStream in = socket.getInputStream();
            SocketAddress remoteAddr = socket.getRemoteSocketAddress();
            SocketAddress localAddr = socket.getLocalSocketAddress();
            log.debug("accepted connection from " + remoteAddr + " with connection " + localAddr);


            int recvBytes;
            byte[] buf = new byte[1024];
            StringBuilder firstRequest = new StringBuilder(REQUEST_LINE_SIZE);
            //while (true) {
                recvBytes = in.read(buf);

                System.out.println("Processing Start of Request: reading " + recvBytes );

                // System.out.println("HEX Output: " + hex(buf, 0, recvBytes));
                firstRequest.append(new String(buf, 0, recvBytes, "UTF-8"));
          //  }
    


            System.out.println("Text = " + firstRequest.toString());

            Response response = evaluateRequest(firstRequest.toString()).handle(socket);
            System.out.println("Response = " + response);
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public Request evaluateRequest(String firstLine) {

        //if (firstLine.matches(ConnectRequest.MATCHER)) {
            return new ConnectRequest(firstLine);
        // } else {
        //     return new UnknownRequest(firstLine);
        // }
    }
}

