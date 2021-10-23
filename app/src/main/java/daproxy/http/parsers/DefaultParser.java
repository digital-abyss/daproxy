package daproxy.http.parsers;

import daproxy.http.exceptions.IncompleteRequestException;
import daproxy.http.exceptions.InvalidRequestException;
import daproxy.http.exceptions.NotYetImplementedException;
import daproxy.http.request.Request;

public class DefaultParser implements RequestParser {

    @Override
    public Request parse(byte[] request, int length) throws InvalidRequestException, IncompleteRequestException {
        throw new NotYetImplementedException("Request Handler for method is not yet implemented");
    }
    
}
