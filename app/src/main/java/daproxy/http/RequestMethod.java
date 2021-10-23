package daproxy.http;

import java.nio.charset.StandardCharsets;

public enum RequestMethod {
    GET,
    HEAD,
    POST,
    PUT,
    DELETE,
    CONNECT,
    OPTIONS,
    TRACE,
    PATCH;

    static int shortestLength = Integer.MAX_VALUE;
    static {
        for(RequestMethod m : RequestMethod.values()) {
            if (m.toString().length() < shortestLength) {
                shortestLength = m.toString().length();
            }
        }
    }

    /**
     * 
     * @return The length of the shortest RequestMethod.
     */
    public static int shortest() {
        return shortestLength;
    }
}
