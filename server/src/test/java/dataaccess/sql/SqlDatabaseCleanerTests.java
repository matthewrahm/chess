package dataaccess.sql;

import chess.ChessGame;
import dataaccess.DataAccessException;
import dataaccess.DatabaseManager;
import model.AuthData;
import model.UserData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class SqlDatabaseCleanerTests {
    private DatabaseTestSupport database;
    private final SqlUserDao users = new SqlUserDao();
    private final SqlAuthDao authorizations = new SqlAuthDao();
    private final SqlGameDao games = new SqlGameDao();
    private final SqlDatabaseCleaner cleaner = new SqlDatabaseCleaner();
    private int gameID;

    @BeforeEach
    void setUp() throws Exception {
        database = new DatabaseTestSupport();
        DatabaseSchema.initialize();
        users.createUser(new UserData("player", "stored-hash", "player@example.com"));
        authorizations.createAuth(new AuthData("token", "player"));
        gameID = games.createGame("Original", new ChessGame());
    }

    @AfterEach
    void tearDown() throws Exception {
        if (database != null) {
            database.close();
        }
    }

    @Test
    void clearRemovesAllRecordsAndLeavesUsableTables() throws Exception {
        cleaner.clear();
        assertNull(users.getUser("player"));
        assertNull(authorizations.getAuth("token"));
        assertNull(games.getGame(gameID));
        assertTrue(games.listGames().isEmpty());
        assertDoesNotThrow(cleaner::clear);
        users.createUser(new UserData("player", "new-hash", "new@example.com"));
        authorizations.createAuth(new AuthData("token", "player"));
        int nextID = games.createGame("New", new ChessGame());
        assertTrue(nextID > gameID);
        assertEquals("new-hash", users.getUser("player").password());
        assertNotNull(authorizations.getAuth("token"));
        assertEquals("New", games.getGame(nextID).gameName());
    }

    @Test
    void laterDeleteFailureRollsBackPreviouslyDeletedAuthorizations() throws Exception {
        execute("""
                CREATE TRIGGER fail_game_delete BEFORE DELETE ON games FOR EACH ROW
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Injected test failure'
                """);
        var error = assertThrows(DataAccessException.class, cleaner::clear);
        assertInstanceOf(SQLException.class, error.getCause());
        assertEquals("45000", ((SQLException) error.getCause()).getSQLState());
        assertOriginalRecordsPresent();
        execute("DROP TRIGGER fail_game_delete");
        assertDoesNotThrow(cleaner::clear);
        assertNull(authorizations.getAuth("token"));
        assertTrue(games.listGames().isEmpty());
    }

    @Test
    void schemaExplicitlyUsesTransactionalTables() throws Exception {
        try (var connection = DatabaseManager.getConnection();
             var statement = connection.prepareStatement(
                     "SELECT engine FROM information_schema.tables WHERE table_schema = ?")) {
            statement.setString(1, database.databaseName());
            try (var results = statement.executeQuery()) {
                int count = 0;
                while (results.next()) {
                    assertEquals("InnoDB", results.getString("engine"));
                    count++;
                }
                assertEquals(3, count);
            }
        }
    }

    @Test
    void connectionFailurePreservesAllRecords() throws Exception {
        database.configureUnavailableServer();
        assertThrows(DataAccessException.class, cleaner::clear);
        database.configureDatabase(database.databaseName());
        assertOriginalRecordsPresent();
    }

    private void assertOriginalRecordsPresent() throws Exception {
        assertEquals(new UserData("player", "stored-hash", "player@example.com"), users.getUser("player"));
        assertEquals(new AuthData("token", "player"), authorizations.getAuth("token"));
        assertEquals("Original", games.getGame(gameID).gameName());
    }

    private void execute(String sql) throws Exception {
        try (var connection = DatabaseManager.getConnection(); var statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }
}
