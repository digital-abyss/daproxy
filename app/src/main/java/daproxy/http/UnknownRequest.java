package daproxy.http;

import java.net.Socket;

public class UnknownRequest implements Request {

    private final String requestString;

    public UnknownRequest(String firstLine) {
        this.requestString = firstLine;
    }

    @Override
    public RequestMethod getMethod() {
        return RequestMethod.INVALID;
    }

    @Override
    public Response handle(Socket socket) {
        System.out.println("Handling a Bad Request");
        return Response.BAD_REQUEST();
    }

}
