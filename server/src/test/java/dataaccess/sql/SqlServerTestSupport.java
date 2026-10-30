package dataaccess.sql;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import server.Server;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Runs the default SQL server with the same JSON requests a client would send. */
final class SqlServerTestSupport implements AutoCloseable {
    private final Gson gson = new Gson();
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private Server server;
    private int port;

    SqlServerTestSupport() {
        try {
            server = new Server();
            port = server.run(0);
        } catch (RuntimeException failure) {
            close();
            throw failure;
        }
    }

    void restart() {
        server.stop();
        server = new Server();
        assertEquals(port, server.run(port));
    }

    JsonObject call(String method, String path, String token, Map<String, ?> body, int status) throws Exception {
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
        if (status == 500) {
            assertEquals(Set.of("message"), result.keySet());
            assertEquals("Error: internal server error", result.get("message").getAsString());
        }
        return result;
    }

    String register(String username) throws Exception {
        return call("POST", "/user", null, registration(username), 200).get("authToken").getAsString();
    }

    int createGame(String token, String name) throws Exception {
        return call("POST", "/game", token, Map.of("gameName", name), 200).get("gameID").getAsInt();
    }

    Map<String, String> registration(String username) {
        return Map.of("username", username, "password", "test-password", "email", username + "@example.com");
    }

    @Override
    public void close() {
        try {
            if (server != null) {
                server.stop();
            }
        } finally {
            client.close();
        }
    }
}
