package client;

import dataaccess.sql.SqlDataAccess;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import server.Server;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;

/** Owns the random-port server and disposable-database lifecycle for facade tests. */
abstract class ServerFacadeTestSupport {
    private static Server server;
    static ServerFacade facade;
    static SqlDataAccess storage;

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


    static Stream<Arguments> invalidRegistrations() {
        return Stream.of(
                Arguments.of(null, "secret", "p@example.com"),
                Arguments.of(" ", "secret", "p@example.com"),
                Arguments.of("player", null, "p@example.com"),
                Arguments.of("player", "", "p@example.com"),
                Arguments.of("player", "secret", null),
                Arguments.of("player", "secret", " "));
    }

    static Stream<Arguments> invalidLogins() {
        return Stream.of(Arguments.of(null, "secret"), Arguments.of(" ", "secret"),
                Arguments.of("player", null), Arguments.of("player", ""));
    }

    static Stream<Arguments> httpFailures() {
        return Stream.of("clear", "register", "login", "create", "list", "join")
                .flatMap(operation -> Stream.of(302, 500).map(status -> Arguments.of(operation, status)));
    }

    static void callFacade(ServerFacade client, String operation) throws ServerException {
        switch (operation) {
            case "clear" -> client.clear();
            case "register" -> client.register("player", "secret", "p@example.com");
            case "login" -> client.login("player", "secret");
            case "create" -> client.createGame("token", "Practice");
            case "list" -> client.listGames("token");
            case "join" -> client.joinGame("token", "WHITE", 1);
            default -> throw new IllegalArgumentException("Unknown test operation: " + operation);
        }
    }

    static Stream<Arguments> invalidSessions() {
        return Stream.of("logout", "create", "list", "join").flatMap(operation ->
                Stream.of(null, "", " ", "unknown", "token\nInjected", "token\rInjected")
                        .map(token -> Arguments.of(operation, token)));
    }

    static void authenticatedCall(String operation, String token, int gameID) throws ServerException {
        switch (operation) {
            case "logout" -> facade.logout(token);
            case "create" -> facade.createGame(token, "Practice");
            case "list" -> facade.listGames(token);
            case "join" -> facade.joinGame(token, "WHITE", gameID);
            default -> throw new IllegalArgumentException("Unknown test operation: " + operation);
        }
    }

    static Stream<Arguments> invalidJoins() {
        return Stream.of(Arguments.of(null, 1), Arguments.of("", 1), Arguments.of("white", 1),
                Arguments.of("OBSERVER", 1), Arguments.of("WHITE", null), Arguments.of("WHITE", 0),
                Arguments.of("BLACK", -1), Arguments.of("WHITE", Integer.MAX_VALUE));
    }
}
