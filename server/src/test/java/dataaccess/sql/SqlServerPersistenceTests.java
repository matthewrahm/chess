package dataaccess.sql;

import chess.ChessMove;
import chess.ChessPosition;
import model.GameData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SqlServerPersistenceTests {
    private DatabaseTestSupport database;
    private SqlServerTestSupport http;

    @BeforeEach
    void setUp() throws Exception {
        database = new DatabaseTestSupport();
        http = new SqlServerTestSupport();
    }

    @AfterEach
    void tearDown() throws Exception {
        try {
            if (http != null) {
                http.close();
            }
        } finally {
            if (database != null) {
                database.close();
            }
        }
    }

    @Test
    void restartPreservesCredentialsSessionsPlayersAndFullGameState() throws Exception {
        String white = http.register("white");
        String black = http.register("black");
        int id = http.createGame(white, "Saved game");
        http.call("PUT", "/game", white, Map.of("gameID", id, "playerColor", "WHITE"), 200);
        http.call("PUT", "/game", black, Map.of("gameID", id, "playerColor", "BLACK"), 200);
        var data = new SqlDataAccess();
        var game = data.getGame(id);
        game.game().makeMove(new ChessMove(new ChessPosition(2, 5), new ChessPosition(4, 5), null));
        data.updateGame(game);

        http.restart();

        var listed = http.call("GET", "/game", white, null, 200).getAsJsonArray("games");
        assertEquals(1, listed.size());
        var saved = listed.get(0).getAsJsonObject();
        assertEquals(id, saved.get("gameID").getAsInt());
        assertEquals("Saved game", saved.get("gameName").getAsString());
        assertEquals("white", saved.get("whiteUsername").getAsString());
        assertEquals("black", saved.get("blackUsername").getAsString());
        assertEquals(listed, http.call("GET", "/game", black, null, 200).getAsJsonArray("games"));
        assertEquals(game, new SqlDataAccess().getGame(id));
        String login = http.call("POST", "/session", null,
                Map.of("username", "white", "password", "test-password"), 200).get("authToken").getAsString();
        assertNotEquals(white, login);
        http.call("POST", "/session", null, Map.of("username", "white", "password", "wrong"), 401);
        assertNotEquals(id, http.createGame(login, "New game"));
    }

    @Test
    void revokedSessionsAndClearedRecordsStayGoneAfterRestart() throws Exception {
        String revoked = http.register("player");
        String retained = http.call("POST", "/session", null,
                Map.of("username", "player", "password", "test-password"), 200).get("authToken").getAsString();
        int id = http.createGame(retained, "Before clear");
        http.call("DELETE", "/session", revoked, null, 200);

        http.restart();

        http.call("GET", "/game", revoked, null, 401);
        assertEquals(1, http.call("GET", "/game", retained, null, 200).getAsJsonArray("games").size());
        http.call("DELETE", "/db", null, null, 200);
        http.restart();
        http.call("GET", "/game", retained, null, 401);
        http.call("POST", "/session", null, Map.of("username", "player", "password", "test-password"), 401);
        String fresh = http.register("player");
        assertTrue(http.call("GET", "/game", fresh, null, 200).getAsJsonArray("games").isEmpty());
        http.call("PUT", "/game", fresh, Map.of("gameID", id, "playerColor", "WHITE"), 400);
        assertNotEquals(id, http.createGame(fresh, "After clear"));
    }

    @Test
    void runningServersShareUpdatesAndSessionRevocations() throws Exception {
        String token = http.register("player");
        int id = http.createGame(token, "Shared");
        try (var other = new SqlServerTestSupport()) {
            other.call("PUT", "/game", token, Map.of("gameID", id, "playerColor", "BLACK"), 200);
            var saved = http.call("GET", "/game", token, null, 200)
                    .getAsJsonArray("games").get(0).getAsJsonObject();
            assertEquals("player", saved.get("blackUsername").getAsString());
            other.call("DELETE", "/session", token, null, 200);
            http.call("GET", "/game", token, null, 401);
        }
        GameData saved = new SqlDataAccess().getGame(id);
        assertEquals("player", saved.blackUsername());
        assertEquals("Shared", saved.gameName());
    }
}
