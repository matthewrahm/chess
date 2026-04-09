package websocket;

import org.eclipse.jetty.websocket.api.Session;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionManager {

    private final ConcurrentHashMap<Integer, ConcurrentHashMap<String, Connection>> gameConnections =
            new ConcurrentHashMap<>();

    private final ConcurrentHashMap<Session, SessionInfo> sessionInfo = new ConcurrentHashMap<>();

    private record SessionInfo(int gameID, String username) { }

    public void add(int gameID, String username, Session session) {
        gameConnections.computeIfAbsent(gameID, k -> new ConcurrentHashMap<>())
                .put(username, new Connection(username, session));
        sessionInfo.put(session, new SessionInfo(gameID, username));
    }

    public void remove(int gameID, String username) {
        var connections = gameConnections.get(gameID);
        if (connections != null) {
            var removed = connections.remove(username);
            if (removed != null) {
                sessionInfo.remove(removed.session());
            }
        }
    }

    public void removeBySession(Session session) {
        var info = sessionInfo.remove(session);
        if (info != null) {
            var connections = gameConnections.get(info.gameID());
            if (connections != null) {
                connections.remove(info.username());
            }
        }
    }

    public void broadcast(int gameID, String excludeUsername, String message) throws IOException {
        var connections = gameConnections.get(gameID);
        if (connections == null) {
            return;
        }

        var closedKeys = new ArrayList<String>();
        for (var entry : connections.entrySet()) {
            if (entry.getKey().equals(excludeUsername)) {
                continue;
            }
            try {
                if (entry.getValue().session().isOpen()) {
                    entry.getValue().session().getRemote().sendString(message);
                } else {
                    closedKeys.add(entry.getKey());
                }
            } catch (IOException e) {
                closedKeys.add(entry.getKey());
            }
        }
        for (String key : closedKeys) {
            remove(gameID, key);
        }
    }
}
