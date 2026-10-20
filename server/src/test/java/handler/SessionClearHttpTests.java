package handler;

import chess.ChessGame;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.MemoryDataAccess;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import server.Server;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SessionClearHttpTests {
    private static final String REGISTER = """
            {"username":"alice","password":"secret","email":"alice@example.com"}
            """;
    private static final String LOGIN = """
            {"username":"alice","password":"secret"}
            """;
    private MemoryDataAccess data;
    private Server server;
    private HttpClient client;
    private int port;
    private String token;

    @BeforeEach
    void setUp() throws Exception {
        data = new MemoryDataAccess();
        server = new Server(data);
        port = server.run(0);
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        var response = send("POST", "/user", null, REGISTER);
        assertEquals(200, response.statusCode());
        token = json(response).get("authToken").getAsString();
    }

    @AfterEach
    void tearDown() {
        client.close();
        server.stop();
    }

    private HttpResponse<String> send(String method, String path, String auth, String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(5)).header("Content-Type", "application/json")
                .method(method, HttpRequest.BodyPublishers.ofString(body));
        if (auth != null) {
            request.header("Authorization", auth);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private JsonObject json(HttpResponse<String> response) {
        assertTrue(response.headers().firstValue("content-type").orElse("").startsWith("application/json"));
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

    private void assertEmptySuccess(HttpResponse<String> response) {
        assertEquals(200, response.statusCode(), response.body());
        assertEquals(Set.of(), json(response).keySet());
    }

    private void assertError(HttpResponse<String> response, int status, String message) {
        assertEquals(status, response.statusCode(), response.body());
        var result = json(response);
        assertEquals(Set.of("message"), result.keySet());
        assertEquals(message, result.get("message").getAsString());
    }

    private String login() throws Exception {
        var response = send("POST", "/session", null, LOGIN);
        assertEquals(200, response.statusCode());
        return json(response).get("authToken").getAsString();
    }

    @Test
    void logoutRevokesOnlyRequestedSessionAndPreservesUsersAndGames() throws Exception {
        String otherSession = login();
        int game = data.createGame("Preserved game", new ChessGame());
        var original = data.getGame(game);

        assertEmptySuccess(send("DELETE", "/session", token, ""));
        assertNull(data.getAuth(token));
        assertNotNull(data.getAuth(otherSession));
        assertNotNull(data.getUser("alice"));
        assertEquals(original, data.getGame(game));
        assertError(send("DELETE", "/session", token, ""), 401, "Error: unauthorized");
        assertEmptySuccess(send("DELETE", "/session", otherSession, ""));
        assertNotNull(data.getAuth(login()));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "unknown-token"})
    void missingOrInvalidHeaderCannotRevokeAValidSession(String auth) throws Exception {
        assertError(send("DELETE", "/session", auth, ""), 401, "Error: unauthorized");
        assertNotNull(data.getAuth(token));
    }

    @Test
    void bodyAndQueryCannotSupplyAuthorization() throws Exception {
        String body = "{\"authToken\":\"" + token + "\"}";
        assertError(send("DELETE", "/session?authToken=" + token, null, body), 401, "Error: unauthorized");
        assertError(send("DELETE", "/session", "invalid-token", body), 401, "Error: unauthorized");
        assertNotNull(data.getAuth(token));
    }

    @Test
    void logoutUsesHeaderTokenEvenWhenBodyNamesAnotherSession() throws Exception {
        String otherSession = login();
        String body = "{\"authToken\":\"" + otherSession + "\"}";
        assertEmptySuccess(send("DELETE", "/session", token, body));
        assertNull(data.getAuth(token));
        assertNotNull(data.getAuth(otherSession));
    }

    @Test
    void bearerPrefixIsNotPartOfTheCourseTokenFormat() throws Exception {
        assertError(send("DELETE", "/session", "Bearer " + token, ""), 401, "Error: unauthorized");
        assertNotNull(data.getAuth(token));
        assertEmptySuccess(send("DELETE", "/session", token, ""));
    }

    @Test
    void clearWithoutAuthorizationRemovesAllUsersSessionsAndGames() throws Exception {
        String secondSession = login();
        var secondUser = send("POST", "/user", null, REGISTER.replace("alice", "bob"));
        assertEquals(200, secondUser.statusCode());
        String otherToken = json(secondUser).get("authToken").getAsString();
        int firstGame = data.createGame("First", new ChessGame());
        int secondGame = data.createGame("Second", new ChessGame());

        assertEmptySuccess(send("DELETE", "/db", null, ""));
        assertNull(data.getUser("alice"));
        assertNull(data.getUser("bob"));
        assertNull(data.getAuth(token));
        assertNull(data.getAuth(secondSession));
        assertNull(data.getAuth(otherToken));
        assertNull(data.getGame(firstGame));
        assertNull(data.getGame(secondGame));
        assertTrue(data.listGames().isEmpty());
        assertError(send("POST", "/session", null, LOGIN), 401, "Error: unauthorized");
        assertError(send("DELETE", "/session", token, ""), 401, "Error: unauthorized");
    }

    @Test
    void clearCanBeRepeatedAndDoesNotRequireAValidToken() throws Exception {
        assertEmptySuccess(send("DELETE", "/db", "invalid-token", ""));
        assertEmptySuccess(send("DELETE", "/db", null, ""));
        assertEmptySuccess(send("DELETE", "/db", token, ""));
        assertNull(data.getUser("alice"));
        assertNull(data.getAuth(token));
    }

    @Test
    void accountCanBeRegisteredAgainAfterClearWithFreshAuthorization() throws Exception {
        assertEmptySuccess(send("DELETE", "/db", null, ""));
        var response = send("POST", "/user", null, REGISTER);
        assertEquals(200, response.statusCode());
        String freshToken = json(response).get("authToken").getAsString();
        assertNotEquals(token, freshToken);
        assertNotNull(data.getAuth(freshToken));
        assertError(send("DELETE", "/session", token, ""), 401, "Error: unauthorized");
        assertNotNull(data.getAuth(login()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"clear", "deleteAuth"})
    void storageFailuresReturnJson500WithoutReportingSuccess(String operation) throws Exception {
        var failingData = (DataAccess) Proxy.newProxyInstance(DataAccess.class.getClassLoader(),
                new Class<?>[]{DataAccess.class}, (proxy, method, args) -> {
                    if (method.getName().equals(operation)) {
                        throw new DataAccessException("Private storage connection details");
                    }
                    try {
                        return method.invoke(data, args);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
        server.stop();
        server = new Server(failingData);
        port = server.run(0);
        String path = operation.equals("clear") ? "/db" : "/session";
        assertError(send("DELETE", path, token, ""), 500, "Error: internal server error");
        assertNotNull(data.getUser("alice"));
        assertNotNull(data.getAuth(token));
    }
}
