package client;

/** Authentication has expired; the console can safely return to its sign-in menu. */
public class SessionExpiredException extends ServerException {
    public SessionExpiredException(String message) {
        super(message);
    }
}
