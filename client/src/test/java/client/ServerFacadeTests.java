package client;

import org.junit.jupiter.api.*;
import server.Server;

import static org.junit.jupiter.api.Assertions.*;

public class ServerFacadeTests {

    private static Server server;
    static ServerFacade facade;

    @BeforeAll
    public static void init() {
        server = new Server();
        var port = server.run(0);
        System.out.println("Started test HTTP server on " + port);
        facade = new ServerFacade(port);
    }

    @AfterAll
    static void stopServer() {
        server.stop();
    }

    @BeforeEach
    void clearDatabase() throws ServerFacadeException {
        facade.clear();
    }

    @Test
    void clearSuccess() throws ServerFacadeException {
        facade.register("user1", "pass1", "u1@mail.com");
        facade.clear();
        // after clear, logging in with the same user should fail
        assertThrows(ServerFacadeException.class, () ->
                facade.login("user1", "pass1"));
    }

    @Test
    void registerSuccess() throws ServerFacadeException {
        var result = facade.register("player1", "password", "p1@email.com");
        assertNotNull(result.authToken());
        assertTrue(result.authToken().length() > 10);
        assertEquals("player1", result.username());
    }

    @Test
    void registerDuplicate() throws ServerFacadeException {
        facade.register("player1", "password", "p1@email.com");
        var ex = assertThrows(ServerFacadeException.class, () ->
                facade.register("player1", "password", "p1@email.com"));
        assertEquals(403, ex.getStatusCode());
    }

    @Test
    void loginSuccess() throws ServerFacadeException {
        facade.register("player1", "password", "p1@email.com");
        var result = facade.login("player1", "password");
        assertNotNull(result.authToken());
        assertEquals("player1", result.username());
    }

    @Test
    void loginWrongPassword() throws ServerFacadeException {
        facade.register("player1", "password", "p1@email.com");
        var ex = assertThrows(ServerFacadeException.class, () ->
                facade.login("player1", "wrongpassword"));
        assertEquals(401, ex.getStatusCode());
    }

    @Test
    void logoutSuccess() throws ServerFacadeException {
        var auth = facade.register("player1", "password", "p1@email.com");
        assertDoesNotThrow(() -> facade.logout(auth.authToken()));
    }

    @Test
    void logoutInvalidToken() {
        var ex = assertThrows(ServerFacadeException.class, () ->
                facade.logout("bad-token"));
        assertEquals(401, ex.getStatusCode());
    }

    @Test
    void createGameSuccess() throws ServerFacadeException {
        var auth = facade.register("player1", "password", "p1@email.com");
        int gameID = facade.createGame(auth.authToken(), "Test Game");
        assertTrue(gameID > 0);
    }

    @Test
    void createGameUnauthorized() {
        var ex = assertThrows(ServerFacadeException.class, () ->
                facade.createGame("bad-token", "Test Game"));
        assertEquals(401, ex.getStatusCode());
    }

    @Test
    void listGamesSuccess() throws ServerFacadeException {
        var auth = facade.register("player1", "password", "p1@email.com");
        facade.createGame(auth.authToken(), "Game 1");
        facade.createGame(auth.authToken(), "Game 2");
        var games = facade.listGames(auth.authToken());
        assertEquals(2, games.length);
    }

    @Test
    void listGamesUnauthorized() {
        var ex = assertThrows(ServerFacadeException.class, () ->
                facade.listGames("bad-token"));
        assertEquals(401, ex.getStatusCode());
    }
}
