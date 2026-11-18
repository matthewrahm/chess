package server.websocket;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** Real network peer with bounded waits and support for fragmented server messages. */
final class SocketTestClient implements AutoCloseable, WebSocket.Listener {
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private final BlockingQueue<String> messages = new LinkedBlockingQueue<>();
    private final CountDownLatch closed = new CountDownLatch(1);
    private final StringBuilder partial = new StringBuilder();
    private final WebSocket socket;

    SocketTestClient(int port) throws Exception {
        socket = http.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(3))
                .buildAsync(URI.create("ws://localhost:" + port + "/ws"), this).get(3, TimeUnit.SECONDS);
    }

    void send(Object command) throws Exception {
        sendText(new Gson().toJson(command));
    }

    void sendText(String text) throws Exception {
        socket.sendText(text, true).get(3, TimeUnit.SECONDS);
    }

    void sendBinary() throws Exception {
        socket.sendBinary(ByteBuffer.wrap(new byte[]{1, 2}), true).get(3, TimeUnit.SECONDS);
    }

    JsonObject next(String expectedType) throws Exception {
        String text = messages.poll(3, TimeUnit.SECONDS);
        assertNotNull(text, "Timed out waiting for " + expectedType);
        var message = JsonParser.parseString(text).getAsJsonObject();
        assertEquals(expectedType, message.get("serverMessageType").getAsString(), text);
        return message;
    }

    String error() throws Exception {
        var message = next("ERROR");
        assertEquals(java.util.Set.of("serverMessageType", "errorMessage"), message.keySet());
        String error = message.get("errorMessage").getAsString();
        assertTrue(error.startsWith("Error:"), error);
        return error;
    }

    void expectSilence() throws Exception {
        assertNull(messages.poll(200, TimeUnit.MILLISECONDS), "Unexpected extra message");
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        webSocket.request(1);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        partial.append(data);
        if (last) {
            messages.add(partial.toString());
            partial.setLength(0);
        }
        webSocket.request(1);
        return null;
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        closed.countDown();
        return null;
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        messages.add("Transport failure: " + error);
        closed.countDown();
    }

    @Override
    public void close() throws Exception {
        try {
            socket.sendClose(WebSocket.NORMAL_CLOSURE, "test complete").get(3, TimeUnit.SECONDS);
            closed.await(3, TimeUnit.SECONDS);
        } finally {
            socket.abort();
            http.close();
        }
    }
}
