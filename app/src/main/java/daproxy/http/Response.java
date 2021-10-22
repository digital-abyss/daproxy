package daproxy.http;

import java.util.Collections;
import java.util.Map;


public class Response {

    private final int code;

    private final Map<Integer, String> codeMap = Collections.unmodifiableMap(Map.of(
        200, "Connection established",
        400, "BAD REQUEST",
        404, "NOT FOUND"
    ));

    public static Response CONNECTION_ESTABLISHED() {
        return new Response(200);
    }
    public static Response BAD_REQUEST() {
        return new Response(400);
    }
    public static Response NOT_FOUD() {
        return new Response(404);
    }

    public Response(int code) {
        this.code = code;
    }

    public String toString() {
        return String.format("HTTP/1.1 %s %s\r\n\r\n",Integer.toString(code), codeMap.get(code)); // +
                // "Proxy-agent: daproxy 0.0.1\n" +
                // "\r\n";
    }
    
}
