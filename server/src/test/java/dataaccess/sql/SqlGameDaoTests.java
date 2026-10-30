package dataaccess.sql;

import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPosition;
import dataaccess.DataAccessException;
import dataaccess.DatabaseManager;
import model.GameData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.SQLException;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class SqlGameDaoTests {
    private DatabaseTestSupport database;
    private SqlGameDao games;

    @BeforeEach
    void setUp() throws Exception {
        database = new DatabaseTestSupport();
        DatabaseSchema.initialize();
        games = new SqlGameDao();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (database != null) {
            database.close();
        }
    }

    @Test
    void initialBoardAndEmptySeatsSurviveNewDaoAndSchemaInitialization() throws Exception {
        var original = new ChessGame();
        int id = games.createGame("Practice", original);
        DatabaseSchema.initialize();
        GameData stored = new SqlGameDao().getGame(id);
        assertEquals(new GameData(id, null, null, "Practice", original), stored);
        assertTrue(id > 0);
        assertNotSame(original, stored.game());
        for (int row = 1; row <= 8; row++) {
            for (int column = 1; column <= 8; column++) {
                var position = new ChessPosition(row, column);
                assertEquals(original.getBoard().getPiece(position), stored.game().getBoard().getPiece(position));
            }
        }
    }

    @Test
    void storesSuppliedPositionAndTurnWithoutResettingTheGame() throws Exception {
        var original = new ChessGame();
        original.makeMove(new ChessMove(new ChessPosition(2, 5), new ChessPosition(4, 5), null));
        int id = games.createGame("In progress", original);
        var stored = games.getGame(id).game();
        assertEquals(original, stored);
        assertEquals(ChessGame.TeamColor.BLACK, stored.getTeamTurn());
        assertNull(stored.getBoard().getPiece(new ChessPosition(2, 5)));
        assertNotNull(stored.getBoard().getPiece(new ChessPosition(4, 5)));
    }

    @Test
    void duplicateAndQuotedNamesHaveIndependentGeneratedIds() throws Exception {
        String name = "O'Brien 棋局'; DROP TABLE games; --";
        int first = games.createGame(name, new ChessGame());
        int second = new SqlGameDao().createGame(name, new ChessGame());
        assertNotEquals(first, second);
        assertEquals(name, games.getGame(first).gameName());
        assertEquals(name, games.getGame(second).gameName());
        assertEquals(Set.of(first, second), games.listGames().stream().map(GameData::gameID).collect(Collectors.toSet()));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 2147483647})
    void absentIdReturnsNull(int id) throws Exception {
        games.createGame("Existing", new ChessGame());
        assertNull(games.getGame(id));
    }

    @Test
    void emptyDatabaseReturnsEmptyCollection() throws Exception {
        assertNotNull(games.listGames());
        assertTrue(games.listGames().isEmpty());
    }

    @Test
    void inputAndReturnedObjectsCannotChangePersistedSnapshots() throws Exception {
        var original = new ChessGame();
        int id = games.createGame("Snapshot", original);
        original.setTeamTurn(ChessGame.TeamColor.BLACK);
        var found = games.getGame(id);
        found.game().getBoard().addPiece(new ChessPosition(1, 1), null);
        var listed = games.listGames();
        listed.iterator().next().game().setTeamTurn(ChessGame.TeamColor.BLACK);
        listed.clear();
        assertEquals(new ChessGame(), games.getGame(id).game());
        assertEquals(1, games.listGames().size());
    }

    @Test
    void invalidGameAndNameCannotCreateRows() throws Exception {
        var noBoard = new ChessGame();
        noBoard.setBoard(null);
        var noTurn = new ChessGame();
        noTurn.setTeamTurn(null);
        assertThrows(DataAccessException.class, () -> games.createGame("Null game", null));
        assertThrows(DataAccessException.class, () -> games.createGame("Null board", noBoard));
        assertThrows(DataAccessException.class, () -> games.createGame("Null turn", noTurn));
        assertThrows(DataAccessException.class, () -> games.createGame(null, new ChessGame()));
        assertThrows(DataAccessException.class, () -> games.createGame("x".repeat(256), new ChessGame()));
        assertTrue(games.listGames().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-json", "null", "{\"board\":null}", "{\"teamTurn\":null}"})
    void corruptStoredGameFailsInsteadOfReturningAUsableGame(String json) throws Exception {
        int id = games.createGame("Damaged", new ChessGame());
        try (var connection = DatabaseManager.getConnection();
             var statement = connection.prepareStatement("UPDATE games SET game_json = ? WHERE game_id = ?")) {
            statement.setString(1, json);
            statement.setInt(2, id);
            statement.executeUpdate();
        }
        assertThrows(DataAccessException.class, () -> games.getGame(id));
        assertThrows(DataAccessException.class, games::listGames);
    }

    @Test
    void missingTableReportsSqlErrorsForAllOperations() throws Exception {
        try (var connection = DatabaseManager.getConnection(); var statement = connection.createStatement()) {
            statement.executeUpdate("DROP TABLE games");
        }
        var readError = assertThrows(DataAccessException.class, () -> games.getGame(1));
        var listError = assertThrows(DataAccessException.class, games::listGames);
        var writeError = assertThrows(DataAccessException.class, () -> games.createGame("Game", new ChessGame()));
        assertInstanceOf(SQLException.class, readError.getCause());
        assertInstanceOf(SQLException.class, listError.getCause());
        assertInstanceOf(SQLException.class, writeError.getCause());
    }

    @Test
    void connectionFailureCannotPretendReadsOrCreationSucceeded() throws Exception {
        int id = games.createGame("Existing", new ChessGame());
        database.configureUnavailableServer();
        assertThrows(DataAccessException.class, () -> games.getGame(id));
        assertThrows(DataAccessException.class, games::listGames);
        assertThrows(DataAccessException.class, () -> games.createGame("New", new ChessGame()));
        database.configureDatabase(database.databaseName());
        assertEquals("Existing", games.getGame(id).gameName());
        assertEquals(1, games.listGames().size());
    }
}
