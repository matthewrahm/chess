package client.gameplay;

import client.ServerException;
import com.google.gson.Gson;
import websocket.commands.UserGameCommand;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

/** Owns socket resources, bounded writes, fragmented text, and typed message delivery. */
public final class WebSocketCommunicator implements GameplayTransport {
    private static final int TIMEOUT_SECONDS = 5;
    private static final int MAX_MESSAGE_CHARS = 1_048_576;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS)).build();
    private final Listener listener;
    private final Gson gson = new Gson();
    private final Object writeLock = new Object();
    private volatile WebSocket socket;
    private boolean closed;

    private WebSocketCommunicator(Listener listener) {
        this.listener = listener;
    }

    public static WebSocketCommunicator connect(int port, Listener listener) throws ServerException {
        if (port < 1 || port > 65535 || listener == null) {
            throw new IllegalArgumentException("A valid port and listener are required.");
        }
        var connection = new WebSocketCommunicator(listener);
        var pending = connection.client.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .buildAsync(URI.create("ws://localhost:" + port + "/ws"), connection.new IncomingMessages());
        try {
            connection.socket = pending.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (connection.isClosed()) {
                connection.socket.abort();
                throw new ServerException("The game connection closed before it was ready. Please reconnect.");
            }
            return connection;
        } catch (Exception exception) {
            pending.cancel(true);
            connection.close();
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ServerException("Unable to connect to the game. Please try again.", exception);
        }
    }

    @Override
    public void send(UserGameCommand command) throws ServerException {
        if (command == null) {
            throw new IllegalArgumentException("A game command is required.");
        }
        synchronized (writeLock) {
            if (isClosed()) {
                throw new ServerException("The game connection is closed. Please reconnect.");
            }
            try {
                socket.sendText(gson.toJson(command), true).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (Exception exception) {
                if (exception instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                disconnect("The game connection was lost. Please reconnect.");
                throw new ServerException("Unable to send the game command. Please reconnect.", exception);
            }
        }
    }

    private synchronized boolean isClosed() {
        return closed;
    }

    private synchronized boolean release() {
        if (closed) {
            return false;
        }
        closed = true;
        if (socket != null) {
            socket.abort();
        }
        client.shutdownNow();
        return true;
    }

    @Override
    public void close() {
        release();
    }

    private void disconnect(String reason) {
        if (release()) {
            listener.onDisconnect(reason);
        }
    }

    private final class IncomingMessages implements WebSocket.Listener {
        private final StringBuilder fragments = new StringBuilder();
        private final ServerMessageReader reader = new ServerMessageReader();

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            if (!isClosed()) {
                if (fragments.length() + data.length() > MAX_MESSAGE_CHARS) {
                    disconnect("The server sent an oversized game message.");
                } else {
                    fragments.append(data);
                    if (last) {
                        deliver();
                    }
                }
            }
            webSocket.request(1);
            return null;
        }

        private void deliver() {
            try {
                var message = reader.read(fragments.toString());
                fragments.setLength(0);
                listener.onMessage(message);
            } catch (RuntimeException exception) {
                disconnect("Unable to process a game message. Please reconnect.");
            }
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            disconnect("The server sent an unsupported game message.");
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            disconnect("The server closed the game connection. Please reconnect.");
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            disconnect("The game connection was lost. Please reconnect.");
        }
    }
}
