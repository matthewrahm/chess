package dataaccess;

import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPosition;
import dataaccess.sql.DatabaseSchema;
import dataaccess.sql.SqlGameDao;
import model.GameData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class SqlGameUpdateTests {
    private DatabaseTestSupport database;
    private SqlGameDao games;
    private int gameID;

    @BeforeEach
    void setUp() throws Exception {
        database = new DatabaseTestSupport();
        DatabaseSchema.initialize();
        games = new SqlGameDao();
        gameID = games.createGame("Original", new ChessGame());
    }

    @AfterEach
    void tearDown() throws Exception {
        if (database != null) {
            database.close();
        }
    }

    @Test
    void updatesPlayersNameBoardAndTurnWithoutChangingOtherGames() throws Exception {
        int otherID = games.createGame("Untouched", new ChessGame());
        var other = games.getGame(otherID);
        var position = new ChessGame();
        position.makeMove(new ChessMove(new ChessPosition(2, 5), new ChessPosition(4, 5), null));
        var updated = new GameData(gameID, "O'Brien", "棋手", "Updated 'game'", position);
        games.updateGame(updated);

        assertEquals(updated, new SqlGameDao().getGame(gameID));
        assertEquals(other, games.getGame(otherID));
        assertTrue(games.listGames().contains(updated));
        position.setTeamTurn(ChessGame.TeamColor.WHITE);
        assertEquals(ChessGame.TeamColor.BLACK, games.getGame(gameID).game().getTeamTurn());
    }

    @Test
    void acceptsUnchangedRecordAndCanVacateBothSeats() throws Exception {
        var original = games.getGame(gameID);
        assertDoesNotThrow(() -> games.updateGame(original));
        games.updateGame(new GameData(gameID, "white", "black", "Original", original.game()));
        games.updateGame(new GameData(gameID, null, "black", "Original", original.game()));
        assertNull(games.getGame(gameID).whiteUsername());
        assertEquals("black", games.getGame(gameID).blackUsername());
        games.updateGame(original);
        assertEquals(original, games.getGame(gameID));
    }

    @Test
    void absentIdIsAnErrorAndDoesNotInsertAGame() throws Exception {
        assertThrows(DataAccessException.class,
                () -> games.updateGame(new GameData(Integer.MAX_VALUE, null, null, "Missing", new ChessGame())));
        assertNull(games.getGame(Integer.MAX_VALUE));
        assertEquals(1, games.listGames().size());
    }

    @Test
    void invalidUpdatePreservesPreviousRecord() throws Exception {
        var original = games.getGame(gameID);
        var error = assertThrows(DataAccessException.class,
                () -> games.updateGame(new GameData(gameID, "white", "black", null, new ChessGame())));
        assertInstanceOf(SQLException.class, error.getCause());
        assertThrows(DataAccessException.class,
                () -> games.updateGame(new GameData(gameID, "white", null, "Invalid", null)));
        assertEquals(original, games.getGame(gameID));
    }

    @Test
    void missingTableIsReportedAsStorageFailure() throws Exception {
        var original = games.getGame(gameID);
        try (var connection = DatabaseManager.getConnection(); var statement = connection.createStatement()) {
            statement.executeUpdate("DROP TABLE games");
        }
        var error = assertThrows(DataAccessException.class, () -> games.updateGame(original));
        assertInstanceOf(SQLException.class, error.getCause());
    }

    @Test
    void connectionFailureLeavesPersistedGameUnchanged() throws Exception {
        var original = games.getGame(gameID);
        database.configureUnavailableServer();
        assertThrows(DataAccessException.class,
                () -> games.updateGame(new GameData(gameID, "white", "black", "New", new ChessGame())));
        database.configureDatabase(database.databaseName());
        assertEquals(original, games.getGame(gameID));
    }
}
