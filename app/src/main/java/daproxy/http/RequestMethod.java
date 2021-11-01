package daproxy.http;

public enum RequestMethod {
    GET, HEAD, POST, PUT, DELETE, CONNECT, OPTIONS, TRACE, PATCH;

    private static final int SHORTEST_REQUEST = 3;

    /**
     * 
     * @return The length of the shortest RequestMethod.
     */
    public static int shortest() {
        return SHORTEST_REQUEST;
    }
}
