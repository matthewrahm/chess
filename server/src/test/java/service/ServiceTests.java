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
}
