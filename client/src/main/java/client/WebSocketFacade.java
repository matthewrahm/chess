package client;

import chess.ChessMove;
import com.google.gson.Gson;
import websocket.commands.MakeMoveCommand;
import websocket.commands.UserGameCommand;
import websocket.messages.ErrorMessage;
import websocket.messages.LoadGameMessage;
import websocket.messages.NotificationMessage;
import websocket.messages.ServerMessage;

import jakarta.websocket.*;
import java.io.IOException;
import java.net.URI;

@ClientEndpoint
public class WebSocketFacade {

    private Session session;
    private final ServerMessageHandler handler;
    private final Gson gson = new Gson();

    public interface ServerMessageHandler {
        void onLoadGame(LoadGameMessage message);
        void onNotification(NotificationMessage message);
        void onError(ErrorMessage message);
    }

    public WebSocketFacade(String url, ServerMessageHandler handler) throws Exception {
        this.handler = handler;
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        this.session = container.connectToServer(this, new URI(url));
    }

    @OnMessage
    public void onMessage(String message) {
        ServerMessage serverMessage = gson.fromJson(message, ServerMessage.class);
        switch (serverMessage.getServerMessageType()) {
            case LOAD_GAME -> handler.onLoadGame(gson.fromJson(message, LoadGameMessage.class));
            case NOTIFICATION -> handler.onNotification(gson.fromJson(message, NotificationMessage.class));
            case ERROR -> handler.onError(gson.fromJson(message, ErrorMessage.class));
        }
    }

    public void sendConnect(String authToken, int gameID) throws IOException {
        var cmd = new UserGameCommand(UserGameCommand.CommandType.CONNECT, authToken, gameID);
        session.getBasicRemote().sendText(gson.toJson(cmd));
    }

    public void sendMakeMove(String authToken, int gameID, ChessMove move) throws IOException {
        var cmd = new MakeMoveCommand(authToken, gameID, move);
        session.getBasicRemote().sendText(gson.toJson(cmd));
    }

    public void sendResign(String authToken, int gameID) throws IOException {
        var cmd = new UserGameCommand(UserGameCommand.CommandType.RESIGN, authToken, gameID);
        session.getBasicRemote().sendText(gson.toJson(cmd));
    }

    public void sendLeave(String authToken, int gameID) throws IOException {
        var cmd = new UserGameCommand(UserGameCommand.CommandType.LEAVE, authToken, gameID);
        session.getBasicRemote().sendText(gson.toJson(cmd));
    }

    public void close() throws IOException {
        session.close();
    }
}
