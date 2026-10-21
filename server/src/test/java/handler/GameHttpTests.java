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
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import server.Server;

import java.net.URI;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class GameHttpTests {
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
        token = register("alice");
    }

    @AfterEach
    void tearDown() {
        client.close();
        server.stop();
    }

    private HttpResponse<String> send(String method, String auth, String body) throws Exception {
        return sendTo(method, "/game", auth, body);
    }

    private HttpResponse<String> sendTo(String method, String path, String auth, String body) throws Exception {
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

    private String register(String username) throws Exception {
        var response = sendTo("POST", "/user", null,
                "{\"username\":\"" + username + "\",\"password\":\"secret\",\"email\":\"test@example.com\"}");
        assertEquals(200, response.statusCode());
        return json(response).get("authToken").getAsString();
    }

    private int create() throws Exception {
        var response = send("POST", token, "{\"gameName\":\"Evening chess\"}");
        assertEquals(200, response.statusCode(), response.body());
        assertEquals(Set.of("gameID"), json(response).keySet());
        return json(response).get("gameID").getAsInt();
    }

    private void error(HttpResponse<String> response, int status) {
        assertEquals(status, response.statusCode(), response.body());
        assertEquals(Set.of("message"), json(response).keySet());
        assertTrue(json(response).get("message").getAsString().startsWith("Error:"));
    }

    @Test
    void createJoinAndListPreserveBoardAndReturnOnlyGameSummaries() throws Exception {
        var empty = send("GET", token, "");
        assertEquals(200, empty.statusCode());
        assertTrue(json(empty).getAsJsonArray("games").isEmpty());
        int id = create();
        assertEquals(new ChessGame(), data.getGame(id).game());
        assertNull(data.getGame(id).whiteUsername());
        assertNull(data.getGame(id).blackUsername());
        String bob = register("bob");
        var white = send("PUT", token, "{\"playerColor\":\"WHITE\",\"gameID\":" + id
                + ",\"username\":\"mallory\",\"authToken\":\"spoofed\"}");
        assertEquals(200, white.statusCode());
        assertTrue(json(white).isEmpty());
        var black = send("PUT", bob, "{\"playerColor\":\"BLACK\",\"gameID\":" + id + "}");
        assertEquals(200, black.statusCode());
        assertTrue(json(black).isEmpty());
        error(send("PUT", bob, "{\"playerColor\":\"WHITE\",\"gameID\":" + id + "}"), 403);
        var response = send("GET", bob, "");
        assertEquals(200, response.statusCode());
        assertEquals(Set.of("games"), json(response).keySet());
        var games = json(response).getAsJsonArray("games");
        assertEquals(1, games.size());
        var game = games.get(0).getAsJsonObject();
        assertEquals(Set.of("gameID", "gameName", "whiteUsername", "blackUsername"), game.keySet());
        assertEquals(id, game.get("gameID").getAsInt());
        assertEquals("Evening chess", game.get("gameName").getAsString());
        assertEquals("alice", game.get("whiteUsername").getAsString());
        assertEquals("bob", game.get("blackUsername").getAsString());
        assertEquals(new ChessGame(), data.getGame(id).game());
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST", "PUT"})
    void everyGameRouteRequiresHeaderAndRejectsRevokedTokens(String method) throws Exception {
        int id = create();
        String body = "{\"gameName\":\"Unauthorized\",\"playerColor\":\"WHITE\",\"gameID\":" + id
                + ",\"authToken\":\"" + token + "\"}";
        error(sendTo(method, "/game?authToken=" + token, null, body), 401);
        error(send(method, "invalid", body), 401);
        error(send(method, "Bearer " + token, body), 401);
        assertEquals(200, sendTo("DELETE", "/session", token, "").statusCode());
        error(send(method, token, body), 401);
        assertEquals(1, data.listGames().size());
        assertNull(data.getGame(id).whiteUsername());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"gameName\":null}", "{\"gameName\":\" \"}", "{\"gameName\":42}",
            "{\"gameName\":true}", "{\"gameName\":[]}", "null", "[]", "{", ""})
    void invalidCreateBodyDoesNotCreateGame(String body) throws Exception {
        error(send("POST", token, body), 400);
        assertTrue(data.listGames().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "\"1\"", "true", "[]", "{}", "1.5", "4294967297", "-2147483649",
            "1e100", "0", "-1", "99999"})
    void invalidGameIDsCannotJoinOrTruncateToExistingGame(String id) throws Exception {
        int existing = create();
        error(send("PUT", token, "{\"playerColor\":\"WHITE\",\"gameID\":" + id + "}"), 400);
        assertNull(data.getGame(existing).whiteUsername());
        assertNull(data.getGame(existing).blackUsername());
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "\"white\"", "\"GREEN\"", "\"\"", "42", "true", "[]"})
    void invalidColorsDoNotOccupySeat(String color) throws Exception {
        int id = create();
        error(send("PUT", token, "{\"playerColor\":" + color + ",\"gameID\":" + id + "}"), 400);
        assertNull(data.getGame(id).whiteUsername());
        assertNull(data.getGame(id).blackUsername());
    }

    @Test
    void missingJoinFieldsFailAndIntegralNumericIDWorks() throws Exception {
        int id = create();
        error(send("PUT", token, "{\"playerColor\":\"WHITE\"}"), 400);
        error(send("PUT", token, "{\"gameID\":" + id + "}"), 400);
        assertEquals(200, send("PUT", token,
                "{\"playerColor\":\"WHITE\",\"gameID\":" + id + ".0}").statusCode());
        assertEquals("alice", data.getGame(id).whiteUsername());
    }

    @ParameterizedTest
    @MethodSource("malformedGameBodies")
    void malformedJsonCannotCreateOrJoinAndDoesNotPreventLaterRequests(String body) throws Exception {
        int id = create();
        var original = data.getGame(id);
        String payload = body.replace("GAME_ID", Integer.toString(id));
        for (String method : new String[]{"POST", "PUT"}) {
            var response = send(method, token, payload);
            error(response, 400);
            assertEquals("Error: bad request", json(response).get("message").getAsString());
            assertEquals(1, data.listGames().size());
            assertEquals(original, data.getGame(id));
            assertEquals(new ChessGame(), data.getGame(id).game());
        }
        assertNotEquals(id, create());
        assertEquals(200, send("PUT", token,
                "{\"playerColor\":\"WHITE\",\"gameID\":" + id + "}").statusCode());
        assertEquals("alice", data.getGame(id).whiteUsername());
    }

    static Stream<String> malformedGameBodies() {
        String valid = "{\"gameName\":\"Second game\",\"playerColor\":\"WHITE\",\"gameID\":GAME_ID}";
        return Stream.of(
                "true", "\"text\"", "42", "[" + valid + "]",
                valid + " {}", valid + " trailing", valid.substring(0, valid.length() - 1),
                valid.replace("}", ",}"), valid.replace('"', '\''),
                valid.replace("\"gameName\"", "gameName"), "/* comment */" + valid,
                valid.replace(":GAME_ID", ":01"), valid.replace("Second game", "Invalid\\q"));
    }

    @ParameterizedTest
    @MethodSource("gameStorageFailures")
    void storageFailuresReturnGenericErrorsWithoutChangingGamesAndCanRecover(
            String method, String operation, boolean unexpected) throws Exception {
        int id = create();
        var original = data.getGame(id);
        var failing = new AtomicBoolean(true);
        DataAccess storage = (DataAccess) Proxy.newProxyInstance(DataAccess.class.getClassLoader(),
                new Class<?>[]{DataAccess.class}, (proxy, called, args) -> {
                    if (failing.get() && called.getName().equals(operation)) {
                        if (unexpected) {
                            throw new IllegalStateException("Private storage diagnostics");
                        }
                        throw new DataAccessException("Private storage diagnostics");
                    }
                    try {
                        return called.invoke(data, args);
                    } catch (InvocationTargetException exception) {
                        throw exception.getCause();
                    }
                });
        server.stop();
        server = new Server(storage);
        port = server.run(0);
        String body = "{\"gameName\":\"Second game\",\"playerColor\":\"WHITE\",\"gameID\":" + id + "}";

        var response = send(method, token, body);
        error(response, 500);
        assertEquals("Error: internal server error", json(response).get("message").getAsString());
        assertEquals(1, data.listGames().size());
        assertEquals(original, data.getGame(id));
        assertEquals(new ChessGame(), data.getGame(id).game());
        assertNotNull(data.getAuth(token));
        assertNotNull(data.getUser("alice"));

        failing.set(false);
        var recovered = send(method, token, body);
        assertEquals(200, recovered.statusCode(), recovered.body());
        switch (method) {
            case "GET" -> assertEquals(1, json(recovered).getAsJsonArray("games").size());
            case "POST" -> {
                assertEquals(2, data.listGames().size());
                assertNotEquals(id, json(recovered).get("gameID").getAsInt());
            }
            case "PUT" -> assertEquals("alice", data.getGame(id).whiteUsername());
            default -> fail("Unexpected HTTP method");
        }
    }

    static Stream<Arguments> gameStorageFailures() {
        return Stream.of(new String[]{"GET", "getAuth"}, new String[]{"GET", "listGames"},
                        new String[]{"POST", "getAuth"}, new String[]{"POST", "createGame"},
                        new String[]{"PUT", "getAuth"}, new String[]{"PUT", "getGame"},
                        new String[]{"PUT", "updateGame"})
                .flatMap(route -> Stream.of(false, true)
                        .map(unexpected -> Arguments.of(route[0], route[1], unexpected)));
    }
}
