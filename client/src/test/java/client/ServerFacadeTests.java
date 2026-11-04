package client;

import chess.ChessGame;
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

import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mindrot.jbcrypt.BCrypt;

import java.util.stream.Stream;

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
        try (var failedServer = new TestHttpServer(500, "private database details")) {
            var failedFacade = new ServerFacade(failedServer.port());
            var error = assertThrows(ServerException.class, failedFacade::clear);
            assertEquals("The server could not complete the request. Please try again.", error.getMessage());
        }
    }

    @Test
    void clearReportsAnUnavailableServer() throws Exception {
        var disconnected = new ServerFacade(TestHttpServer.stoppedPort());
        var error = assertThrows(ServerException.class, disconnected::clear);
        assertEquals("Unable to reach the server. Please try again.", error.getMessage());
    }

    @Test
    void registrationCreatesUserAndUsableSession() throws Exception {
        var result = facade.register("player", "secret", "player@example.com");

        assertEquals("player", result.username());
        assertNotNull(result.authToken());
        assertFalse(result.authToken().isBlank());
        assertEquals(new AuthData(result.authToken(), "player"), storage.getAuth(result.authToken()));
        var user = storage.getUser("player");
        assertEquals("player@example.com", user.email());
        assertNotEquals("secret", user.password());
        assertTrue(BCrypt.checkpw("secret", user.password()));
    }

    @Test
    void registrationPreservesUnicodeAndJsonPunctuation() throws Exception {
        String username = "Zoë \"王\"\\";
        String password = "a\\b\"c";
        var result = facade.register(username, password, "zoe@example.com");

        assertEquals(username, result.username());
        assertEquals(username, storage.getAuth(result.authToken()).username());
        assertTrue(BCrypt.checkpw(password, storage.getUser(username).password()));
    }

    @Test
    void duplicateRegistrationPreservesOriginalUserAndSession() throws Exception {
        var first = facade.register("player", "original", "first@example.com");
        var originalUser = storage.getUser("player");

        var error = assertThrows(ServerException.class,
                () -> facade.register("player", "replacement", "second@example.com"));

        assertEquals("That username is already taken.", error.getMessage());
        assertEquals(originalUser, storage.getUser("player"));
        assertEquals("player", storage.getAuth(first.authToken()).username());
    }

    @ParameterizedTest
    @MethodSource("invalidRegistrations")
    void invalidRegistrationCreatesNoUser(String username, String password, String email) throws Exception {
        var error = assertThrows(ServerException.class, () -> facade.register(username, password, email));
        assertEquals("Please check the information you entered.", error.getMessage());
        assertNull(storage.getUser(username));
    }

    static Stream<Arguments> invalidRegistrations() {
        return Stream.of(
                Arguments.of(null, "secret", "p@example.com"),
                Arguments.of(" ", "secret", "p@example.com"),
                Arguments.of("player", null, "p@example.com"),
                Arguments.of("player", "", "p@example.com"),
                Arguments.of("player", "secret", null),
                Arguments.of("player", "secret", " "));
    }

    @Test
    void failedRegistrationDoesNotPreventLaterRegistration() throws Exception {
        assertThrows(ServerException.class, () -> facade.register("player", "", "p@example.com"));
        var first = facade.register("player", "secret", "p@example.com");
        var second = facade.register("other", "secret", "o@example.com");
        assertNotEquals(first.authToken(), second.authToken());
        assertEquals("player", storage.getAuth(first.authToken()).username());
        assertEquals("other", storage.getAuth(second.authToken()).username());
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-json", "null", "{}", "{\"username\":\"player\"}",
            "{\"username\":\"player\",\"authToken\":\"\"}", "{\"username\":\"other\",\"authToken\":\"token\"}"})
    void invalidRegistrationResponseIsReportedAsFailure(String response) throws Exception {
        try (var invalidServer = new TestHttpServer(200, response)) {
            var invalidFacade = new ServerFacade(invalidServer.port());
            var error = assertThrows(ServerException.class,
                    () -> invalidFacade.register("player", "secret", "p@example.com"));
            assertEquals("The server returned an invalid registration response. Please try again.", error.getMessage());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {302, 500})
    void registrationDoesNotTreatHttpErrorsOrRedirectsAsSuccess(int status) throws Exception {
        try (var failedServer = new TestHttpServer(status, "private server details")) {
            var failedFacade = new ServerFacade(failedServer.port());
            var error = assertThrows(ServerException.class,
                    () -> failedFacade.register("player", "secret", "p@example.com"));
            assertEquals("The server could not complete the request. Please try again.", error.getMessage());
        }
    }

    @Test
    void registrationReportsAnUnavailableServer() throws Exception {
        var disconnected = new ServerFacade(TestHttpServer.stoppedPort());
        var error = assertThrows(ServerException.class,
                () -> disconnected.register("player", "secret", "p@example.com"));
        assertEquals("Unable to reach the server. Please try again.", error.getMessage());
    }

}
