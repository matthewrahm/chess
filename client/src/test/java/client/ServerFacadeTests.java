package client;

import chess.ChessGame;
import com.google.gson.JsonParser;
import model.AuthData;
import model.GameData;
import result.GameSummary;
import model.UserData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import org.junit.jupiter.params.provider.MethodSource;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/** Uses the configured disposable database; run with scripts/verify_tests.py. */
public class ServerFacadeTests extends ServerFacadeTestSupport {
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

    @Test
    void listGamesReturnsAnEmptyCollectionForAnEmptyDatabase() throws Exception {
        var auth = facade.register("player", "secret", "p@example.com");
        assertTrue(facade.listGames(auth.authToken()).games().isEmpty());
    }

    @Test
    void listGamesReturnsAllNamesIdsAndPlayerSeatsForAnySignedInUser() throws Exception {
        var auth = facade.register("player", "secret", "p@example.com");
        var observer = facade.register("observer", "secret", "o@example.com");
        String name = "Practice \"王\"";
        int firstId = facade.createGame(auth.authToken(), name).gameID();
        int secondId = facade.createGame(auth.authToken(), name).gameID();
        int thirdId = facade.createGame(auth.authToken(), "Open").gameID();
        storage.updateGame(new GameData(firstId, "player", "opponent", name, new ChessGame()));
        storage.updateGame(new GameData(secondId, null, "opponent", name, new ChessGame()));

        var games = facade.listGames(observer.authToken()).games().stream()
                .collect(Collectors.toMap(GameSummary::gameID, Function.identity()));

        assertEquals(Map.of(
                firstId, new GameSummary(firstId, "player", "opponent", name),
                secondId, new GameSummary(secondId, null, "opponent", name),
                thirdId, new GameSummary(thirdId, null, null, "Open")), games);
    }

    @Test
    void listGamesReadsFreshDataWithoutChangingEarlierResults() throws Exception {
        var auth = facade.register("player", "secret", "p@example.com");
        int id = facade.createGame(auth.authToken(), "Practice").gameID();
        var earlier = facade.listGames(auth.authToken());
        storage.updateGame(new GameData(id, "player", null, "Practice", new ChessGame()));

        var current = facade.listGames(auth.authToken());

        assertNull(earlier.games().iterator().next().whiteUsername());
        assertEquals("player", current.games().iterator().next().whiteUsername());
        assertThrows(UnsupportedOperationException.class, () -> current.games().clear());
        assertEquals(1, storage.listGames().size());
        facade.clear();
        var fresh = facade.register("player", "secret", "p@example.com");
        assertTrue(facade.listGames(fresh.authToken()).games().isEmpty());
    }

    @Test
    void revokedSessionCannotListGamesButFreshLoginCan() throws Exception {
        var auth = facade.register("player", "secret", "p@example.com");
        int id = facade.createGame(auth.authToken(), "Practice").gameID();
        facade.logout(auth.authToken());
        var error = assertThrows(ServerException.class, () -> facade.listGames(auth.authToken()));
        assertEquals("Your session is no longer valid. Please log in again.", error.getMessage());
        var login = facade.login("player", "secret");
        assertEquals(id, facade.listGames(login.authToken()).games().iterator().next().gameID());
    }

    @Test
    void listGamesUsesGetWithAuthorizationAndNoBody() throws Exception {
        try (var http = new TestHttpServer(200, "{\"games\":[]}")) {
            var client = new ServerFacade(http.port());
            assertTrue(client.listGames("private-token").games().isEmpty());
            var request = http.request();
            assertEquals("GET", request.method());
            assertEquals("/game", request.path());
            assertEquals("private-token", request.authorization());
            assertEquals("", request.body());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "not-json",
            "null",
            "{}",
            "{\"games\":null}",
            "{\"games\":{}}",
            "{\"games\":[null]}",
            "{\"games\":[{\"gameID\":0,\"gameName\":\"Practice\"}]}",
            "{\"games\":[{\"gameID\":1}]}",
            "{\"games\":[{\"gameID\":1,\"gameName\":\" \"}]}"})
    void invalidGameListIsNotReturnedAsAnEmptyOrUsableList(String response) throws Exception {
        try (var http = new TestHttpServer(200, response)) {
            var client = new ServerFacade(http.port());
            var error = assertThrows(ServerException.class, () -> client.listGames("token"));
            assertEquals("The server returned an invalid game-list response. Please try again.", error.getMessage());
        }
    }

