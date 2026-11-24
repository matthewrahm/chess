package client.gameplay;

import chess.ChessGame;
import client.ServerException;
import websocket.commands.UserGameCommand;
import websocket.messages.ErrorMessage;
import websocket.messages.LoadGameMessage;
import websocket.messages.ServerMessage;

import java.util.Objects;

/** Tracks one game's live state; reconnects invalidate callbacks from older sockets. */
public final class GameplaySession implements AutoCloseable {
    public enum Role { WHITE, BLACK, OBSERVER }
    public enum Status { DISCONNECTED, CONNECTING, ACTIVE, CLOSED }

    /** Snapshots never expose the session's mutable board. */
    public record Snapshot(int gameID, Role role, ChessGame.TeamColor perspective, Status status, ChessGame game) {
        public Snapshot {
            game = game == null ? null : game.copy();
        }

        @Override
        public ChessGame game() {
            return game == null ? null : game.copy();
        }
    }

    /** Callbacks are serialized with session changes; handlers should only update state/output. */
    public interface Listener {
        void onMessage(Snapshot snapshot, ServerMessage message);

        void onDisconnect(Snapshot snapshot, String reason);
    }

    private final int gameID;
    private final String authToken;
    private final Role role;
    private final Listener listener;
    private final GameplayTransport.Factory factory;
    private GameplayTransport transport;
    private Status status = Status.DISCONNECTED;
    private ChessGame game;
    private long generation;

    public GameplaySession(int port, int gameID, String authToken, Role role, Listener listener) {
        this(gameID, authToken, role, listener, connectionFactory(port));
    }

    private static GameplayTransport.Factory connectionFactory(int port) {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("The server port must be between 1 and 65535.");
        }
        return observer -> WebSocketCommunicator.connect(port, observer);
    }

    GameplaySession(int gameID, String authToken, Role role, Listener listener, GameplayTransport.Factory factory) {
        if (gameID <= 0 || authToken == null || authToken.isBlank()) {
            throw new IllegalArgumentException("A game ID and authentication token are required.");
        }
        this.gameID = gameID;
        this.authToken = authToken;
        this.role = Objects.requireNonNull(role);
        this.listener = Objects.requireNonNull(listener);
        this.factory = Objects.requireNonNull(factory);
    }

    public synchronized Snapshot snapshot() {
        var perspective = role == Role.BLACK ? ChessGame.TeamColor.BLACK : ChessGame.TeamColor.WHITE;
        return new Snapshot(gameID, role, perspective, status, game);
    }

    /** HTTP join is the caller's responsibility for players; observers connect directly. */
    public void connect() throws ServerException {
        long attempt;
        synchronized (this) {
            if (status != Status.DISCONNECTED) {
                throw new ServerException("The game session is already connecting, active, or closed.");
            }
            status = Status.CONNECTING;
            attempt = ++generation;
        }
        GameplayTransport opened = null;
        try {
            opened = factory.open(new SessionMessages(attempt));
            synchronized (this) {
                if (attempt != generation || status != Status.CONNECTING) {
                    throw new ServerException("The game connection was cancelled.");
                }
                transport = opened;
            }
            opened.send(new UserGameCommand(UserGameCommand.CommandType.CONNECT, authToken, gameID));
        } catch (ServerException exception) {
            if (opened != null) {
                opened.close();
            }
            disconnected(attempt, exception.getMessage());
            throw exception;
        }
    }

    private synchronized void disconnected(long attempt, String reason) {
        if (attempt != generation || status == Status.CLOSED || status == Status.DISCONNECTED) {
            return;
        }
        generation++;
        status = Status.DISCONNECTED;
        if (transport != null) {
            transport.close();
            transport = null;
        }
        listener.onDisconnect(snapshot(), reason);
    }

    @Override
    public synchronized void close() {
        generation++;
        status = Status.CLOSED;
        if (transport != null) {
            transport.close();
            transport = null;
        }
    }

    private final class SessionMessages implements GameplayTransport.Listener {
        private final long attempt;

        private SessionMessages(long attempt) {
            this.attempt = attempt;
        }

        @Override
        public void onMessage(ServerMessage message) {
            synchronized (GameplaySession.this) {
                if (attempt != generation || status == Status.CLOSED || status == Status.DISCONNECTED) {
                    return;
                }
                if (message instanceof LoadGameMessage loaded) {
                    game = loaded.getGame();
                    status = Status.ACTIVE;
                }
                listener.onMessage(snapshot(), message);
                if (message instanceof ErrorMessage && status == Status.CONNECTING) {
                    disconnected(attempt, "Unable to join the live game. Please reconnect after correcting the error.");
                }
            }
        }

        @Override
        public void onDisconnect(String reason) {
            disconnected(attempt, reason);
        }
    }
}
