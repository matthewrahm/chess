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
}
