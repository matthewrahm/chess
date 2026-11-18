package server.websocket;

import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPosition;
import model.GameData;
import org.junit.jupiter.api.Test;
import websocket.commands.UserGameCommand;

import static org.junit.jupiter.api.Assertions.*;
import static websocket.commands.UserGameCommand.CommandType.*;

class GameplayConnectTests extends GameSocketFixture {
    @Test
    void playerAndObserverConnectionsLoadSavedPositionAndAnnounceRoles() throws Exception {
        join("white-token", gameID, "WHITE");
        join("black-token", gameID, "BLACK");
        var stored = data.getGame(gameID);
        stored.game().makeMove(new ChessMove(new ChessPosition(2, 5), new ChessPosition(4, 5), null));
        data.updateGame(stored);
        try (var white = new SocketTestClient(port); var black = new SocketTestClient(port);
             var observer = new SocketTestClient(port)) {
            assertEquals(stored.game(), connect(white, "white-token", gameID));
            assertEquals(stored.game(), connect(black, "black-token", gameID));
            assertEquals("Bob connected as black.", notice(white));
            var loaded = connect(observer, "observer-token", gameID);
            assertEquals(stored.game(), loaded);
            assertEquals(ChessGame.TeamColor.BLACK, loaded.getTeamTurn());
            assertEquals("Charlie connected as an observer.", notice(white));
            assertEquals("Charlie connected as an observer.", notice(black));
            observer.expectSilence();
            white.expectSilence();
            black.expectSilence();
            assertEquals(stored, data.getGame(gameID));
        }
    }

    @Test
    void observerCanConnectWithoutTakingEitherOpenSeat() throws Exception {
        var before = data.getGame(gameID);
        try (var observer = new SocketTestClient(port)) {
            assertEquals(before.game(), connect(observer, "observer-token", gameID));
            observer.expectSilence();
            assertEquals(before, data.getGame(gameID));
            assertNull(data.getGame(gameID).whiteUsername());
            assertNull(data.getGame(gameID).blackUsername());
        }
    }

    @Test
    void repeatedConnectRefreshesStateWithoutDuplicateSubscriptionOrNotice() throws Exception {
        try (var first = new SocketTestClient(port); var second = new SocketTestClient(port)) {
            connect(first, "white-token", gameID);
            connect(second, "black-token", gameID);
            notice(first);
            var changed = data.getGame(gameID);
            changed.game().makeMove(new ChessMove(new ChessPosition(2, 1), new ChessPosition(3, 1), null));
            data.updateGame(changed);
            assertEquals(changed.game(), connect(second, "black-token", gameID));
            first.expectSilence();
            try (var third = new SocketTestClient(port)) {
                connect(third, "observer-token", gameID);
                assertEquals("Charlie connected as an observer.", notice(first));
                assertEquals("Charlie connected as an observer.", notice(second));
                first.expectSilence();
                second.expectSilence();
            }
        }
    }

    @Test
    void finishedGameLoadsWithoutReopeningIt() throws Exception {
        var ended = data.getGame(gameID);
        ended.game().endGame();
        data.updateGame(ended);
        try (var observer = new SocketTestClient(port)) {
            assertTrue(connect(observer, "observer-token", gameID).isGameOver());
            assertEquals(ended, data.getGame(gameID));
        }
    }

    @Test
    void disconnectedPlayerCanReconnectWithoutHttpRejoinOrSeatLoss() throws Exception {
        join("white-token", gameID, "WHITE");
        try (var observer = new SocketTestClient(port)) {
            connect(observer, "observer-token", gameID);
            try (var original = new SocketTestClient(port)) {
                connect(original, "white-token", gameID);
                assertEquals("Alice connected as white.", notice(observer));
            }
            assertEquals("Alice", data.getGame(gameID).whiteUsername());
            observer.expectSilence();
            try (var reconnected = new SocketTestClient(port)) {
                connect(reconnected, "white-token", gameID);
                assertEquals("Alice connected as white.", notice(observer));
                reconnected.expectSilence();
            }
        }
    }

    @Test
    void invalidCommandFromConnectedPeerDoesNotNotifyOrMutate() throws Exception {
        try (var first = new SocketTestClient(port); var second = new SocketTestClient(port)) {
            connect(first, "white-token", gameID);
            connect(second, "black-token", gameID);
            notice(first);
            var before = data.getGame(gameID);
            second.send(new UserGameCommand(CONNECT, "bad-token", gameID));
            assertTrue(second.error().contains("Unauthorized"));
            second.sendText("not JSON");
            second.error();
            first.expectSilence();
            assertEquals(before, data.getGame(gameID));
            connect(second, "black-token", gameID);
            first.expectSilence();
        }
    }

    @Test
    void roleComesFromStoredSeatsRatherThanClientClaims() throws Exception {
        try (var first = new SocketTestClient(port); var observer = new SocketTestClient(port)) {
            connect(first, "white-token", gameID);
            observer.sendText("{\"commandType\":\"CONNECT\",\"authToken\":\"observer-token\","
                    + "\"gameID\":" + gameID + ",\"playerColor\":\"WHITE\",\"username\":\"Alice\"}");
            observer.next("LOAD_GAME");
            assertEquals("Charlie connected as an observer.", notice(first));
            assertNull(data.getGame(gameID).whiteUsername());
        }
    }
}
