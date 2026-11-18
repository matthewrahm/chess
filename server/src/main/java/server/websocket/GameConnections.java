package server.websocket;

import com.google.gson.Gson;
import org.eclipse.jetty.websocket.api.Session;
import websocket.messages.ServerMessage;

import java.util.HashMap;
import java.util.Map;

/** Owns transport connections for one server instance. */
final class GameConnections {
    private final Map<String, GameConnection> connections = new HashMap<>();
    private final Gson gson = new Gson();

    synchronized void open(String id, Session session) {
        connections.put(id, new GameConnection(session));
    }

    synchronized void remove(String id) {
        connections.remove(id);
    }

    synchronized void send(String id, ServerMessage message) {
        var connection = connections.get(id);
        if (connection != null && connection.isOpen()) {
            connection.send(gson.toJson(message));
        } else {
            remove(id);
        }
    }
}
