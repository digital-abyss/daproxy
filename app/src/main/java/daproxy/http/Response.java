package daproxy.http;

import java.util.Collections;
import java.util.Map;

public enum Response {
    CONNECTION_ESTABLISHED, BAD_REQUEST, NOT_FOUND, SERVICE_UNAVAILABLE;

    // This is to allow overloading of the response code and messages.
    // While https://datatracker.ietf.org/doc/html/rfc7231#section-6 does not have
    // duplicate response codes
    // It seems that some proxy servers return 200 Connection Established as the
    // message for a proxy request.
    private static final Map<Response, Integer> code = Collections.unmodifiableMap(
            Map.of(CONNECTION_ESTABLISHED, 200, BAD_REQUEST, 400, NOT_FOUND, 404, SERVICE_UNAVAILABLE, 503));
    private static final Map<Response, String> reasonPhrase = Collections
            .unmodifiableMap(Map.of(CONNECTION_ESTABLISHED, "Connection Established", BAD_REQUEST, "Bad Request",
                    NOT_FOUND, "Not Found", SERVICE_UNAVAILABLE, "Service Unavailable"));

    public String toString() {
        return String.format("HTTP/1.1 %s %s\r\n\r\n", Integer.toString(code.get(this)), reasonPhrase.get(this)); // +
        // "Proxy-agent: daproxy 0.0.1\n" +
        // "\r\n";
    }
}
