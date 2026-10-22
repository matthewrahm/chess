package server;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Exercises the public HTTP contract without seeding or inspecting server storage. */
class ApiLifecycleTests {
    private final Gson gson = new Gson();
    private Server server;
    private HttpClient client;
    private int port;

    @BeforeEach
    void setUp() {
        server = new Server();
        port = server.run(0);
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    }

    @AfterEach
    void tearDown() {
        client.close();
        server.stop();
    }

    private JsonObject call(String method, String path, String token, Map<String, ?> body, int status)
            throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(5)).header("Content-Type", "application/json")
                .method(method, HttpRequest.BodyPublishers.ofString(body == null ? "" : gson.toJson(body)));
        if (token != null) {
            request.header("authorization", token);
        }
        var response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(status, response.statusCode(), response.body());
        assertTrue(response.headers().firstValue("content-type").orElse("").startsWith("application/json"));
        var result = JsonParser.parseString(response.body()).getAsJsonObject();
        if (status != 200) {
            assertEquals(Set.of("message"), result.keySet());
            assertTrue(result.get("message").getAsString().startsWith("Error:"));
        }
        return result;
    }

    private Map<String, String> credentials(String username) {
        return Map.of("username", username, "password", "test-password");
    }

    private String register(String username) throws Exception {
        var response = call("POST", "/user", null,
                Map.of("username", username, "password", "test-password", "email", username + "@example.com"), 200);
        assertEquals(username, response.get("username").getAsString());
        return response.get("authToken").getAsString();
    }

    private int create(String token, String name) throws Exception {
        return call("POST", "/game", token, Map.of("gameName", name), 200).get("gameID").getAsInt();
    }

    private void join(String token, int id, String color) throws Exception {
        assertTrue(call("PUT", "/game", token, Map.of("gameID", id, "playerColor", color), 200).isEmpty());
    }

    private JsonArray list(String token) throws Exception {
        return call("GET", "/game", token, null, 200).getAsJsonArray("games");
    }

    private void assertRevoked(String token, int gameID) throws Exception {
        call("GET", "/game", token, null, 401);
        call("POST", "/game", token, Map.of("gameName", "Unauthorized"), 401);
        call("PUT", "/game", token, Map.of("gameID", gameID, "playerColor", "WHITE"), 401);
        call("DELETE", "/session", token, null, 401);
    }

    @Test
    void fullWorkflowPreservesOtherSessionsAndStartsFreshAfterClear() throws Exception {
        assertTrue(call("DELETE", "/db", null, null, 200).isEmpty());
        String alice = register("alice");
        String bob = register("bob");
        String aliceSecond = call("POST", "/session", null, credentials("alice"), 200)
                .get("authToken").getAsString();
        assertNotEquals(alice, aliceSecond);
        assertTrue(list(alice).isEmpty());

        int first = create(alice, "First game");
        int second = create(bob, "Second game");
        assertNotEquals(first, second);
        join(alice, first, "WHITE");
        join(bob, first, "BLACK");
        join(bob, second, "WHITE");
        join(aliceSecond, second, "BLACK");
        var games = list(alice);
        assertEquals(2, games.size());
        var listedIDs = new HashSet<Integer>();
        for (var entry : games) {
            var game = entry.getAsJsonObject();
            listedIDs.add(game.get("gameID").getAsInt());
            assertEquals(Set.of("gameID", "gameName", "whiteUsername", "blackUsername"), game.keySet());
            boolean isFirst = game.get("gameID").getAsInt() == first;
            assertEquals(isFirst ? first : second, game.get("gameID").getAsInt());
            assertEquals(isFirst ? "First game" : "Second game", game.get("gameName").getAsString());
            assertEquals(isFirst ? "alice" : "bob", game.get("whiteUsername").getAsString());
            assertEquals(isFirst ? "bob" : "alice", game.get("blackUsername").getAsString());
        }
        assertEquals(Set.of(first, second), listedIDs);
        call("PUT", "/game", bob, Map.of("gameID", first, "playerColor", "WHITE"), 403);
        assertTrue(call("DELETE", "/session", alice, null, 200).isEmpty());
        assertRevoked(alice, first);
        assertEquals(new HashSet<>(games.asList()), new HashSet<>(list(aliceSecond).asList()));
        assertEquals(new HashSet<>(games.asList()), new HashSet<>(list(bob).asList()));

        assertTrue(call("DELETE", "/db", null, null, 200).isEmpty());
        for (String old : new String[]{alice, aliceSecond, bob}) {
            assertRevoked(old, first);
        }
        call("POST", "/session", null, credentials("alice"), 401);
        call("POST", "/session", null, credentials("bob"), 401);
        assertTrue(call("DELETE", "/db", null, null, 200).isEmpty());

        String fresh = register("alice");
        assertNotEquals(alice, fresh);
        assertNotEquals(aliceSecond, fresh);
        assertTrue(list(fresh).isEmpty());
        call("PUT", "/game", fresh, Map.of("gameID", first, "playerColor", "WHITE"), 400);
        int freshGame = create(fresh, "After clear");
        join(fresh, freshGame, "WHITE");
        var freshGames = list(fresh);
        assertEquals(1, freshGames.size());
        assertEquals("After clear", freshGames.get(0).getAsJsonObject().get("gameName").getAsString());
        assertEquals("alice", freshGames.get(0).getAsJsonObject().get("whiteUsername").getAsString());
    }

    @Test
    void replacementServerOnSamePortHasFreshInMemoryStorage() throws Exception {
        String old = register("alice");
        int game = create(old, "Before restart");
        join(old, game, "WHITE");
        assertEquals(1, list(old).size());
        server.stop();
        server = new Server();
        assertEquals(port, server.run(port));

        assertRevoked(old, game);
        call("POST", "/session", null, credentials("alice"), 401);
        String fresh = register("alice");
        assertNotEquals(old, fresh);
        assertTrue(list(fresh).isEmpty());
        int freshGame = create(fresh, "After restart");
        join(fresh, freshGame, "BLACK");
        assertEquals("alice", list(fresh).get(0).getAsJsonObject().get("blackUsername").getAsString());
    }
}
