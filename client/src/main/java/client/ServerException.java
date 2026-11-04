package client;

/** A server or connection failure with a message suitable for the console UI. */
public class ServerException extends Exception {
    public ServerException(String message) {
        super(message);
    }

    public ServerException(String message, Throwable cause) {
        super(message, cause);
    }
}
