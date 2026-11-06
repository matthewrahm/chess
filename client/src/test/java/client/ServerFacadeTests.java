package client;

import chess.ChessGame;
import com.google.gson.JsonParser;
import dataaccess.sql.SqlDataAccess;
import model.AuthData;
import model.UserData;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
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

    @Test
    void loginCreatesFreshSessionsWithoutReplacingCredentialsOrEarlierSessions() throws Exception {
        var registered = facade.register("player", "secret", "p@example.com");
        var original = storage.getUser("player");

        var first = facade.login("player", "secret");
        var second = facade.login("player", "secret");

        assertEquals("player", first.username());
        assertNotEquals(registered.authToken(), first.authToken());
        assertNotEquals(first.authToken(), second.authToken());
        assertEquals(new AuthData(first.authToken(), "player"), storage.getAuth(first.authToken()));
        assertEquals(new AuthData(second.authToken(), "player"), storage.getAuth(second.authToken()));
        assertNotNull(storage.getAuth(registered.authToken()));
        assertEquals(original, storage.getUser("player"));
    }

    @Test
    void loginPreservesUnicodeAndJsonPunctuation() throws Exception {
        String username = "Zoë 王";
        String password = "a\\b\"c";
        facade.register(username, password, "p@example.com");
        var result = facade.login(username, password);
        assertEquals(username, result.username());
        assertEquals(username, storage.getAuth(result.authToken()).username());
    }

    @ParameterizedTest
    @ValueSource(strings = {"player", "unknown"})
    void incorrectCredentialsPreserveExistingSessionAndAllowRetry(String username) throws Exception {
        var registered = facade.register("player", "secret", "p@example.com");
        var original = storage.getUser("player");

        var error = assertThrows(ServerException.class, () -> facade.login(username, "incorrect"));

        assertEquals("Incorrect username or password.", error.getMessage());
        assertEquals(original, storage.getUser("player"));
        assertNotNull(storage.getAuth(registered.authToken()));
        assertEquals("player", facade.login("player", "secret").username());
    }

    @ParameterizedTest
    @MethodSource("invalidLogins")
    void missingLoginFieldsAreReportedWithoutChangingCredentials(String username, String password) throws Exception {
        facade.register("player", "secret", "p@example.com");
        var original = storage.getUser("player");
        var error = assertThrows(ServerException.class, () -> facade.login(username, password));
        assertEquals("Please check the information you entered.", error.getMessage());
        assertEquals(original, storage.getUser("player"));
    }

    static Stream<Arguments> invalidLogins() {
        return Stream.of(Arguments.of(null, "secret"), Arguments.of(" ", "secret"),
                Arguments.of("player", null), Arguments.of("player", ""));
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-json", "null", "{}", "{\"username\":\"player\"}",
            "{\"username\":\"player\",\"authToken\":\" \"}",
            "{\"username\":\"other\",\"authToken\":\"token\"}"})
    void invalidLoginResponseDoesNotReturnASession(String response) throws Exception {
        try (var invalidServer = new TestHttpServer(200, response)) {
            var invalidFacade = new ServerFacade(invalidServer.port());
            var error = assertThrows(ServerException.class, () -> invalidFacade.login("player", "secret"));
            assertEquals("The server returned an invalid login response. Please try again.", error.getMessage());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {302, 500})
    void loginDoesNotTreatHttpErrorsOrRedirectsAsSuccess(int status) throws Exception {
        try (var failedServer = new TestHttpServer(status, "private server details")) {
            var failedFacade = new ServerFacade(failedServer.port());
            var error = assertThrows(ServerException.class, () -> failedFacade.login("player", "secret"));
            assertEquals("The server could not complete the request. Please try again.", error.getMessage());
        }
    }

    @Test
    void loginReportsAnUnavailableServer() throws Exception {
        var disconnected = new ServerFacade(TestHttpServer.stoppedPort());
        var error = assertThrows(ServerException.class, () -> disconnected.login("player", "secret"));
        assertEquals("Unable to reach the server. Please try again.", error.getMessage());
    }

    @Test
    void logoutRevokesOnlyTheRequestedSessionAndPreservesStoredData() throws Exception {
        var registered = facade.register("player", "secret", "p@example.com");
        var login = facade.login("player", "secret");
        var other = facade.register("other", "secret", "o@example.com");
        var user = storage.getUser("player");
        int gameId = storage.createGame("Practice", new ChessGame());
        var game = storage.getGame(gameId);

        facade.logout(login.authToken());

        assertNull(storage.getAuth(login.authToken()));
        assertNotNull(storage.getAuth(registered.authToken()));
        assertNotNull(storage.getAuth(other.authToken()));
        assertEquals(user, storage.getUser("player"));
        assertEquals(game, storage.getGame(gameId));
        var error = assertThrows(ServerException.class, () -> facade.logout(login.authToken()));
        assertEquals("Your session is no longer valid. Please log in again.", error.getMessage());
    }

    @Test
    void logoutAcceptsRegistrationTokenAndAllowsFreshLogin() throws Exception {
        var registered = facade.register("player", "secret", "p@example.com");
        facade.logout(registered.authToken());
        var loggedIn = facade.login("player", "secret");

        assertNull(storage.getAuth(registered.authToken()));
        assertNotEquals(registered.authToken(), loggedIn.authToken());
        assertEquals("player", storage.getAuth(loggedIn.authToken()).username());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "unknown", "token\nInjected", "token\rInjected"})
    void invalidLogoutPreservesValidSessionsAndAllowsRetry(String token) throws Exception {
        var registered = facade.register("player", "secret", "p@example.com");

        var error = assertThrows(ServerException.class, () -> facade.logout(token));

        assertEquals("Your session is no longer valid. Please log in again.", error.getMessage());
        assertNotNull(storage.getAuth(registered.authToken()));
        facade.logout(registered.authToken());
        assertNull(storage.getAuth(registered.authToken()));
    }

    @ParameterizedTest
    @ValueSource(ints = {302, 500})
    void logoutDoesNotTreatHttpErrorsOrRedirectsAsSuccess(int status) throws Exception {
        var registered = facade.register("player", "secret", "p@example.com");
        try (var failedServer = new TestHttpServer(status, "private server details")) {
            var failedFacade = new ServerFacade(failedServer.port());
            var error = assertThrows(ServerException.class, () -> failedFacade.logout(registered.authToken()));
            assertEquals("The server could not complete the request. Please try again.", error.getMessage());
            assertNotNull(storage.getAuth(registered.authToken()));
        }
    }

    @Test
    void unavailableServerLeavesSessionUsableForLogoutRetry() throws Exception {
        var registered = facade.register("player", "secret", "p@example.com");
        var disconnected = new ServerFacade(TestHttpServer.stoppedPort());

        var error = assertThrows(ServerException.class, () -> disconnected.logout(registered.authToken()));

        assertEquals("Unable to reach the server. Please try again.", error.getMessage());
        assertNotNull(storage.getAuth(registered.authToken()));
        facade.logout(registered.authToken());
        assertNull(storage.getAuth(registered.authToken()));
    }

    @Test
    void createGameStoresInitialBoardWithoutJoiningEitherSeat() throws Exception {
        var auth = facade.register("player", "secret", "p@example.com");
        String name = "Practice \"王\"";
        var result = facade.createGame(auth.authToken(), name);
        var game = storage.getGame(result.gameID());

        assertTrue(result.gameID() > 0);
        assertEquals(name, game.gameName());
        assertNull(game.whiteUsername());
        assertNull(game.blackUsername());
        assertEquals(new ChessGame(), game.game());
        assertNotNull(storage.getAuth(auth.authToken()));
    }

    @Test
    void duplicateGameNamesHaveIndependentIds() throws Exception {
        var auth = facade.register("player", "secret", "p@example.com");
        var first = facade.createGame(auth.authToken(), "Practice");
        var second = facade.createGame(auth.authToken(), "Practice");
        assertNotEquals(first.gameID(), second.gameID());
        assertEquals(2, storage.listGames().size());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "unknown", "token\nInjected", "token\rInjected"})
    void invalidSessionCannotCreateGame(String token) throws Exception {
        var error = assertThrows(ServerException.class, () -> facade.createGame(token, "Practice"));
        assertEquals("Your session is no longer valid. Please log in again.", error.getMessage());
        assertTrue(storage.listGames().isEmpty());
    }

    @Test
    void revokedSessionCannotCreateGameButFreshLoginCan() throws Exception {
        var auth = facade.register("player", "secret", "p@example.com");
        facade.logout(auth.authToken());
        assertThrows(ServerException.class, () -> facade.createGame(auth.authToken(), "Practice"));
        assertTrue(storage.listGames().isEmpty());
        var login = facade.login("player", "secret");
        assertNotNull(storage.getGame(facade.createGame(login.authToken(), "Practice").gameID()));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void invalidGameNameDoesNotCreateRows(String name) throws Exception {
        var auth = facade.register("player", "secret", "p@example.com");
        var error = assertThrows(ServerException.class, () -> facade.createGame(auth.authToken(), name));
        assertEquals("Please check the information you entered.", error.getMessage());
        assertTrue(storage.listGames().isEmpty());
        assertTrue(facade.createGame(auth.authToken(), "Retry").gameID() > 0);
    }

    @Test
    void createGameSendsTokenOnlyInAuthorizationHeader() throws Exception {
        try (var http = new TestHttpServer(200, "{\"gameID\":42}")) {
            var client = new ServerFacade(http.port());
            assertEquals(42, client.createGame("private-token", "Practice").gameID());
            var request = http.request();
            assertEquals("POST", request.method());
            assertEquals("/game", request.path());
            assertEquals("private-token", request.authorization());
            var json = JsonParser.parseString(request.body()).getAsJsonObject();
            assertEquals(1, json.size());
            assertEquals("Practice", json.get("gameName").getAsString());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-json", "null", "{}", "{\"gameID\":0}", "{\"gameID\":-1}",
            "{\"gameID\":\"invalid\"}"})
    void invalidCreateResponseIsNotTreatedAsSuccess(String response) throws Exception {
        try (var http = new TestHttpServer(200, response)) {
            var client = new ServerFacade(http.port());
            var error = assertThrows(ServerException.class, () -> client.createGame("token", "Practice"));
            assertEquals("The server returned an invalid create-game response. Please try again.", error.getMessage());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {302, 500})
    void createGameReportsHttpFailureWithoutLeakingServerDetails(int status) throws Exception {
        try (var http = new TestHttpServer(status, "private database details")) {
            var client = new ServerFacade(http.port());
            var error = assertThrows(ServerException.class, () -> client.createGame("token", "Practice"));
            assertEquals("The server could not complete the request. Please try again.", error.getMessage());
        }
    }

    @Test
    void createGameReportsAnUnavailableServer() throws Exception {
        var client = new ServerFacade(TestHttpServer.stoppedPort());
        var error = assertThrows(ServerException.class, () -> client.createGame("token", "Practice"));
        assertEquals("Unable to reach the server. Please try again.", error.getMessage());
    }

}
