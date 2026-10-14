package service;

import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPosition;
import com.google.gson.Gson;
import dataaccess.DataAccessException;
import dataaccess.MemoryDataAccess;
import model.AuthData;
import model.GameData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import request.CreateGameRequest;
import request.JoinGameRequest;
import request.ListGamesRequest;
import request.LogoutRequest;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static service.ServiceException.Code.*;

class JoinGameServiceTests {
    private MemoryDataAccess data;
    private GameService service;
    private int gameID;
    private static final String TOKEN = "matthew-session";
    private static final String OTHER_TOKEN = "other-session";

    @BeforeEach
    void setUp() throws Exception {
        data = new MemoryDataAccess();
        data.createAuth(new AuthData(TOKEN, "matthew"));
        data.createAuth(new AuthData(OTHER_TOKEN, "other"));
        service = new GameService(data);
        gameID = service.createGame(new CreateGameRequest(TOKEN, "Practice")).gameID();
    }

    @ParameterizedTest
    @ValueSource(strings = {"WHITE", "BLACK"})
    void joinPreservesOtherSeatGameNameAndPosition(String color) throws Exception {
        var position = data.getGame(gameID).game();
        position.makeMove(new ChessMove(new ChessPosition(2, 5), new ChessPosition(4, 5), null));
        boolean white = color.equals("WHITE");
        var before = new GameData(gameID, white ? null : "existing-white",
                white ? "existing-black" : null, "Practice", position);
        data.updateGame(before);
        String originalPosition = new Gson().toJson(position);
        int otherID = service.createGame(new CreateGameRequest(OTHER_TOKEN, "Other game")).gameID();
        var otherGame = data.getGame(otherID);

        service.joinGame(new JoinGameRequest(TOKEN, color, gameID));

        var joined = data.getGame(gameID);
        assertEquals(gameID, joined.gameID());
        assertEquals("Practice", joined.gameName());
        assertEquals(white ? "matthew" : "existing-white", joined.whiteUsername());
        assertEquals(white ? "existing-black" : "matthew", joined.blackUsername());
        assertEquals(originalPosition, new Gson().toJson(joined.game()));
        assertEquals(otherGame, data.getGame(otherID));
        var summary = service.listGames(new ListGamesRequest(TOKEN)).games().stream()
                .filter(game -> game.gameID() == gameID).findFirst().orElseThrow();
        assertEquals(joined.whiteUsername(), summary.whiteUsername());
        assertEquals(joined.blackUsername(), summary.blackUsername());
    }

    @Test
    void sameUserCanJoinBothDifferentSeats() throws Exception {
        service.joinGame(new JoinGameRequest(TOKEN, "WHITE", gameID));
        service.joinGame(new JoinGameRequest(TOKEN, "BLACK", gameID));
        assertEquals("matthew", data.getGame(gameID).whiteUsername());
        assertEquals("matthew", data.getGame(gameID).blackUsername());
    }

    @ParameterizedTest
    @ValueSource(strings = {"WHITE", "BLACK"})
    void occupiedSeatRejectsOtherPlayerAndRepeatedJoin(String color) throws Exception {
        service.joinGame(new JoinGameRequest(TOKEN, color, gameID));
        var before = data.getGame(gameID);
        for (String token : List.of(OTHER_TOKEN, TOKEN)) {
            var error = assertThrows(ServiceException.class,
                    () -> service.joinGame(new JoinGameRequest(token, color, gameID)));
            assertEquals(ALREADY_TAKEN, error.getCode());
            assertEquals(before, data.getGame(gameID));
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "unknown-token"})
    void invalidAuthorizationCannotClaimSeat(String token) {
        var before = data.getGame(gameID);
        var error = assertThrows(ServiceException.class,
                () -> service.joinGame(new JoinGameRequest(token, "WHITE", gameID)));
        assertEquals(UNAUTHORIZED, error.getCode());
        assertEquals(before, data.getGame(gameID));
    }

