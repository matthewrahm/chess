package client.gameplay;

import chess.ChessMove;
import chess.ChessPosition;
import client.ServerException;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.javalin.Javalin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import websocket.commands.MakeMoveCommand;
import websocket.commands.UserGameCommand;
import websocket.messages.NotificationMessage;
import websocket.messages.ServerMessage;

import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class WebSocketCommunicatorTests {
    @Test
    void sendsAllFourCommandsWithExactWireFieldsAndPreservesOrder() throws Exception {
        var received = new LinkedBlockingQueue<JsonObject>();
        var events = new Events();
        var app = Javalin.create().ws("/ws", ws -> ws.onMessage(ctx -> {
            received.add(JsonParser.parseString(ctx.message()).getAsJsonObject());
            ctx.send(new Gson().toJson(new NotificationMessage(ctx.message())));
        })).start(0);
        try (var connection = WebSocketCommunicator.connect(app.port(), events)) {
            var move = new ChessMove(new ChessPosition(2, 5), new ChessPosition(4, 5), null);
            for (var type : UserGameCommand.CommandType.values()) {
                connection.send(type == UserGameCommand.CommandType.MAKE_MOVE
                        ? new MakeMoveCommand("secret", 17, move) : new UserGameCommand(type, "secret", 17));
                var json = take(received);
                assertEquals(type.name(), json.get("commandType").getAsString());
                assertEquals("secret", json.get("authToken").getAsString());
                assertEquals(17, json.get("gameID").getAsInt());
                if (type == UserGameCommand.CommandType.MAKE_MOVE) {
                    assertEquals(move, new Gson().fromJson(json.get("move"), ChessMove.class));
                    assertEquals(4, json.size());
                } else {
                    assertEquals(3, json.size());
                }
                assertInstanceOf(NotificationMessage.class, take(events.messages));
            }
            connection.close();
            assertThrows(ServerException.class,
                    () -> connection.send(new UserGameCommand(UserGameCommand.CommandType.CONNECT, "secret", 17)));
            assertNull(events.disconnections.poll(150, TimeUnit.MILLISECONDS));
        } finally {
            app.stop();
        }
    }

    @Test
    void assemblesFragmentedMessagesBeforeDeliveringExactlyOnce() throws Exception {
        var events = new Events();
        var app = Javalin.create().ws("/ws", ws -> ws.onMessage(ctx -> {
            var remote = ctx.session.getRemote();
            remote.sendPartialString("{\"serverMessageType\":\"NOTIF", false);
            remote.sendPartialString("ICATION\",\"message\":\"hello\"}", true);
        })).start(0);
        try (var connection = WebSocketCommunicator.connect(app.port(), events)) {
            connection.send(new UserGameCommand(UserGameCommand.CommandType.CONNECT, "token", 1));
            var notice = assertInstanceOf(NotificationMessage.class, take(events.messages));
            assertEquals("hello", notice.getMessage());
            assertNull(events.messages.poll(150, TimeUnit.MILLISECONDS));
            assertTrue(events.disconnections.isEmpty());
        } finally {
            app.stop();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid", "binary", "close", "oversized"})
    void badFramesAndServerCloseDisconnectOnceWithoutLeakingRemoteDetails(String kind) throws Exception {
        var events = new Events();
        var app = Javalin.create().ws("/ws", ws -> ws.onMessage(ctx -> {
            switch (kind) {
                case "invalid" -> ctx.send("private invalid response");
                case "binary" -> ctx.session.getRemote().sendBytes(ByteBuffer.wrap(new byte[]{1, 2}));
                case "oversized" -> ctx.send("x".repeat(1_048_577));
                default -> ctx.session.close(1000, "private diagnostics");
            }
        })).start(0);
        try (var connection = WebSocketCommunicator.connect(app.port(), events)) {
            connection.send(new UserGameCommand(UserGameCommand.CommandType.CONNECT, "token", 1));
            assertFalse(take(events.disconnections).contains("private"));
            assertTrue(events.messages.isEmpty());
            assertThrows(ServerException.class,
                    () -> connection.send(new UserGameCommand(UserGameCommand.CommandType.RESIGN, "token", 1)));
            assertNull(events.disconnections.poll(150, TimeUnit.MILLISECONDS));
        } finally {
            app.stop();
        }
    }

    @Test
    void refusedConnectionIsReportedAsServerException() throws Exception {
        int port;
        try (var reservation = new ServerSocket(0)) {
            port = reservation.getLocalPort();
        }
        assertThrows(ServerException.class, () -> WebSocketCommunicator.connect(port, new Events()));
        assertThrows(IllegalArgumentException.class, () -> WebSocketCommunicator.connect(0, new Events()));
    }

    static <T> T take(BlockingQueue<T> queue) throws InterruptedException {
        var event = queue.poll(3, TimeUnit.SECONDS);
        assertNotNull(event, "Expected a socket callback within three seconds");
        return event;
    }

    static final class Events implements GameplayTransport.Listener {
        final BlockingQueue<ServerMessage> messages = new LinkedBlockingQueue<>();
        final BlockingQueue<String> disconnections = new LinkedBlockingQueue<>();

        @Override
        public void onMessage(ServerMessage message) {
            messages.add(message);
        }

        @Override
        public void onDisconnect(String reason) {
            disconnections.add(reason);
        }
    }
}
