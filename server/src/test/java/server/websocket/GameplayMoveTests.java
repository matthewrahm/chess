package server.websocket;

import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPosition;
import dataaccess.MemoryDataAccess;
import org.junit.jupiter.api.Test;
import websocket.commands.MakeMoveCommand;

import static org.junit.jupiter.api.Assertions.*;

class GameplayMoveTests extends GameSocketFixture {
    private final FailingStore store = new FailingStore();

    @Override
    protected MemoryDataAccess createDataAccess() {
        return store;
    }

    @Test
    void movesPersistAndBroadcastInOrderOnlyWithinTheirGame() throws Exception {
        join("white-token", gameID, "WHITE");
        join("black-token", gameID, "BLACK");
        int otherID = data.createGame("Other", new ChessGame());
        try (var white = new SocketTestClient(port); var black = new SocketTestClient(port);
             var observer = new SocketTestClient(port); var other = new SocketTestClient(port);
             var idle = new SocketTestClient(port)) {
            connect(white, "white-token", gameID);
            connect(black, "black-token", gameID);
            notice(white);
            connect(observer, "observer-token", gameID);
            notice(white);
            notice(black);
            connect(other, "white-token", otherID);
            white.send(command("white-token", 2, 5, 4, 5));
            assertSavedBoard(white);
            assertSavedBoard(black);
            assertEquals("Alice moved pawn from e2 to e4.", notice(black));
            assertSavedBoard(observer);
            assertEquals("Alice moved pawn from e2 to e4.", notice(observer));
            white.expectSilence();
            black.send(command("black-token", 7, 5, 5, 5));
            assertSavedBoard(black);
            assertSavedBoard(white);
            assertEquals("Bob moved pawn from e7 to e5.", notice(white));
            assertSavedBoard(observer);
            notice(observer);
            black.expectSilence();
            observer.expectSilence();
            other.expectSilence();
            idle.expectSilence();
            assertEquals(new ChessGame(), data.getGame(otherID).game());
            assertEquals("Alice", data.getGame(gameID).whiteUsername());
            assertEquals("Bob", data.getGame(gameID).blackUsername());
        }
    }

    @Test
    void illegalMovesAndRevokedTokensNeverChangeOrBroadcastState() throws Exception {
        join("white-token", gameID, "WHITE");
        join("black-token", gameID, "BLACK");
        try (var white = new SocketTestClient(port); var black = new SocketTestClient(port);
             var observer = new SocketTestClient(port)) {
            connect(white, "white-token", gameID);
            connect(black, "black-token", gameID);
            notice(white);
            connect(observer, "observer-token", gameID);
            notice(white);
            notice(black);
            var original = data.getGame(gameID);
            white.send(command("white-token", 7, 5, 5, 5)); // Opponent piece.
            white.error();
            white.send(command("white-token", 2, 5, 5, 5)); // Illegal distance.
            white.error();
            black.send(command("black-token", 7, 5, 5, 5)); // Wrong turn.
            black.error();
            observer.send(command("observer-token", 2, 5, 4, 5));
            observer.error();
            white.send(new MakeMoveCommand("white-token", gameID, null));
            white.error();
            data.deleteAuth("white-token");
            white.send(command("white-token", 2, 5, 4, 5));
            assertTrue(white.error().contains("Unauthorized"));
            assertEquals(original, data.getGame(gameID));
            white.expectSilence();
            black.expectSilence();
            observer.expectSilence();
        }
    }

    @Test
    void moveRequiresSubscriptionAndRejectsEndedGames() throws Exception {
        join("white-token", gameID, "WHITE");
        try (var white = new SocketTestClient(port)) {
            white.send(command("white-token", 2, 5, 4, 5));
            assertTrue(white.error().contains("Connect to this game"));
            connect(white, "white-token", gameID);
            var ended = data.getGame(gameID);
            ended.game().endGame();
            data.updateGame(ended);
            white.send(command("white-token", 2, 5, 4, 5));
            assertTrue(white.error().contains("game is over"));
            assertEquals(ended, data.getGame(gameID));
            white.expectSilence();
        }
    }

    @Test
    void failedSaveDoesNotLeakBoardOrStorageDetailsAndCanBeRetried() throws Exception {
        join("white-token", gameID, "WHITE");
        try (var white = new SocketTestClient(port); var observer = new SocketTestClient(port)) {
            connect(white, "white-token", gameID);
            connect(observer, "observer-token", gameID);
            notice(white);
            var original = data.getGame(gameID);
            store.fail = true;
            white.send(command("white-token", 2, 5, 4, 5));
            assertEquals("Error: Unable to process game command", white.error());
            assertEquals(original, data.getGame(gameID));
            observer.expectSilence();
            white.expectSilence();
            store.fail = false;
            white.send(command("white-token", 2, 5, 4, 5));
            assertSavedBoard(white);
            assertSavedBoard(observer);
            notice(observer);
            assertEquals(ChessGame.TeamColor.BLACK, data.getGame(gameID).game().getTeamTurn());
        }
    }

    private void assertSavedBoard(SocketTestClient client) throws Exception {
        var loaded = gson.fromJson(client.next("LOAD_GAME").get("game"), ChessGame.class);
        assertEquals(data.getGame(gameID).game(), loaded);
    }

    private MakeMoveCommand command(String token, int row, int col, int endRow, int endCol) {
        return new MakeMoveCommand(token, gameID,
                new ChessMove(new ChessPosition(row, col), new ChessPosition(endRow, endCol), null));
    }

}
