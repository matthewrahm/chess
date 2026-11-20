package server.websocket;

import dataaccess.DataAccessException;
import io.javalin.websocket.WsContext;
import io.javalin.websocket.WsMessageContext;
import service.GameplayException;
import service.GameplayService;
import websocket.commands.UserGameCommand;
import websocket.commands.MakeMoveCommand;
import websocket.messages.ErrorMessage;
import websocket.messages.LoadGameMessage;
import websocket.messages.NotificationMessage;

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
            var access = service.authorize(command);
            if (command.getCommandType() == UserGameCommand.CommandType.CONNECT) {
                connect(context.sessionId(), access);
            } else {
                connections.requireSubscription(context.sessionId(), access.username(), command.getGameID());
                if (command instanceof MakeMoveCommand move) {
                    makeMove(context.sessionId(), access.username(), move);
                } else if (command.getCommandType() == UserGameCommand.CommandType.LEAVE) {
                    leave(context.sessionId(), access.username(), command);
                } else if (command.getCommandType() == UserGameCommand.CommandType.RESIGN) {
                    resign(access.username(), command);
                } else {
                    throw new GameplayException("Unknown gameplay action");
                }
            }
        } catch (GameplayException exception) {
            connections.send(context.sessionId(), new ErrorMessage(exception.getMessage()));
        } catch (DataAccessException | RuntimeException exception) {
            connections.send(context.sessionId(), new ErrorMessage("Unable to process game command"));
        }
    }

    private void resign(String username, UserGameCommand command) throws GameplayException, DataAccessException {
        var result = service.resign(command, username);
        connections.broadcast(result.game().gameID(), null,
                new NotificationMessage(result.username() + " resigned. Game over."));
    }

    private void leave(String id, String username, UserGameCommand command)
            throws GameplayException, DataAccessException {
        var result = service.leave(command, username);
        connections.unsubscribe(id);
        connections.broadcast(result.game().gameID(), id,
                new NotificationMessage(result.username() + " left the game."));
    }

    private void makeMove(String id, String username, MakeMoveCommand command)
            throws GameplayException, DataAccessException {
        var result = service.makeMove(command, username);
        int gameID = result.game().gameID();
        connections.broadcast(gameID, null, new LoadGameMessage(result.game().game()));
        connections.broadcast(gameID, id, new NotificationMessage(result.moveNotice()));
        if (result.statusNotice() != null) {
            connections.broadcast(gameID, null, new NotificationMessage(result.statusNotice()));
        }
    }

    private void connect(String id, GameplayService.GameAccess access) throws GameplayException {
        var loaded = new LoadGameMessage(access.game().game());
        int gameID = access.game().gameID();
        boolean newSubscription = connections.subscribe(id, access.username(), gameID);
        connections.send(id, loaded);
        if (newSubscription) {
            connections.broadcast(gameID, id, new NotificationMessage(access.connectionNotice()));
        }
    }

    public synchronized void binary(WsContext context) {
        connections.send(context.sessionId(), new ErrorMessage("Expected a JSON text command"));
    }

    public synchronized void close(WsContext context) {
        connections.remove(context.sessionId());
    }
}
