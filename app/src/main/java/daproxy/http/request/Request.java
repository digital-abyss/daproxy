package daproxy.http.request;

import java.net.Socket;

import daproxy.http.RequestMethod;
import daproxy.http.Response;

public interface Request {

    public RequestMethod getMethod();

    public Response handle(Socket socket);

}
