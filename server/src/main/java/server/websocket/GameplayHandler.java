package server.websocket;

import dataaccess.DataAccessException;
import io.javalin.websocket.WsContext;
import io.javalin.websocket.WsMessageContext;
import service.GameplayException;
import service.GameplayService;
import websocket.messages.ErrorMessage;

/** Translates WebSocket events into validated gameplay requests. */
public final class GameplayHandler {
    private final GameplayService service;
    private final GameCommandReader reader = new GameCommandReader();
    private final GameConnections connections = new GameConnections();

    public GameplayHandler(GameplayService service) {
        this.service = service;
    }

    public synchronized void open(WsContext context) {
        connections.open(context.sessionId(), context.session);
    }

    public synchronized void message(WsMessageContext context) {
        try {
            var command = reader.read(context.message());
            service.authorize(command);
            connections.send(context.sessionId(), new ErrorMessage("This gameplay action is not implemented yet"));
        } catch (GameplayException exception) {
            connections.send(context.sessionId(), new ErrorMessage(exception.getMessage()));
        } catch (DataAccessException | RuntimeException exception) {
            connections.send(context.sessionId(), new ErrorMessage("Unable to process game command"));
        }
    }

    public synchronized void binary(WsContext context) {
        connections.send(context.sessionId(), new ErrorMessage("Expected a JSON text command"));
    }

    public synchronized void close(WsContext context) {
        connections.remove(context.sessionId());
    }
}
