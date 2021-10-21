package daproxy.http;


public class InvalidRequestException extends Exception {

    private final String requestString;

    public InvalidRequestException(String firstLine) {
        super("Server does not support other HTTP Methods");
        this.requestString = firstLine;
    }

}
