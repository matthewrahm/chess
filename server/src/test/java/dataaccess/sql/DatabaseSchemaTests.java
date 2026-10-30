package dataaccess.sql;

import dataaccess.DataAccessException;
import dataaccess.DatabaseManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseSchemaTests {
    private DatabaseTestSupport database;

    @BeforeEach
    void setUp() throws Exception {
        database = new DatabaseTestSupport();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (database != null) {
            database.close();
        }
    }

    @Test
    void initializesAllThreeTablesInTheConfiguredDatabase() throws Exception {
        DatabaseSchema.initialize();
        try (var connection = DatabaseManager.getConnection();
             var tables = connection.getMetaData().getTables(database.databaseName(), null, "%", new String[]{"TABLE"})) {
            var names = new HashSet<String>();
            while (tables.next()) {
                names.add(tables.getString("TABLE_NAME"));
            }
            assertEquals(Set.of("users", "authorizations", "games"), names);
            assertEquals(database.databaseName(), connection.getCatalog());
        }
    }

    @Test
    void repeatedInitializationPreservesExistingRecords() throws Exception {
        DatabaseSchema.initialize();
        execute("INSERT INTO users VALUES ('player', 'stored-hash', 'player@example.com')");
        execute("INSERT INTO authorizations VALUES ('token', 'player')");
        execute("INSERT INTO games (game_name, game_json) VALUES ('Practice', '{}')");

        DatabaseSchema.initialize();

        assertEquals("stored-hash", scalar("SELECT password_hash FROM users WHERE username = 'player'"));
        assertEquals("player", scalar("SELECT username FROM authorizations WHERE auth_token = 'token'"));
        assertEquals("{}", scalar("SELECT game_json FROM games WHERE game_name = 'Practice'"));
    }

    @Test
    void userKeysPreserveCaseAndTrailingSpacesLikeMemoryStorage() throws Exception {
        DatabaseSchema.initialize();
        execute("INSERT INTO users VALUES ('Player', 'first-hash', 'one@example.com')");
        execute("INSERT INTO users VALUES ('player', 'second-hash', 'two@example.com')");
        execute("INSERT INTO users VALUES ('player ', 'third-hash', 'three@example.com')");

        assertEquals("first-hash", scalar("SELECT password_hash FROM users WHERE username = 'Player'"));
        assertEquals("second-hash", scalar("SELECT password_hash FROM users WHERE username = 'player'"));
        assertEquals("third-hash", scalar("SELECT password_hash FROM users WHERE username = 'player '"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "INSERT INTO users VALUES ('player', 'stored-hash', 'player@example.com')",
            "INSERT INTO authorizations VALUES ('token', 'player')"
    })
    void primaryKeysRejectDuplicateUsersAndTokens(String insert) throws Exception {
        DatabaseSchema.initialize();
        execute(insert);
        var error = assertThrows(SQLException.class, () -> execute(insert));
        assertEquals("23000", error.getSQLState());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "INSERT INTO users VALUES ('player', NULL, 'player@example.com')",
            "INSERT INTO users VALUES ('player', 'stored-hash', NULL)",
            "INSERT INTO authorizations VALUES ('token', NULL)",
            "INSERT INTO games (game_name, game_json) VALUES (NULL, '{}')",
            "INSERT INTO games (game_name, game_json) VALUES ('Practice', NULL)"
    })
    void requiredFieldsRejectNullValues(String insert) throws Exception {
        DatabaseSchema.initialize();
        var error = assertThrows(SQLException.class, () -> execute(insert));
        assertEquals("23000", error.getSQLState());
    }

    @Test
    void gamesGenerateDistinctIdsAndAllowUnassignedSeatsAndDuplicateNames() throws Exception {
        DatabaseSchema.initialize();
        execute("INSERT INTO games (game_name, game_json) VALUES ('Practice', '{}'), ('Practice', '{}')");
        try (var connection = DatabaseManager.getConnection();
             var statement = connection.prepareStatement("SELECT * FROM games ORDER BY game_id");
             var results = statement.executeQuery()) {
            assertTrue(results.next());
            int firstID = results.getInt("game_id");
            assertTrue(firstID > 0);
            assertNull(results.getString("white_username"));
            assertNull(results.getString("black_username"));
            assertTrue(results.next());
            assertNotEquals(firstID, results.getInt("game_id"));
            assertFalse(results.next());
        }
    }

    @Test
    void unavailableDatabasePreventsInitializationAndCanRecover() throws Exception {
        database.configureUnavailableServer();
        assertThrows(DataAccessException.class, DatabaseSchema::initialize);
        database.configureDatabase(database.databaseName());
        assertDoesNotThrow(DatabaseSchema::initialize);
    }

    private void execute(String sql) throws Exception {
        try (var connection = DatabaseManager.getConnection(); var statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private String scalar(String sql) throws Exception {
        try (var connection = DatabaseManager.getConnection();
             var statement = connection.prepareStatement(sql);
             var results = statement.executeQuery()) {
            assertTrue(results.next());
            return results.getString(1);
        }
    }
}
