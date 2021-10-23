package daproxy.http.exceptions;

public class NotYetImplementedException extends RuntimeException {
    public NotYetImplementedException(String errorMessage) {
        super(errorMessage);
    }
}