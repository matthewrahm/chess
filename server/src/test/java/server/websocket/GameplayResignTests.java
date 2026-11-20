package server.websocket;

import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPosition;
import dataaccess.MemoryDataAccess;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import websocket.commands.MakeMoveCommand;
import websocket.commands.UserGameCommand;

import static org.junit.jupiter.api.Assertions.*;
import static websocket.commands.UserGameCommand.CommandType.*;

class GameplayResignTests extends GameSocketFixture {
    private final FailingStore store = new FailingStore();

    @Override
    protected MemoryDataAccess createDataAccess() {
        return store;
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void eitherPlayerCanResignRegardlessOfTurnWithOneNoticePerSubscriber(boolean whiteResigns) throws Exception {
        join("white-token", gameID, "WHITE");
        join("black-token", gameID, "BLACK");
        int otherID = data.createGame("Other", new ChessGame());
        String token = whiteResigns ? "white-token" : "black-token";
        String text = (whiteResigns ? "Alice" : "Bob") + " resigned. Game over.";
        try (var player = new SocketTestClient(port); var opponent = new SocketTestClient(port);
             var observer = new SocketTestClient(port); var other = new SocketTestClient(port);
             var idle = new SocketTestClient(port)) {
            connect(player, token, gameID);
            connect(opponent, whiteResigns ? "black-token" : "white-token", gameID);
            notice(player);
            connect(observer, "observer-token", gameID);
            notice(player);
            notice(opponent);
            connect(other, "observer-token", otherID);
            var original = data.getGame(gameID);
            player.send(new UserGameCommand(RESIGN, token, gameID));
            assertEquals(text, notice(player));
            assertEquals(text, notice(opponent));
            assertEquals(text, notice(observer));
            var ended = data.getGame(gameID);
            assertTrue(ended.game().isGameOver());
            assertEquals("Alice", ended.whiteUsername());
            assertEquals("Bob", ended.blackUsername());
            assertEquals(original.game().getBoard(), ended.game().getBoard());
            assertEquals(original.game().getTeamTurn(), ended.game().getTeamTurn());
            player.send(new UserGameCommand(RESIGN, token, gameID));
            assertTrue(player.error().contains("game is over"));
            player.send(new MakeMoveCommand(token, gameID, move(whiteResigns)));
            assertTrue(player.error().contains("game is over"));
            assertEquals(ended, data.getGame(gameID));
            player.expectSilence();
            opponent.expectSilence();
            observer.expectSilence();
            other.expectSilence();
            idle.expectSilence();
            assertFalse(data.getGame(otherID).game().isGameOver());
        }
    }

    @Test
    void resignationKeepsSubscriptionsAndAllowsExplicitLeave() throws Exception {
        join("white-token", gameID, "WHITE");
        try (var player = new SocketTestClient(port); var observer = new SocketTestClient(port)) {
            connect(player, "white-token", gameID);
            connect(observer, "observer-token", gameID);
            notice(player);
            player.send(new UserGameCommand(RESIGN, "white-token", gameID));
            notice(player);
            notice(observer);
            try (var reconnected = new SocketTestClient(port)) {
                assertTrue(connect(reconnected, "white-token", gameID).isGameOver());
                assertEquals("Alice connected as white.", notice(player));
                assertEquals("Alice connected as white.", notice(observer));
                player.send(new UserGameCommand(LEAVE, "white-token", gameID));
                assertEquals("Alice left the game.", notice(observer));
                assertEquals("Alice left the game.", notice(reconnected));
                assertNull(data.getGame(gameID).whiteUsername());
                assertTrue(data.getGame(gameID).game().isGameOver());
                player.expectSilence();
            }
        }
    }

    @Test
    void observersAndRevokedTokensCannotResignOrNotifyOtherClients() throws Exception {
        join("white-token", gameID, "WHITE");
        try (var player = new SocketTestClient(port); var observer = new SocketTestClient(port)) {
            connect(player, "white-token", gameID);
            connect(observer, "observer-token", gameID);
            notice(player);
            var original = data.getGame(gameID);
            observer.send(new UserGameCommand(RESIGN, "observer-token", gameID));
            assertTrue(observer.error().contains("Only players"));
            player.expectSilence();
            data.deleteAuth("white-token");
            player.send(new UserGameCommand(RESIGN, "white-token", gameID));
            assertTrue(player.error().contains("Unauthorized"));
            observer.expectSilence();
            assertEquals(original, data.getGame(gameID));
        }
    }

    @Test
    void failedResignationDoesNotEndGameOrBroadcastAndCanBeRetried() throws Exception {
        join("white-token", gameID, "WHITE");
        try (var player = new SocketTestClient(port); var observer = new SocketTestClient(port)) {
            connect(player, "white-token", gameID);
            connect(observer, "observer-token", gameID);
            notice(player);
            var original = data.getGame(gameID);
            store.fail = true;
            player.send(new UserGameCommand(RESIGN, "white-token", gameID));
            assertEquals("Error: Unable to process game command", player.error());
            assertEquals(original, data.getGame(gameID));
            observer.expectSilence();
            player.expectSilence();
            store.fail = false;
            player.send(new UserGameCommand(RESIGN, "white-token", gameID));
            assertEquals("Alice resigned. Game over.", notice(player));
            assertEquals("Alice resigned. Game over.", notice(observer));
            assertTrue(data.getGame(gameID).game().isGameOver());
        }
    }

    @Test
    void playerWhoseSeatWasReleasedByAnotherSessionCannotResign() throws Exception {
        join("white-token", gameID, "WHITE");
        try (var first = new SocketTestClient(port); var second = new SocketTestClient(port)) {
            connect(first, "white-token", gameID);
            connect(second, "white-token", gameID);
            notice(first);
            first.send(new UserGameCommand(LEAVE, "white-token", gameID));
            assertEquals("Alice left the game.", notice(second));
            second.send(new UserGameCommand(RESIGN, "white-token", gameID));
            assertTrue(second.error().contains("Only players"));
            assertFalse(data.getGame(gameID).game().isGameOver());
            first.expectSilence();
        }
    }

    private ChessMove move(boolean white) {
        return new ChessMove(new ChessPosition(white ? 2 : 7, 5), new ChessPosition(white ? 4 : 5, 5), null);
    }
}
