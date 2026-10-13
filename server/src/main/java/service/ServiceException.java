package service;

/** Business errors that the HTTP handler will translate to API status codes. */
public class ServiceException extends Exception {
    public enum Code {
        BAD_REQUEST, UNAUTHORIZED, ALREADY_TAKEN
    }

    private final Code code;

    public ServiceException(Code code) {
        super(switch (code) {
            case BAD_REQUEST -> "Error: bad request";
            case UNAUTHORIZED -> "Error: unauthorized";
            case ALREADY_TAKEN -> "Error: already taken";
        });
        this.code = code;
    }

    public Code getCode() {
        return code;
    }
}
