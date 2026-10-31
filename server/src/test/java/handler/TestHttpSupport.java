package handler;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

/** Shared HTTP requests and JSON contract assertions for handler tests. */
final class TestHttpSupport {
    private TestHttpSupport() {
    }

    static HttpResponse<String> send(HttpClient client, int port, String method, String path, String auth, String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(5)).header("Content-Type", "application/json")
                .method(method, HttpRequest.BodyPublishers.ofString(body));
        if (auth != null) {
            request.header("Authorization", auth);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    static JsonObject json(HttpResponse<String> response) {
        assertTrue(response.headers().firstValue("content-type").orElse("").startsWith("application/json"));
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

    static void assertError(HttpResponse<String> response, int status, String message) {
        assertEquals(status, response.statusCode(), response.body());
        var result = json(response);
        assertEquals(Set.of("message"), result.keySet());
        assertEquals(message, result.get("message").getAsString());
    }
}