    @ParameterizedTest
    @MethodSource("httpFailures")
    void httpFailuresDoNotLeakServerDetails(String operation, int status) throws Exception {
        try (var http = new TestHttpServer(status, "private database details")) {
            var client = new ServerFacade(http.port());
            var error = assertThrows(ServerException.class, () -> callFacade(client, operation));
            assertEquals("The server could not complete the request. Please try again.", error.getMessage());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"clear", "register", "login", "create", "list", "join"})
    void unavailableServerIsReportedForEachOperation(String operation) throws Exception {
        var client = new ServerFacade(TestHttpServer.stoppedPort());
        var error = assertThrows(ServerException.class, () -> callFacade(client, operation));
        assertEquals("Unable to reach the server. Please try again.", error.getMessage());
    }

    @ParameterizedTest
    @MethodSource("invalidSessions")
    void invalidSessionsPreserveGamesAndValidSessions(String operation, String token) throws Exception {
        var auth = facade.register("player", "secret", "p@example.com");
        int id = facade.createGame(auth.authToken(), "Practice").gameID();
        var original = storage.getGame(id);
        var error = assertThrows(SessionExpiredException.class, () -> authenticatedCall(operation, token, id));
        assertEquals("Your session is no longer valid. Please log in again.", error.getMessage());
        assertEquals(original, storage.getGame(id));
        assertEquals(1, storage.listGames().size());
        assertNotNull(storage.getAuth(auth.authToken()));
        assertDoesNotThrow(() -> authenticatedCall(operation, auth.authToken(), id));
    }

    @ParameterizedTest
    @ValueSource(strings = {"WHITE", "BLACK"})
    void joinPreservesPositionAndOtherSeatAndRejectsOccupiedSeats(String color) throws Exception {
        var player = facade.register("player", "secret", "p@example.com");
        var other = facade.register("other", "secret", "o@example.com");
        int id = facade.createGame(player.authToken(), "Practice").gameID();
        var position = new ChessGame();
        position.setTeamTurn(ChessGame.TeamColor.BLACK);
        storage.updateGame(new GameData(id, null, null, "Practice", position));
        facade.joinGame(player.authToken(), color, id);
        var joined = storage.getGame(id);
        assertEquals("player", color.equals("WHITE") ? joined.whiteUsername() : joined.blackUsername());
        assertEquals(position, joined.game());
        var error = assertThrows(ServerException.class, () -> facade.joinGame(other.authToken(), color, id));
        assertEquals("That seat is already taken. Please choose another color or game.", error.getMessage());
        assertThrows(ServerException.class, () -> facade.joinGame(player.authToken(), color, id));
        assertEquals(joined, storage.getGame(id));
        facade.joinGame(other.authToken(), color.equals("WHITE") ? "BLACK" : "WHITE", id);
        var full = facade.listGames(player.authToken()).games().iterator().next();
        assertEquals(color.equals("WHITE") ? "player" : "other", full.whiteUsername());
        assertEquals(color.equals("BLACK") ? "player" : "other", full.blackUsername());
        assertEquals(position, storage.getGame(id).game());
    }

    @ParameterizedTest
    @MethodSource("invalidJoins")
    void invalidJoinDoesNotChangeExistingGame(String color, Integer id) throws Exception {
        var auth = facade.register("player", "secret", "p@example.com");
        int existingId = facade.createGame(auth.authToken(), "Practice").gameID();
        var original = storage.getGame(existingId);
        var error = assertThrows(ServerException.class, () -> facade.joinGame(auth.authToken(), color, id));
        assertEquals("Please check the information you entered.", error.getMessage());
        assertEquals(original, storage.getGame(existingId));
    }

    @Test
    void revokedSessionCannotJoinButFreshLoginCan() throws Exception {
        var auth = facade.register("player", "secret", "p@example.com");
        int id = facade.createGame(auth.authToken(), "Practice").gameID();
        facade.logout(auth.authToken());
        assertThrows(ServerException.class, () -> facade.joinGame(auth.authToken(), "WHITE", id));
        assertNull(storage.getGame(id).whiteUsername());
        var login = facade.login("player", "secret");
        facade.joinGame(login.authToken(), "WHITE", id);
        assertEquals("player", storage.getGame(id).whiteUsername());
    }

    @Test
    void joinSendsPutWithColorAndGameIdAndTokenOnlyInHeader() throws Exception {
        try (var http = new TestHttpServer(200, "{}")) {
            assertDoesNotThrow(() -> new ServerFacade(http.port()).joinGame("private-token", "BLACK", 42));
            var request = http.request();
            assertEquals("PUT", request.method());
            assertEquals("/game", request.path());
            assertEquals("private-token", request.authorization());
            var json = JsonParser.parseString(request.body()).getAsJsonObject();
            assertEquals(2, json.size());
            assertEquals("BLACK", json.get("playerColor").getAsString());
            assertEquals(42, json.get("gameID").getAsInt());
        }
    }

}
