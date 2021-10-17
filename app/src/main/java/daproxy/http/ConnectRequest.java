package daproxy.http;

import java.net.Socket;

public class ConnectRequest implements Request {

    private final String connectString;

    public ConnectRequest(String firstLine) {
        connectString = firstLine;
    }

    @Override
    public RequestMethod getMethod() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String handle(Socket socket) {
        // TODO Auto-generated method stub
        return "Connect Request";
    }

    
}
