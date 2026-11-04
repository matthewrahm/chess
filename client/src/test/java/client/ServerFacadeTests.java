package client;

import chess.ChessGame;
import com.sun.net.httpserver.HttpServer;
import dataaccess.sql.SqlDataAccess;
import model.AuthData;
import model.UserData;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import server.Server;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/** Uses the configured disposable database; run with scripts/verify_tests.py. */
public class ServerFacadeTests {
    private static Server server;
    private static ServerFacade facade;
    private static SqlDataAccess storage;

    @BeforeAll
    static void startServer() throws Exception {
        server = new Server();
        var port = server.run(0);
        facade = new ServerFacade(port);
        storage = new SqlDataAccess();
    }

    @BeforeEach
    void clearDatabase() throws Exception {
        facade.clear();
    }

    @AfterAll
    static void stopServer() throws Exception {
        try {
            if (storage != null) {
                storage.clear();
            }
        } finally {
            if (server != null) {
                server.stop();
            }
        }
    }

    @Test
    void clearRemovesUsersSessionsAndGamesThroughHttp() throws Exception {
        storage.createUser(new UserData("player", "stored-hash", "player@example.com"));
        storage.createAuth(new AuthData("token", "player"));
        int gameId = storage.createGame("Practice", new ChessGame());

        facade.clear();

        assertNull(storage.getUser("player"));
        assertNull(storage.getAuth("token"));
        assertNull(storage.getGame(gameId));
        assertTrue(storage.listGames().isEmpty());
    }

    @Test
    void clearingAnEmptyDatabaseIsSafeToRepeat() {
        assertDoesNotThrow(() -> {
            facade.clear();
            facade.clear();
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 65536})
    void rejectsInvalidPorts(int port) {
        assertThrows(IllegalArgumentException.class, () -> new ServerFacade(port));
    }

    @Test
    void clearReportsServerFailureWithoutLeakingResponseDetails() throws Exception {
        var failedServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        failedServer.createContext("/db", exchange -> {
            byte[] body = "private database details".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(500, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        failedServer.start();
        try {
            var failedFacade = new ServerFacade(failedServer.getAddress().getPort());
            var error = assertThrows(ServerException.class, failedFacade::clear);
            assertEquals("The server could not complete the request. Please try again.", error.getMessage());
        } finally {
            failedServer.stop(0);
        }
    }

    @Test
    void clearReportsAnUnavailableServer() throws Exception {
        var stoppedServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        stoppedServer.start();
        int port = stoppedServer.getAddress().getPort();
        stoppedServer.stop(0);

        var disconnected = new ServerFacade(port);
        var error = assertThrows(ServerException.class, disconnected::clear);
        assertEquals("Unable to reach the server. Please try again.", error.getMessage());
    }
}