    @Test
    void loggedOutTokenCannotClaimSeat() throws Exception {
        new UserService(data).logout(new LogoutRequest(TOKEN));
        var error = assertThrows(ServiceException.class,
                () -> service.joinGame(new JoinGameRequest(TOKEN, "WHITE", gameID)));
        assertEquals(UNAUTHORIZED, error.getCode());
        assertNull(data.getGame(gameID).whiteUsername());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"GREEN", "white", "black", "WHITE "})
    void invalidColorIsBadRequestWithoutChangingGame(String color) {
        var before = data.getGame(gameID);
        var error = assertThrows(ServiceException.class,
                () -> service.joinGame(new JoinGameRequest(TOKEN, color, gameID)));
        assertEquals(BAD_REQUEST, error.getCode());
        assertEquals(before, data.getGame(gameID));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0, -1, 999})
    void missingOrUnknownGameIsBadRequest(Integer id) {
        var before = data.getGame(gameID);
        var error = assertThrows(ServiceException.class,
                () -> service.joinGame(new JoinGameRequest(TOKEN, "WHITE", id)));
        assertEquals(BAD_REQUEST, error.getCode());
        assertEquals(before, data.getGame(gameID));
        assertEquals(1, data.listGames().size());
    }

    @Test
    void nullRequestIsBadRequest() {
        var error = assertThrows(ServiceException.class, () -> service.joinGame(null));
        assertEquals(BAD_REQUEST, error.getCode());
    }

    @Test
    void authorizationIsCheckedBeforeGameAndColorValidation() {
        var error = assertThrows(ServiceException.class,
                () -> service.joinGame(new JoinGameRequest("unknown-token", null, null)));
        assertEquals(UNAUTHORIZED, error.getCode());
    }

    @Test
    void failedUpdatePropagatesWithoutChangingStoredSeats() throws Exception {
        var failure = new DataAccessException("Simulated game update failure");
        var failing = new MemoryDataAccess() {
            @Override
            public void updateGame(GameData game) throws DataAccessException {
                throw failure;
            }
        };
        failing.createAuth(new AuthData(TOKEN, "matthew"));
        int id = failing.createGame("Practice", new ChessGame());
        var before = failing.getGame(id);
        assertSame(failure, assertThrows(DataAccessException.class,
                () -> new GameService(failing).joinGame(new JoinGameRequest(TOKEN, "WHITE", id))));
        assertEquals(before, failing.getGame(id));
    }

    @Test
    void concurrentPlayersCannotBothClaimTheSameSeat() throws Exception {
        var outcomes = joinTogether("WHITE", "WHITE");
        assertEquals(Set.of("joined", "ALREADY_TAKEN"), Set.copyOf(outcomes));
        assertTrue(Set.of("matthew", "other").contains(data.getGame(gameID).whiteUsername()));
        assertNull(data.getGame(gameID).blackUsername());
    }

    @Test
    void concurrentJoinsToDifferentSeatsPreserveBothPlayers() throws Exception {
        assertEquals(List.of("joined", "joined"), joinTogether("WHITE", "BLACK"));
        assertEquals("matthew", data.getGame(gameID).whiteUsername());
        assertEquals("other", data.getGame(gameID).blackUsername());
    }

    private List<String> joinTogether(String firstColor, String secondColor) throws Exception {
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(joinTask(TOKEN, firstColor, ready, start));
            var second = executor.submit(joinTask(OTHER_TOKEN, secondColor, ready, start));
            try {
                assertTrue(ready.await(5, TimeUnit.SECONDS));
            } finally {
                start.countDown();
            }
            return List.of(first.get(5, TimeUnit.SECONDS), second.get(5, TimeUnit.SECONDS));
        }
    }

    private Callable<String> joinTask(String token, String color, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            assertTrue(start.await(5, TimeUnit.SECONDS));
            try {
                new GameService(data).joinGame(new JoinGameRequest(token, color, gameID));
                return "joined";
            } catch (ServiceException e) {
                return e.getCode().name();
            }
        };
    }
}
