package chess;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import websocket.messages.LoadGameMessage;

import static org.junit.jupiter.api.Assertions.*;

class GameLifecycleTests {
    private final Gson gson = new Gson();

    @Test
    void endingGameBlocksMovesWithoutChangingPositionOrAnalysis() {
        var game = new ChessGame();
        var before = game.copy();
        assertFalse(game.isGameOver());
        game.endGame();
        game.endGame();
        assertTrue(game.isGameOver());
        assertThrows(InvalidMoveException.class, () -> game.makeMove(move()));
        assertEquals(before.getBoard(), game.getBoard());
        assertEquals(before.getTeamTurn(), game.getTeamTurn());
        assertEquals(before.validMoves(move().getStartPosition()), game.validMoves(move().getStartPosition()));
        assertNotEquals(before, game);
        assertEquals(game, game.copy());
        assertEquals(game.hashCode(), game.copy().hashCode());
    }

    @Test
    void legacyJsonWithoutGameOverStillAllowsPlay() {
        var legacy = gson.toJsonTree(new ChessGame()).getAsJsonObject();
        legacy.remove("gameOver");
        var restored = gson.fromJson(legacy, ChessGame.class);
        assertFalse(restored.isGameOver());
        assertDoesNotThrow(() -> restored.makeMove(move()));
    }

    @Test
    void endedGameRemainsEndedAfterJsonRoundTrip() {
        var game = new ChessGame();
        game.endGame();
        var restored = gson.fromJson(gson.toJson(game), ChessGame.class);
        assertTrue(restored.isGameOver());
        assertEquals(game, restored);
        assertThrows(InvalidMoveException.class, () -> restored.makeMove(move()));
    }

    @Test
    void replacingBoardDoesNotReopenAnEndedGame() {
        var game = new ChessGame();
        game.endGame();
        game.setBoard(new ChessGame().getBoard());
        assertTrue(game.isGameOver());
        assertThrows(InvalidMoveException.class, () -> game.makeMove(move()));
    }

    @Test
    void loadMessageKeepsItsSnapshotWhenSourceAndReturnedGamesChange() throws Exception {
        var game = new ChessGame();
        game.makeMove(move());
        game.endGame();
        var expected = game.copy();
        var message = new LoadGameMessage(game);
        game.getBoard().resetBoard();
        var received = message.getGame();
        received.setTeamTurn(ChessGame.TeamColor.WHITE);
        received.getBoard().resetBoard();
        var decoded = gson.fromJson(gson.toJson(message), LoadGameMessage.class);
        assertEquals(expected, decoded.getGame());
        assertTrue(decoded.getGame().isGameOver());
        assertEquals(expected, message.getGame());
    }

    private ChessMove move() {
        return new ChessMove(new ChessPosition(2, 5), new ChessPosition(4, 5), null);
    }
}
