package client;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/** Controlled HTTP responses for transport failures that the real server should never produce. */
final class TestHttpServer implements AutoCloseable {
    private final HttpServer server;

    TestHttpServer(int status, String response) throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            try (exchange) {
                exchange.getRequestBody().readAllBytes();
                byte[] body = response.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(status, body.length);
                exchange.getResponseBody().write(body);
            }
        });
        server.start();
    }

    int port() {
        return server.getAddress().getPort();
    }

    static int stoppedPort() throws IOException {
        try (var server = new TestHttpServer(200, "{}")) {
            return server.port();
        }
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
