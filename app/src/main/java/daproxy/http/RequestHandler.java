package daproxy.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketAddress;

public class RequestHandler {

    private static final int REQUEST_LINE_SIZE = 80;

    private final Socket socket;

    public RequestHandler(Socket socket) {
        this.socket = socket;
    }

    public void handle() {
        try (InputStream in = socket.getInputStream()) {
            SocketAddress remoteAddr = socket.getRemoteSocketAddress();
            SocketAddress localAddr = socket.getLocalSocketAddress();
            System.out.println("accepted connection from " + remoteAddr + " with connection " + localAddr);


            StringBuilder firstLine = new StringBuilder(REQUEST_LINE_SIZE);
            //TODO: Implement timeout waiting for first line and size limit
            while (true) {
                int c = in.read();
                if (c == '\r' || c == '\n' || c == -1)
                    break;
                firstLine.append((char) c);
            }

            System.out.println("Text = " + firstLine.toString());

            String response = evaluateRequest(firstLine.toString()).handle(socket);
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

        if (firstLine.matches("CONNECT [a-z,A-Z,0-9,.,:]* HTTP1.1$")) {
            return new ConnectRequest(firstLine);
        } else {
            return new UnknownRequest(firstLine);
        }
    }
}

