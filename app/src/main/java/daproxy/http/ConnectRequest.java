package daproxy.http;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class ConnectRequest implements Request {

    public static final String MATCHER = "CONNECT [-a-zA-Z0-9+&@#/%?=~_|!:,.;]* HTTP/1\\.1$";

    private final String connectString;

    public ConnectRequest(String firstLine) {
        connectString = firstLine;
    }

    @Override
    public RequestMethod getMethod() {
        return RequestMethod.CONNECT;
    }

    @Override
    public Response handle(Socket socket) {

        try {
            Socket downstreamSocket = new Socket(extractUrl(), extractPort());
            
        } catch (IndexOutOfBoundsException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return Response.OK();
    }

    public String extractUrl() {
        return connectString.split(" ")[1].split(":")[0];
    }

    public int extractPort() {
        String[] portString = connectString.split(" ")[1].split(":");
        if (portString.length >= 1) {
            return Integer.parseInt(portString[1]);
        }
        //TODO -- is this correct?
        return 80;
    }
}
