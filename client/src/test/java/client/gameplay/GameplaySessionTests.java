package client.gameplay;

import chess.ChessGame;
import chess.ChessPosition;
import client.ServerException;
import org.junit.jupiter.api.Test;
import websocket.commands.UserGameCommand;
import websocket.messages.ErrorMessage;
import websocket.messages.LoadGameMessage;
import websocket.messages.NotificationMessage;
import websocket.messages.ServerMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class GameplaySessionTests {
    @Test
    void waitsForLoadThenPublishesDetachedStateAndPerspective() throws Exception {
        var fixture = new Fixture(GameplaySession.Role.BLACK);
        try (var session = fixture.session) {
            assertEquals(GameplaySession.Status.DISCONNECTED, session.snapshot().status());
            session.connect();
            assertEquals(GameplaySession.Status.CONNECTING, session.snapshot().status());
            var command = fixture.connections.getFirst().commands.getFirst();
            assertEquals(UserGameCommand.CommandType.CONNECT, command.getCommandType());
            assertEquals(42, command.getGameID());
            assertEquals("token", command.getAuthToken());
            fixture.latest().onMessage(new NotificationMessage("hello"));
            assertNull(session.snapshot().game());
            var game = new ChessGame();
            fixture.latest().onMessage(new LoadGameMessage(game));
            assertEquals(GameplaySession.Status.ACTIVE, session.snapshot().status());
            assertEquals(ChessGame.TeamColor.BLACK, session.snapshot().perspective());
            session.snapshot().game().getBoard().addPiece(new ChessPosition(1, 1), null);
            assertNotNull(session.snapshot().game().getBoard().getPiece(new ChessPosition(1, 1)));
            assertEquals(2, fixture.messages.size());
        }
        assertTrue(fixture.connections.getFirst().closed);
    }

    @Test
    void reconnectIgnoresEveryKindOfOldCallback() throws Exception {
        var fixture = new Fixture(GameplaySession.Role.OBSERVER);
        try (var session = fixture.session) {
            session.connect();
            var old = fixture.latest();
            old.onMessage(new LoadGameMessage(new ChessGame()));
            old.onDisconnect("lost");
            assertEquals(GameplaySession.Status.DISCONNECTED, session.snapshot().status());
            assertNotNull(session.snapshot().game());
            session.connect();
            var current = new ChessGame();
            current.setTeamTurn(ChessGame.TeamColor.BLACK);
            fixture.latest().onMessage(new LoadGameMessage(current));
            int count = fixture.messages.size();
            old.onMessage(new LoadGameMessage(new ChessGame()));
            old.onMessage(new ErrorMessage("old error"));
            old.onDisconnect("late close");
            assertEquals(count, fixture.messages.size());
            assertEquals(1, fixture.disconnects.size());
            assertEquals(ChessGame.TeamColor.BLACK, session.snapshot().game().getTeamTurn());
            assertEquals(ChessGame.TeamColor.WHITE, session.snapshot().perspective());
            assertEquals(GameplaySession.Status.ACTIVE, session.snapshot().status());
        }
    }

    @Test
    void initialErrorClosesConnectionButAnActiveCommandErrorKeepsState() throws Exception {
        var fixture = new Fixture(GameplaySession.Role.WHITE);
        try (var session = fixture.session) {
            session.connect();
            fixture.latest().onMessage(new ErrorMessage("unauthorized"));
            assertEquals(GameplaySession.Status.DISCONNECTED, session.snapshot().status());
            assertInstanceOf(ErrorMessage.class, fixture.messages.getFirst());
            assertTrue(fixture.connections.getFirst().closed);
            session.connect();
            fixture.latest().onMessage(new LoadGameMessage(new ChessGame()));
            fixture.latest().onMessage(new ErrorMessage("illegal move"));
            assertEquals(GameplaySession.Status.ACTIVE, session.snapshot().status());
            assertFalse(fixture.connections.getLast().closed);
        }
    }

    @Test
    void closeIsIdempotentSuppressesCallbacksAndNeverSendsLeave() throws Exception {
        var fixture = new Fixture(GameplaySession.Role.WHITE);
        fixture.session.connect();
        fixture.session.close();
        fixture.session.close();
        fixture.latest().onMessage(new LoadGameMessage(new ChessGame()));
        fixture.latest().onDisconnect("closed");
        assertTrue(fixture.messages.isEmpty());
        assertTrue(fixture.disconnects.isEmpty());
        assertEquals(1, fixture.connections.getFirst().commands.size());
        assertEquals(GameplaySession.Status.CLOSED, fixture.session.snapshot().status());
        assertThrows(ServerException.class, fixture.session::connect);
    }

    @Test
    void duplicateConnectDoesNotOpenAnotherSocket() throws Exception {
        var fixture = new Fixture(GameplaySession.Role.WHITE);
        try (var session = fixture.session) {
            session.connect();
            assertThrows(ServerException.class, session::connect);
            fixture.latest().onMessage(new LoadGameMessage(new ChessGame()));
            assertThrows(ServerException.class, session::connect);
            assertEquals(1, fixture.connections.size());
        }
    }

    @Test
    void openAndSendFailuresAllowRetryWithoutLeakingConnections() throws Exception {
        var fixture = new Fixture(GameplaySession.Role.WHITE);
        try (var session = fixture.session) {
            fixture.failOpen = true;
            assertThrows(ServerException.class, session::connect);
            assertEquals(GameplaySession.Status.DISCONNECTED, session.snapshot().status());
            fixture.failOpen = false;
            fixture.failSend = true;
            assertThrows(ServerException.class, session::connect);
            assertTrue(fixture.connections.getFirst().closed);
            fixture.failSend = false;
            session.connect();
            fixture.latest().onMessage(new LoadGameMessage(new ChessGame()));
            assertEquals(GameplaySession.Status.ACTIVE, session.snapshot().status());
            assertEquals(2, fixture.disconnects.size());
        }
    }

    @Test
    void closeDuringOpenDisposesLateSocketWithoutSendingConnect() throws Exception {
        var opened = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var connection = new FakeTransport(false);
        var fixture = new Fixture(GameplaySession.Role.WHITE);
        try (var session = new GameplaySession(42, "token", GameplaySession.Role.WHITE, fixture, listener -> {
            opened.countDown();
            try {
                assertTrue(release.await(3, TimeUnit.SECONDS));
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new ServerException("interrupted");
            }
            return connection;
        }); var executor = Executors.newSingleThreadExecutor()) {
            var pending = executor.submit(() -> assertThrows(ServerException.class, session::connect));
            assertTrue(opened.await(3, TimeUnit.SECONDS));
            session.close();
            release.countDown();
            pending.get(3, TimeUnit.SECONDS);
            assertTrue(connection.closed);
            assertTrue(connection.commands.isEmpty());
            assertTrue(fixture.messages.isEmpty());
        } finally {
            release.countDown();
        }
    }

    private static final class FakeTransport implements GameplayTransport {
        private final List<UserGameCommand> commands = new ArrayList<>();
        private final boolean failSend;
        private boolean closed;

        private FakeTransport(boolean failSend) {
            this.failSend = failSend;
        }

        @Override
        public void send(UserGameCommand command) throws ServerException {
            if (failSend) {
                throw new ServerException("send failed");
            }
            commands.add(command);
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    private static final class Fixture implements GameplaySession.Listener {
        private final List<GameplayTransport.Listener> listeners = new ArrayList<>();
        private final List<FakeTransport> connections = new ArrayList<>();
        private final List<ServerMessage> messages = new ArrayList<>();
        private final List<String> disconnects = new ArrayList<>();
        private final GameplaySession session;
        private boolean failOpen;
        private boolean failSend;

        private Fixture(GameplaySession.Role role) {
            session = new GameplaySession(42, "token", role, this, listener -> {
                if (failOpen) {
                    throw new ServerException("open failed");
                }
                listeners.add(listener);
                var connection = new FakeTransport(failSend);
                connections.add(connection);
                return connection;
            });
        }

        private GameplayTransport.Listener latest() {
            return listeners.getLast();
        }

        @Override
        public void onMessage(GameplaySession.Snapshot snapshot, ServerMessage message) {
            messages.add(message);
        }

        @Override
        public void onDisconnect(GameplaySession.Snapshot snapshot, String reason) {
            disconnects.add(reason);
        }
    }
}
