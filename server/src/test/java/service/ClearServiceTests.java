package service;

import dataaccess.MemoryDataAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import request.*;
import result.RegisterResult;

import static org.junit.jupiter.api.Assertions.*;
import static service.ServiceException.Code.*;

class ClearServiceTests {
    private MemoryDataAccess data;
    private UserService users;
    private GameService games;
    private ClearService clear;

    @BeforeEach
    void setUp() {
        data = new MemoryDataAccess();
        users = new UserService(data);
        games = new GameService(data);
        clear = new ClearService(data);
    }

    private RegisterResult register(String username) throws Exception {
        return users.register(new RegisterRequest(username, "password", username + "@example.com"));
    }

    @Test
    void clearRemovesAllUsersSessionsAndJoinedGames() throws Exception {
        var white = register("white");
        var black = register("black");
        var extraSession = users.login(new LoginRequest("white", "password"));
        int game = games.createGame(new CreateGameRequest(white.authToken(), "Match")).gameID();
        int otherGame = games.createGame(new CreateGameRequest(black.authToken(), "Practice")).gameID();
        games.joinGame(new JoinGameRequest(white.authToken(), "WHITE", game));
        games.joinGame(new JoinGameRequest(black.authToken(), "BLACK", game));

        clear.clear();

        assertNull(data.getUser("white"));
        assertNull(data.getUser("black"));
        for (String token : new String[]{white.authToken(), black.authToken(), extraSession.authToken()}) {
            assertNull(data.getAuth(token));
        }
        assertNull(data.getGame(game));
        assertNull(data.getGame(otherGame));
        assertTrue(data.listGames().isEmpty());
    }

    @Test
    void clearIsRepeatableOnAnEmptyStore() {
        assertDoesNotThrow(() -> {
            clear.clear();
            clear.clear();
        });
        assertTrue(data.listGames().isEmpty());
    }

    @Test
    void clearedTokensCannotAccessAnyProtectedService() throws Exception {
        var user = register("player");
        String token = user.authToken();
        int game = games.createGame(new CreateGameRequest(token, "Match")).gameID();
        clear.clear();

        assertEquals(UNAUTHORIZED, assertThrows(ServiceException.class,
                () -> games.listGames(new ListGamesRequest(token))).getCode());
        assertEquals(UNAUTHORIZED, assertThrows(ServiceException.class,
                () -> games.createGame(new CreateGameRequest(token, "New match"))).getCode());
        assertEquals(UNAUTHORIZED, assertThrows(ServiceException.class,
                () -> games.joinGame(new JoinGameRequest(token, "WHITE", game))).getCode());
        assertEquals(UNAUTHORIZED, assertThrows(ServiceException.class,
                () -> users.logout(new LogoutRequest(token))).getCode());
        assertEquals(UNAUTHORIZED, assertThrows(ServiceException.class,
                () -> users.login(new LoginRequest("player", "password"))).getCode());
    }

    @Test
    void registerAndPlayAgainAfterClearWithoutRevivingOldData() throws Exception {
        var old = register("player");
        int oldGame = games.createGame(new CreateGameRequest(old.authToken(), "Match")).gameID();
        clear.clear();

        var fresh = users.register(new RegisterRequest("player", "new password", "new@example.com"));
        assertNotEquals(old.authToken(), fresh.authToken());
        assertTrue(games.listGames(new ListGamesRequest(fresh.authToken())).games().isEmpty());
        assertEquals(UNAUTHORIZED, assertThrows(ServiceException.class,
                () -> users.login(new LoginRequest("player", "password"))).getCode());
        var login = users.login(new LoginRequest("player", "new password"));
        int newGame = games.createGame(new CreateGameRequest(login.authToken(), "Match")).gameID();
        assertNotEquals(oldGame, newGame);
        assertEquals(BAD_REQUEST, assertThrows(ServiceException.class,
                () -> games.joinGame(new JoinGameRequest(login.authToken(), "WHITE", oldGame))).getCode());
        games.joinGame(new JoinGameRequest(login.authToken(), "WHITE", newGame));
        assertEquals("player", data.getGame(newGame).whiteUsername());
        assertNull(data.getAuth(old.authToken()));
    }

    @Test
    void logoutPreservesGameUntilClearRemovesIt() throws Exception {
        var white = register("white");
        var black = register("black");
        int game = games.createGame(new CreateGameRequest(white.authToken(), "Match")).gameID();
        games.joinGame(new JoinGameRequest(white.authToken(), "WHITE", game));
        users.logout(new LogoutRequest(white.authToken()));
        games.joinGame(new JoinGameRequest(black.authToken(), "BLACK", game));
        var listing = games.listGames(new ListGamesRequest(black.authToken())).games();
        assertEquals(1, listing.size());
        var summary = listing.iterator().next();
        assertEquals("white", summary.whiteUsername());
        assertEquals("black", summary.blackUsername());
        assertNotNull(users.login(new LoginRequest("white", "password")).authToken());

        clear.clear();
        assertNull(data.getGame(game));
        assertNull(data.getUser("white"));
        assertNull(data.getUser("black"));
    }

    @Test
    void clearingOneStoreDoesNotClearAnotherServer() throws Exception {
        var otherData = new MemoryDataAccess();
        var otherUsers = new UserService(otherData);
        var otherGames = new GameService(otherData);
        var otherUser = otherUsers.register(new RegisterRequest("player", "password", "other@example.com"));
        int otherGame = otherGames.createGame(new CreateGameRequest(otherUser.authToken(), "Other match")).gameID();
        register("player");

        clear.clear();

        assertNull(data.getUser("player"));
        assertNotNull(otherData.getUser("player"));
        assertNotNull(otherData.getAuth(otherUser.authToken()));
        assertEquals(otherGame, otherGames.listGames(new ListGamesRequest(otherUser.authToken()))
                .games().iterator().next().gameID());
    }
}
