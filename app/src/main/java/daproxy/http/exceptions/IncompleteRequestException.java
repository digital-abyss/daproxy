package daproxy.http.exceptions;

public class IncompleteRequestException extends Exception {

    public IncompleteRequestException(String errorMessage) {
        super("errorMessage");
    }
}
