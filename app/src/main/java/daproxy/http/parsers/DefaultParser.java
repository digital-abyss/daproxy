package daproxy.http.parsers;

import daproxy.http.exceptions.NotYetImplementedException;

public class DefaultParser implements RequestParser {

    @Override
    public void parse() {
       throw new NotYetImplementedException("Request Handler for method is not yet implemented");
        
    }
    
}
