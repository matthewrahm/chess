package server;

import dataaccess.MemoryDataAccess;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class ServerLifecycleTests {
    private HttpResponse<byte[]> get(int port, String path) throws Exception {
        try (var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build()) {
            var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                    .timeout(Duration.ofSeconds(5)).GET().build();
            return client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        }
    }

    @Test
    void servesCoursePageAndAllAssetsOnAssignedPort() throws Exception {
        var server = new Server(new MemoryDataAccess());
        try {
            int port = server.run(0);
            assertTrue(port > 0);
            for (String name : new String[]{"index.html", "index.css", "index.js", "favicon.ico"}) {
                var response = get(port, "/" + name);
                assertEquals(200, response.statusCode(), name);
                try (var resource = getClass().getResourceAsStream("/web/" + name)) {
                    assertNotNull(resource, name);
                    assertArrayEquals(resource.readAllBytes(), response.body(), name);
                }
            }
            var home = get(port, "/");
            assertEquals(200, home.statusCode());
            assertTrue(home.headers().firstValue("content-type").orElse("").startsWith("text/html"));
            assertArrayEquals(get(port, "/index.html").body(), home.body());
        } finally {
            server.stop();
        }
    }

    @Test
    void stopClosesListenerAndAllowsPortReuse() throws Exception {
        var server = new Server(new MemoryDataAccess());
        int port;
        try {
            port = server.run(0);
            assertEquals(200, get(port, "/").statusCode());
        } finally {
            server.stop();
        }
        int stoppedPort = port;
        assertThrows(IOException.class, () -> get(stoppedPort, "/"));

        var replacement = new Server(new MemoryDataAccess());
        try {
            assertEquals(port, replacement.run(port));
            assertEquals(200, get(port, "/").statusCode());
        } finally {
            replacement.stop();
        }
    }

    @Test
    void separateServersCanRunAndStopIndependently() throws Exception {
        var first = new Server(new MemoryDataAccess());
        var second = new Server(new MemoryDataAccess());
        try {
            int firstPort = first.run(0);
            int secondPort = second.run(0);
            assertNotEquals(firstPort, secondPort);
            assertEquals(200, get(firstPort, "/").statusCode());
            assertEquals(200, get(secondPort, "/").statusCode());
            first.stop();
            assertEquals(200, get(secondPort, "/").statusCode());
        } finally {
            first.stop();
            second.stop();
        }
    }
}
