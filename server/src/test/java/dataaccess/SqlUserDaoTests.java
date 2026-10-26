package dataaccess;

import dataaccess.sql.DatabaseSchema;
import dataaccess.sql.SqlUserDao;
import model.UserData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class SqlUserDaoTests {
    private DatabaseTestSupport database;
    private SqlUserDao users;

    @BeforeEach
    void setUp() throws Exception {
        database = new DatabaseTestSupport();
        DatabaseSchema.initialize();
        users = new SqlUserDao();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (database != null) {
            database.close();
        }
    }

    @Test
    void userSurvivesNewDaoAndSchemaInitialization() throws Exception {
        var user = new UserData("player", "stored-hash", "player@example.com");
        users.createUser(user);
        DatabaseSchema.initialize();
        assertEquals(user, new SqlUserDao().getUser("player"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"missing", "' OR '1'='1"})
    void absentUsernameReturnsNull(String username) throws Exception {
        users.createUser(new UserData("player", "stored-hash", "player@example.com"));
        assertNull(users.getUser(username));
    }

    @Test
    void duplicateUsernameDoesNotReplaceOriginalRecord() throws Exception {
        var original = new UserData("player", "original-hash", "original@example.com");
        users.createUser(original);
        assertThrows(DuplicateUserException.class,
                () -> users.createUser(new UserData("player", "replacement-hash", "new@example.com")));
        assertEquals(original, users.getUser("player"));
    }

    @Test
    void caseSpacesUnicodeAndSqlPunctuationAreStoredLiterally() throws Exception {
        for (String username : new String[]{"Player", "player", "player ", "棋手", "O'Brien'; DROP TABLE users; --"}) {
            var user = new UserData(username, "stored-hash", "o'brien@example.com");
            users.createUser(user);
            assertEquals(user, users.getUser(username));
        }
        assertEquals("Player", users.getUser("Player").username());
    }

    @Test
    void bcryptHashIsStoredUnchangedAndStillVerifies() throws Exception {
        String password = "correct horse battery staple";
        String hash = BCrypt.hashpw(password, BCrypt.gensalt(4));
        users.createUser(new UserData("player", hash, "player@example.com"));
        try (var connection = DatabaseManager.getConnection();
             var statement = connection.prepareStatement("SELECT password_hash FROM users WHERE username = 'player'");
             var results = statement.executeQuery()) {
            assertTrue(results.next());
            assertEquals(hash, results.getString(1));
            assertNotEquals(password, results.getString(1));
        }
        String storedHash = new SqlUserDao().getUser("player").password();
        assertTrue(BCrypt.checkpw(password, storedHash));
        assertFalse(BCrypt.checkpw("wrong password", storedHash));
    }

    @Test
    void nullRequiredFieldIsStorageErrorRatherThanDuplicate() {
        var error = assertThrows(DataAccessException.class,
                () -> users.createUser(new UserData("player", "stored-hash", null)));
        assertFalse(error instanceof DuplicateUserException);
        assertInstanceOf(SQLException.class, error.getCause());
    }

    @Test
    void oversizedUsernameFailsWithoutInsertingTruncatedRecord() throws Exception {
        var error = assertThrows(DataAccessException.class,
                () -> users.createUser(new UserData("x".repeat(256), "stored-hash", "player@example.com")));
        assertInstanceOf(SQLException.class, error.getCause());
        assertNull(users.getUser("x".repeat(255)));
    }

    @Test
    void missingTableIsStorageErrorRatherThanMissingUser() throws Exception {
        try (var connection = DatabaseManager.getConnection(); var statement = connection.createStatement()) {
            statement.executeUpdate("DROP TABLE users");
        }
        var error = assertThrows(DataAccessException.class, () -> users.getUser("player"));
        assertInstanceOf(SQLException.class, error.getCause());
    }

    @Test
    void connectionFailureDoesNotPretendLookupOrCreationSucceeded() throws Exception {
        database.configureUnavailableServer();
        assertThrows(DataAccessException.class, () -> users.getUser("player"));
        assertThrows(DataAccessException.class,
                () -> users.createUser(new UserData("player", "stored-hash", "player@example.com")));
        database.configureDatabase(database.databaseName());
        assertNull(users.getUser("player"));
    }
}
