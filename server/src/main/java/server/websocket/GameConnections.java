package server.websocket;

import com.google.gson.Gson;
import org.eclipse.jetty.websocket.api.Session;
import service.GameplayException;
import websocket.messages.ServerMessage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Owns connections and authenticated game subscriptions for one server instance. */
final class GameConnections {
    private final Map<String, GameConnection> connections = new HashMap<>();
    private final Map<String, Subscription> subscriptions = new HashMap<>();
    private final Gson gson = new Gson();

    synchronized void open(String id, Session session) {
        connections.put(id, new GameConnection(session));
    }

    synchronized void remove(String id) {
        connections.remove(id);
        subscriptions.remove(id);
    }

    synchronized void unsubscribe(String id) {
        subscriptions.remove(id);
    }

    synchronized boolean subscribe(String id, String username, int gameID) throws GameplayException {
        var connection = connections.get(id);
        if (connection == null || !connection.isOpen()) {
            remove(id);
            throw new GameplayException("Connection is closed");
        }
        var requested = new Subscription(username, gameID);
        var existing = subscriptions.get(id);
        if (existing != null && !existing.equals(requested)) {
            throw new GameplayException("Leave the current game before changing game or user");
        }
        subscriptions.put(id, requested);
        return existing == null;
    }

    synchronized void requireSubscription(String id, String username, int gameID) throws GameplayException {
        if (!new Subscription(username, gameID).equals(subscriptions.get(id))) {
            throw new GameplayException("Connect to this game as this user first");
        }
    }

    synchronized void broadcast(int gameID, String excludedID, ServerMessage message) {
        for (String id : List.copyOf(subscriptions.keySet())) {
            var subscription = subscriptions.get(id);
            if (subscription.gameID() == gameID && !id.equals(excludedID)) {
                send(id, message);
            }
        }
    }

    synchronized void send(String id, ServerMessage message) {
        var connection = connections.get(id);
        if (connection != null && connection.isOpen()) {
            connection.send(gson.toJson(message));
        } else {
            remove(id);
        }
    }

    private record Subscription(String username, int gameID) { }
}
