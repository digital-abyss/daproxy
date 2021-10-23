package daproxy.http.parsers;

import java.util.Collections;
import java.util.Map;

import daproxy.http.RequestMethod;

public class ParserMap {

    private static final Map<RequestMethod, RequestParser> H_MAP = Collections.unmodifiableMap(Map.of(
       RequestMethod.CONNECT, new ConnectParser()
    ));
    
    private static final RequestParser DEFAULT = new DefaultParser();
    public RequestParser get(RequestMethod rm) {
        if(!H_MAP.containsKey(rm)) {
            return DEFAULT;
        }
        return H_MAP.get(rm);
    }
}
