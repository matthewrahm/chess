package websocket.messages;

/** An error intended only for the client whose command failed. */
public final class ErrorMessage extends ServerMessage {
    private final String errorMessage;

    public ErrorMessage(String detail) {
        super(ServerMessageType.ERROR);
        errorMessage = "Error: " + detail;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
