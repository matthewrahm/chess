package websocket.messages;

import chess.ChessGame;

/** Sends the current chess position, turn, and move history to a client. */
public final class LoadGameMessage extends ServerMessage {
    private final ChessGame game;

    public LoadGameMessage(ChessGame game) {
        super(ServerMessageType.LOAD_GAME);
        this.game = game;
    }

    public ChessGame getGame() {
        return game;
    }
}
