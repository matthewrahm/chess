package websocket.messages;

/** A human-readable game event for players and observers. */
public final class NotificationMessage extends ServerMessage {
    private final String message;

    public NotificationMessage(String message) {
        super(ServerMessageType.NOTIFICATION);
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
