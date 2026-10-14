package service;

import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPosition;
import com.google.gson.GsonBuilder;
import dataaccess.MemoryDataAccess;
import model.AuthData;
import model.GameData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import request.CreateGameRequest;
import request.ListGamesRequest;
import request.LogoutRequest;
import result.GameSummary;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static service.ServiceException.Code.*;

class GameServiceTests {
    private MemoryDataAccess data;
    private GameService service;
    private static final String TOKEN = "matthew-session";
    private static final String OTHER_TOKEN = "other-session";

    @BeforeEach
    void setUp() throws Exception {
        data = new MemoryDataAccess();
        data.createAuth(new AuthData(TOKEN, "matthew"));
        data.createAuth(new AuthData(OTHER_TOKEN, "other"));
        service = new GameService(data);
    }

    @Test
    void createStoresStandardGameWithEmptySeats() throws Exception {
        var result = service.createGame(new CreateGameRequest(TOKEN, "Practice"));
        var stored = data.getGame(result.gameID());
        assertNotNull(stored);
        assertEquals("Practice", stored.gameName());
        assertNull(stored.whiteUsername());
        assertNull(stored.blackUsername());
        assertEquals(new ChessGame().getBoard(), stored.game().getBoard());
        assertEquals(ChessGame.TeamColor.WHITE, stored.game().getTeamTurn());
        var json = new GsonBuilder().create().toJsonTree(result).getAsJsonObject();
        assertEquals(Set.of("gameID"), json.keySet());
        assertEquals(result.gameID(), json.get("gameID").getAsInt());
    }

    @Test
    void repeatedNamesHaveUniqueIdsAndIndependentBoards() throws Exception {
        var first = service.createGame(new CreateGameRequest(TOKEN, "Practice"));
        var second = service.createGame(new CreateGameRequest(OTHER_TOKEN, "Practice"));
        assertNotEquals(first.gameID(), second.gameID());
        var firstGame = data.getGame(first.gameID()).game();
        var secondGame = data.getGame(second.gameID()).game();
        assertNotSame(firstGame, secondGame);
        firstGame.makeMove(new ChessMove(new ChessPosition(2, 5), new ChessPosition(4, 5), null));
        assertEquals(new ChessGame().getBoard(), secondGame.getBoard());
        assertEquals(ChessGame.TeamColor.WHITE, secondGame.getTeamTurn());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void missingGameNameIsBadRequestWithoutCreatingGame(String name) {
        var error = assertThrows(ServiceException.class, () -> service.createGame(new CreateGameRequest(TOKEN, name)));
        assertEquals(BAD_REQUEST, error.getCode());
        assertTrue(data.listGames().isEmpty());
    }

    @Test
    void nullCreateRequestIsBadRequest() {
        var error = assertThrows(ServiceException.class, () -> service.createGame(null));
        assertEquals(BAD_REQUEST, error.getCode());
        assertTrue(data.listGames().isEmpty());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "unknown-token"})
    void unauthorizedCallerCannotCreateOrListGames(String token) {
        var createError = assertThrows(ServiceException.class,
                () -> service.createGame(new CreateGameRequest(token, "Practice")));
        var listError = assertThrows(ServiceException.class, () -> service.listGames(new ListGamesRequest(token)));
        assertEquals(UNAUTHORIZED, createError.getCode());
        assertEquals(UNAUTHORIZED, listError.getCode());
        assertTrue(data.listGames().isEmpty());
    }

    @Test
    void authorizationIsCheckedBeforeCreateBodyFields() {
        var error = assertThrows(ServiceException.class,
                () -> service.createGame(new CreateGameRequest("unknown-token", null)));
        assertEquals(UNAUTHORIZED, error.getCode());
    }

    @Test
    void nullListRequestIsUnauthorized() {
        var error = assertThrows(ServiceException.class, () -> service.listGames(null));
        assertEquals(UNAUTHORIZED, error.getCode());
    }

    @Test
    void emptyStoreListsEmptyArray() throws Exception {
        var result = service.listGames(new ListGamesRequest(TOKEN));
        assertNotNull(result.games());
        assertTrue(result.games().isEmpty());
        assertEquals("{\"games\":[]}", new GsonBuilder().create().toJson(result));
    }

    @Test
    void listIncludesAllUsersGamesAndOnlySummaryFields() throws Exception {
        int first = service.createGame(new CreateGameRequest(TOKEN, "First")).gameID();
        int second = service.createGame(new CreateGameRequest(OTHER_TOKEN, "Second")).gameID();
        var original = data.getGame(first);
        var occupied = new GameData(first, "white-player", "black-player", "First", original.game());
        data.updateGame(occupied);

        var result = service.listGames(new ListGamesRequest(OTHER_TOKEN));

        assertEquals(Set.of(new GameSummary(first, "white-player", "black-player", "First"),
                new GameSummary(second, null, null, "Second")), Set.copyOf(result.games()));
        assertEquals(occupied, data.getGame(first));
        var json = new GsonBuilder().serializeNulls().create().toJsonTree(result).getAsJsonObject();
        assertEquals(Set.of("games"), json.keySet());
        for (var entry : json.getAsJsonArray("games")) {
            assertEquals(Set.of("gameID", "whiteUsername", "blackUsername", "gameName"), entry.getAsJsonObject().keySet());
        }
    }

    @Test
    void listedSummariesAreASnapshot() throws Exception {
        int gameID = service.createGame(new CreateGameRequest(TOKEN, "Practice")).gameID();
        var result = service.listGames(new ListGamesRequest(TOKEN));
        data.updateGame(new GameData(gameID, "matthew", null, "Practice", data.getGame(gameID).game()));
        assertNull(result.games().iterator().next().whiteUsername());
        assertEquals("matthew", service.listGames(new ListGamesRequest(TOKEN)).games().iterator().next().whiteUsername());
    }

    @Test
    void loggedOutTokenLosesAccessWhileAnotherSessionRemainsValid() throws Exception {
        new UserService(data).logout(new LogoutRequest(TOKEN));
        assertEquals(UNAUTHORIZED, assertThrows(ServiceException.class,
                () -> service.createGame(new CreateGameRequest(TOKEN, "Practice"))).getCode());
        assertEquals(UNAUTHORIZED, assertThrows(ServiceException.class,
                () -> service.listGames(new ListGamesRequest(TOKEN))).getCode());
        assertNotNull(service.createGame(new CreateGameRequest(OTHER_TOKEN, "Still authorized")));
        assertEquals(1, service.listGames(new ListGamesRequest(OTHER_TOKEN)).games().size());
    }
}
