package service;

import dataaccess.MemoryAuthDAO;
import dataaccess.MemoryGameDAO;
import dataaccess.MemoryUserDAO;
import model.AuthData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ServiceTests {
    private MemoryUserDAO userDAO;
    private MemoryAuthDAO authDAO;
    private MemoryGameDAO gameDAO;
    private AuthHelper authHelper;
    private UserService userService;
    private GameService gameService;
    private ClearService clearService;

    @BeforeEach
    void setUp() {
        userDAO = new MemoryUserDAO();
        authDAO = new MemoryAuthDAO();
        gameDAO = new MemoryGameDAO();
        authHelper = new AuthHelper(authDAO);
        userService = new UserService(userDAO, authDAO, authHelper);
        gameService = new GameService(gameDAO, authHelper);
        clearService = new ClearService(userDAO, authDAO, gameDAO);
    }

    @Test
    void clearSuccess() throws Exception {
        userService.register("user1", "pass1", "e@mail.com");
        clearService.clear();
        AuthData auth = userService.register("user1", "pass1", "e@mail.com");
        assertNotNull(auth);
    }

    @Test
    void registerSuccess() throws Exception {
        AuthData auth = userService.register("testUser", "testPass", "test@mail.com");
        assertNotNull(auth.authToken());
        assertEquals("testUser", auth.username());
    }

    @Test
    void registerDuplicateUser() throws Exception {
        userService.register("testUser", "testPass", "test@mail.com");
        ServiceException ex = assertThrows(ServiceException.class, () ->
                userService.register("testUser", "testPass", "test@mail.com"));
        assertEquals(403, ex.getStatusCode());
    }

    @Test
    void loginSuccess() throws Exception {
        userService.register("testUser", "testPass", "test@mail.com");
        AuthData auth = userService.login("testUser", "testPass");
        assertNotNull(auth.authToken());
        assertEquals("testUser", auth.username());
    }

    @Test
    void loginWrongPassword() throws Exception {
        userService.register("testUser", "testPass", "test@mail.com");
        ServiceException ex = assertThrows(ServiceException.class, () ->
                userService.login("testUser", "wrongPass"));
        assertEquals(401, ex.getStatusCode());
    }

    @Test
    void logoutSuccess() throws Exception {
        AuthData auth = userService.register("testUser", "testPass", "test@mail.com");
        assertDoesNotThrow(() -> userService.logout(auth.authToken()));
    }

    @Test
    void logoutInvalidToken() {
        ServiceException ex = assertThrows(ServiceException.class, () ->
                userService.logout("badToken"));
        assertEquals(401, ex.getStatusCode());
    }

    @Test
    void listGamesSuccess() throws Exception {
        AuthData auth = userService.register("testUser", "testPass", "test@mail.com");
        gameService.createGame(auth.authToken(), "Game 1");
        gameService.createGame(auth.authToken(), "Game 2");
        var games = gameService.listGames(auth.authToken());
        assertEquals(2, games.size());
    }

    @Test
    void listGamesUnauthorized() {
        ServiceException ex = assertThrows(ServiceException.class, () ->
                gameService.listGames("badToken"));
        assertEquals(401, ex.getStatusCode());
    }

    @Test
    void createGameSuccess() throws Exception {
        AuthData auth = userService.register("testUser", "testPass", "test@mail.com");
        int gameID = gameService.createGame(auth.authToken(), "My Game");
        assertTrue(gameID > 0);
    }

    @Test
    void createGameUnauthorized() {
        ServiceException ex = assertThrows(ServiceException.class, () ->
                gameService.createGame("badToken", "My Game"));
        assertEquals(401, ex.getStatusCode());
    }

    @Test
    void joinGameSuccess() throws Exception {
        AuthData auth = userService.register("testUser", "testPass", "test@mail.com");
        int gameID = gameService.createGame(auth.authToken(), "My Game");
        assertDoesNotThrow(() -> gameService.joinGame(auth.authToken(), "WHITE", gameID));
    }

    @Test
    void joinGameAlreadyTaken() throws Exception {
        AuthData auth1 = userService.register("user1", "pass1", "e1@mail.com");
        AuthData auth2 = userService.register("user2", "pass2", "e2@mail.com");
        int gameID = gameService.createGame(auth1.authToken(), "My Game");
        gameService.joinGame(auth1.authToken(), "WHITE", gameID);
        ServiceException ex = assertThrows(ServiceException.class, () ->
                gameService.joinGame(auth2.authToken(), "WHITE", gameID));
        assertEquals(403, ex.getStatusCode());
    }
}
