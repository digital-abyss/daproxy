package daproxy.http.parsers;

import daproxy.http.exceptions.IncompleteRequestException;
import daproxy.http.exceptions.InvalidRequestException;
import daproxy.http.request.Request;

public interface RequestParser {
    public Request parse(byte[] request, int length) throws InvalidRequestException, IncompleteRequestException;
}
