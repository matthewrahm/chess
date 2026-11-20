package server.websocket;

import chess.ChessGame;
import com.google.gson.Gson;
import dataaccess.MemoryDataAccess;
import dataaccess.DataAccessException;
import model.GameData;
import model.AuthData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import server.Server;
import websocket.commands.UserGameCommand;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static websocket.commands.UserGameCommand.CommandType.CONNECT;

abstract class GameSocketFixture {
    protected MemoryDataAccess data;
    protected Server server;
    protected int port;
    protected int gameID;
    protected final Gson gson = new Gson();

    @BeforeEach
    void startServer() throws Exception {
        data = createDataAccess();
        data.createAuth(new AuthData("white-token", "Alice"));
        data.createAuth(new AuthData("black-token", "Bob"));
        data.createAuth(new AuthData("observer-token", "Charlie"));
        gameID = data.createGame("Practice", new ChessGame());
        server = new Server(data);
        port = server.run(0);
    }

    protected MemoryDataAccess createDataAccess() {
        return new MemoryDataAccess();
    }

    @AfterEach
    void stopServer() {
        server.stop();
    }

    protected void join(String token, int id, String color) throws Exception {
        try (var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build()) {
            String json = gson.toJson(Map.of("gameID", id, "playerColor", color));
            var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/game"))
                    .timeout(Duration.ofSeconds(3)).header("authorization", token)
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(json)).build();
            assertEquals(200, client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode());
        }
    }

    protected ChessGame connect(SocketTestClient client, String token, int id) throws Exception {
        client.send(new UserGameCommand(CONNECT, token, id));
        var response = client.next("LOAD_GAME");
        assertEquals(Set.of("serverMessageType", "game"), response.keySet());
        return gson.fromJson(response.get("game"), ChessGame.class);
    }

    protected String notice(SocketTestClient client) throws Exception {
        var response = client.next("NOTIFICATION");
        assertEquals(Set.of("serverMessageType", "message"), response.keySet());
        return response.get("message").getAsString();
    }

    protected static class FailingStore extends MemoryDataAccess {
        volatile boolean fail;

        @Override
        public synchronized void updateGame(GameData game) throws DataAccessException {
            if (fail) {
                throw new DataAccessException("private storage diagnostics");
            }
            super.updateGame(game);
        }
    }
}
