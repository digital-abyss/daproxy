package daproxy.http.request;

import java.net.Socket;

import daproxy.http.RequestMethod;
import daproxy.http.Response;
import daproxy.http.exceptions.InvalidRequestException;

public interface Request {

    /**
     * 
     * @return the HTTP method for the given request
     */
    public RequestMethod getMethod();

    /**
     * Evaluates the constructed HTTP Request object, and writes data (as necessary) back to the socket, or, if only returning an HTTP response,
     * the HTTP Response object is returned to the caller for writing back to the socket.
     * @param socket
     * @return an HTTP Response object that can be serialized on the socket.
     */
    public Response handle(Socket socket) throws InvalidRequestException;

}
