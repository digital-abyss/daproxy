package daproxy.http;

import java.net.Socket;

public interface Request {

    public RequestMethod getMethod();

    public String handle(Socket socket);

}
