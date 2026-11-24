package client.gameplay;

import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPosition;
import client.ServerFacade;
import dataaccess.MemoryDataAccess;
import org.junit.jupiter.api.Test;
import server.Server;
import websocket.commands.MakeMoveCommand;
import websocket.commands.UserGameCommand;
import websocket.messages.ErrorMessage;
import websocket.messages.LoadGameMessage;
import websocket.messages.NotificationMessage;
import websocket.messages.ServerMessage;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static client.gameplay.WebSocketCommunicatorTests.take;
import static org.junit.jupiter.api.Assertions.*;

class GameplayClientIntegrationTests {
    @Test
    void playerAndObserverReceiveSavedMovesAndCanReconnectWithoutRejoining() throws Exception {
        var data = new MemoryDataAccess();
        var server = new Server(data);
        int port = server.run(0);
        try {
            var http = new ServerFacade(port);
            var alice = http.register("Alice", "password", "alice@example.com");
            var bob = http.register("Bob", "password", "bob@example.com");
            int id = http.createGame(alice.authToken(), "Practice").gameID();
            http.joinGame(alice.authToken(), "WHITE", id);
            var playerEvents = new WebSocketCommunicatorTests.Events();
            var observerEvents = new SessionEvents();
            try (var player = WebSocketCommunicator.connect(port, playerEvents);
                 var observer = new GameplaySession(port, id, bob.authToken(), GameplaySession.Role.OBSERVER, observerEvents)) {
                player.send(new UserGameCommand(UserGameCommand.CommandType.CONNECT, alice.authToken(), id));
                assertInstanceOf(LoadGameMessage.class, take(playerEvents.messages));
                observer.connect();
                assertInstanceOf(LoadGameMessage.class, take(observerEvents.messages));
                assertEquals(GameplaySession.Status.ACTIVE, observer.snapshot().status());
                assertInstanceOf(NotificationMessage.class, take(playerEvents.messages));
                var move = new ChessMove(new ChessPosition(2, 5), new ChessPosition(4, 5), null);
                player.send(new MakeMoveCommand(alice.authToken(), id, move));
                assertInstanceOf(LoadGameMessage.class, take(playerEvents.messages));
                assertInstanceOf(LoadGameMessage.class, take(observerEvents.messages));
                assertInstanceOf(NotificationMessage.class, take(observerEvents.messages));
                assertEquals(ChessGame.TeamColor.BLACK, observer.snapshot().game().getTeamTurn());
                assertNotNull(observer.snapshot().game().getBoard().getPiece(new ChessPosition(4, 5)));
                observer.close();
                player.close();
                assertEquals("Alice", data.getGame(id).whiteUsername());
                var reconnectEvents = new SessionEvents();
                try (var reconnected = new GameplaySession(port, id, alice.authToken(), GameplaySession.Role.WHITE, reconnectEvents)) {
                    reconnected.connect();
                    assertInstanceOf(LoadGameMessage.class, take(reconnectEvents.messages));
                    assertEquals(ChessGame.TeamColor.BLACK, reconnected.snapshot().game().getTeamTurn());
                    assertNull(reconnected.snapshot().game().getBoard().getPiece(new ChessPosition(2, 5)));
                    assertEquals("Alice", data.getGame(id).whiteUsername());
                }
            }
        } finally {
            server.stop();
        }
    }

    @Test
    void rejectedAuthenticationDeliversErrorAndLeavesSessionDisconnected() throws Exception {
        var server = new Server(new MemoryDataAccess());
        int port = server.run(0);
        var events = new SessionEvents();
        try (var session = new GameplaySession(port, 1, "invalid", GameplaySession.Role.OBSERVER, events)) {
            session.connect();
            assertInstanceOf(ErrorMessage.class, take(events.messages));
            take(events.disconnections);
            assertEquals(GameplaySession.Status.DISCONNECTED, session.snapshot().status());
            assertNull(session.snapshot().game());
        } finally {
            server.stop();
        }
    }

    private static final class SessionEvents implements GameplaySession.Listener {
        private final BlockingQueue<ServerMessage> messages = new LinkedBlockingQueue<>();
        private final BlockingQueue<String> disconnections = new LinkedBlockingQueue<>();

        @Override
        public void onMessage(GameplaySession.Snapshot snapshot, ServerMessage message) {
            messages.add(message);
        }

        @Override
        public void onDisconnect(GameplaySession.Snapshot snapshot, String reason) {
            disconnections.add(reason);
        }
    }
}
