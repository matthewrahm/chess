package dataaccess.sql;

import dataaccess.DataAccessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import server.Server;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SqlServerFailureTests {
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

    @ParameterizedTest
    @ValueSource(strings = {"register", "login", "logout", "create", "list", "join", "clear"})
    void databaseOutageReturnsSafeErrorsAndSameServerRecovers(String operation) throws Exception {
        String token = http.register("player");
        int id = http.createGame(token, "Preserved");
        var data = new SqlDataAccess();
        var original = data.getGame(id);
        database.configureUnavailableServer();
        try {
            request(operation, token, id, 500);
        } finally {
            database.configureDatabase(database.databaseName());
        }

        assertEquals(original, data.getGame(id));
        assertEquals(1, data.listGames().size());
        assertNotNull(data.getUser("player"));
        assertNotNull(data.getAuth(token));
        assertNull(data.getUser("new-player"));
        request(operation, token, id, 200);
    }

    @Test
    void unavailableStoragePreventsStartupUntilDatabaseRecovers() throws Exception {
        database.configureUnavailableServer();
        try {
            var failure = assertThrows(IllegalStateException.class, Server::new);
            assertEquals("Unable to initialize server storage", failure.getMessage());
            assertInstanceOf(DataAccessException.class, failure.getCause());
        } finally {
            database.configureDatabase(database.databaseName());
        }
        http.restart();
        String token = http.register("recovered");
        assertTrue(http.call("GET", "/game", token, null, 200).getAsJsonArray("games").isEmpty());
    }

    private void request(String operation, String token, int id, int expectedStatus) throws Exception {
        switch (operation) {
            case "register" -> http.call("POST", "/user", null, http.registration("new-player"), expectedStatus);
            case "login" -> http.call("POST", "/session", null,
                    Map.of("username", "player", "password", "test-password"), expectedStatus);
            case "logout" -> http.call("DELETE", "/session", token, null, expectedStatus);
            case "create" -> http.call("POST", "/game", token, Map.of("gameName", "Recovered"), expectedStatus);
            case "list" -> http.call("GET", "/game", token, null, expectedStatus);
            case "join" -> http.call("PUT", "/game", token,
                    Map.of("gameID", id, "playerColor", "WHITE"), expectedStatus);
            case "clear" -> http.call("DELETE", "/db", null, null, expectedStatus);
            default -> throw new IllegalArgumentException("Unknown test operation: " + operation);
        }
    }
}
