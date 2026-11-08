package client;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

/** Controlled HTTP responses for transport failures that the real server should never produce. */
final class TestHttpServer implements AutoCloseable {
    private final HttpServer server;
    private volatile Request request;

    TestHttpServer(int status, String response) throws IOException {
        this(request -> new Response(status, response));
    }

    TestHttpServer(Function<Request, Response> handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            try (exchange) {
                request = new Request(exchange.getRequestMethod(), exchange.getRequestURI().toString(),
                        exchange.getRequestHeaders().getFirst("authorization"),
                        new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                Response response = handler.apply(request);
                byte[] body = response.body().getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(response.status(), body.length);
                exchange.getResponseBody().write(body);
            }
        });
        server.start();
    }

    record Response(int status, String body) {
    }

    record Request(String method, String path, String authorization, String body) {
    }

    Request request() {
        return request;
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
