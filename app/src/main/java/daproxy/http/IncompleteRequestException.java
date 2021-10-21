package daproxy.http;

public class IncompleteRequestException extends Exception {
    private final String requestString;

    public IncompleteRequestException(String firstLine) {
        super("Request is not a fully formed HTTP request");
        this.requestString = firstLine;
    }
}
