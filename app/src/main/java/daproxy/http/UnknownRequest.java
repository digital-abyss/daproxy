package daproxy.http;

import java.net.Socket;

public class UnknownRequest implements Request {

    private final String requestString;

    public UnknownRequest(String firstLine) {
        this.requestString = firstLine;
    }

    @Override
    public RequestMethod getMethod() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String handle(Socket socket) {
        // TODO Auto-generated method stub
        return "Unknown Request";
    }

}
