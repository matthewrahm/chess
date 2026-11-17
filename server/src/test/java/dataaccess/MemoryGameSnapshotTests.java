package dataaccess;

import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPosition;
import model.GameData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MemoryGameSnapshotTests {
    @Test
    void creatingGameDoesNotRetainCallersMutablePosition() throws Exception {
        var data = new MemoryDataAccess();
        var supplied = new ChessGame();
        int id = data.createGame("Practice", supplied);
        supplied.makeMove(move());
        supplied.endGame();
        assertEquals(new ChessGame(), data.getGame(id).game());
    }

    @Test
    void readsAndListsCannotMutateStoredStateWithoutUpdate() throws Exception {
        var data = new MemoryDataAccess();
        int id = data.createGame("Practice", new ChessGame());
        var fetched = data.getGame(id);
        fetched.game().makeMove(move());
        fetched.game().endGame();
        var listed = data.listGames().iterator().next();
        listed.game().getBoard().addPiece(new ChessPosition(1, 1), null);
        listed.game().endGame();
        assertEquals(new ChessGame(), data.getGame(id).game());
    }

    @Test
    void updateStoresDetachedPositionAndRetainsGameOverAndSeats() throws Exception {
        var data = new MemoryDataAccess();
        int id = data.createGame("Practice", new ChessGame());
        var position = data.getGame(id).game();
        position.makeMove(move());
        position.endGame();
        var expected = new GameData(id, "white", "black", "Practice", position.copy());
        data.updateGame(new GameData(id, "white", "black", "Practice", position));
        position.getBoard().resetBoard();
        position.setTeamTurn(ChessGame.TeamColor.WHITE);
        assertEquals(expected, data.getGame(id));
        assertTrue(data.listGames().iterator().next().game().isGameOver());
    }

    @Test
    void rejectedUpdateCannotLeakEditedSnapshotIntoStoredGame() throws Exception {
        var data = new MemoryDataAccess();
        int id = data.createGame("Practice", new ChessGame());
        var original = data.getGame(id);
        var edited = original.copy();
        edited.game().makeMove(move());
        edited.game().endGame();
        assertThrows(DataAccessException.class, () -> data.updateGame(
                new GameData(id + 1, "white", "black", "Missing", edited.game())));
        assertEquals(original, data.getGame(id));
        assertNull(data.getGame(id + 1));
    }

    private ChessMove move() {
        return new ChessMove(new ChessPosition(2, 5), new ChessPosition(4, 5), null);
    }
}
