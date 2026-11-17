package dataaccess.sql;

import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPosition;
import chess.InvalidMoveException;
import com.google.gson.Gson;
import dataaccess.DataAccessException;
import dataaccess.DatabaseManager;
import model.GameData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SqlGameLifecycleTests {
    private TestDatabaseSupport database;
    private SqlGameDao games;
    private int id;

    @BeforeEach
    void setUp() throws Exception {
        database = new TestDatabaseSupport();
        DatabaseSchema.initialize();
        games = new SqlGameDao();
        id = games.createGame("Practice", new ChessGame());
    }

    @AfterEach
    void tearDown() throws Exception {
        if (database != null) {
            database.close();
        }
    }

    @Test
    void finishedGameAndSeatsSurviveNewStorageInstance() throws Exception {
        var game = games.getGame(id).game();
        game.makeMove(move());
        game.endGame();
        var ended = new GameData(id, "white", "black", "Practice", game);
        games.updateGame(ended);
        var restored = new SqlDataAccess().getGame(id);
        assertEquals(ended, restored);
        assertTrue(restored.game().isGameOver());
        assertEquals("white", restored.whiteUsername());
        assertEquals("black", restored.blackUsername());
        assertThrows(InvalidMoveException.class, () -> restored.game().makeMove(
                new ChessMove(new ChessPosition(7, 5), new ChessPosition(5, 5), null)));
        assertTrue(new SqlGameDao().listGames().iterator().next().game().isGameOver());
    }

    @Test
    void legacyDatabaseRowLoadsAsPlayableWithoutSchemaMigration() throws Exception {
        var json = new Gson().toJsonTree(new ChessGame()).getAsJsonObject();
        json.remove("gameOver");
        try (var connection = DatabaseManager.getConnection();
             var statement = connection.prepareStatement("UPDATE games SET game_json = ? WHERE game_id = ?")) {
            statement.setString(1, json.toString());
            statement.setInt(2, id);
            assertEquals(1, statement.executeUpdate());
        }
        var restored = new SqlGameDao().getGame(id);
        assertFalse(restored.game().isGameOver());
        restored.game().makeMove(move());
        games.updateGame(restored);
        assertEquals(ChessGame.TeamColor.BLACK, games.getGame(id).game().getTeamTurn());
    }

    @Test
    void failedWriteDoesNotPersistMovesOrEndState() throws Exception {
        var original = games.getGame(id);
        var candidate = original.copy();
        candidate.game().makeMove(move());
        candidate.game().endGame();
        database.configureUnavailableServer();
        assertThrows(DataAccessException.class, () -> games.updateGame(candidate));
        database.configureDatabase(database.databaseName());
        assertEquals(original, new SqlGameDao().getGame(id));
        assertFalse(games.getGame(id).game().isGameOver());
    }

    private ChessMove move() {
        return new ChessMove(new ChessPosition(2, 5), new ChessPosition(4, 5), null);
    }
}
