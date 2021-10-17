package daproxy.http;

import com.google.common.collect.ImmutableMap;

public class Response {

    private final int code;

    private final ImmutableMap<Integer, String> codeMap = ImmutableMap.of(
        200, "OK",
        400, "BAD REQUEST",
        404, "NOT FOUND"
    );

    public static Response OK() {
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
        return String.format("HTTP1.1 %s %s\n",Integer.toString(code), codeMap.get(code)) +
                "Proxy-agent: daproxy 0.0.1\n" +
                "\r\n";
    }
    
}
