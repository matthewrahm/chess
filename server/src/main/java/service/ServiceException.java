package service;

/** Exception thrown by service methods, carrying an HTTP status code. */
public class ServiceException extends Exception {
    private final int statusCode;

    public ServiceException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
