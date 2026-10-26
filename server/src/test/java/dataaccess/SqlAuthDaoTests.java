package dataaccess;

import dataaccess.sql.DatabaseSchema;
import dataaccess.sql.SqlAuthDao;
import dataaccess.sql.SqlUserDao;
import model.AuthData;
import model.UserData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class SqlAuthDaoTests {
    private DatabaseTestSupport database;
    private SqlAuthDao authorizations;

    @BeforeEach
    void setUp() throws Exception {
        database = new DatabaseTestSupport();
        DatabaseSchema.initialize();
        authorizations = new SqlAuthDao();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (database != null) {
            database.close();
        }
    }

    @Test
    void multipleSessionsForOneUserSurviveNewDaoAndSchemaInitialization() throws Exception {
        var first = new AuthData("first-token", "player");
        var second = new AuthData("second-token", "player");
        authorizations.createAuth(first);
        authorizations.createAuth(second);
        DatabaseSchema.initialize();
        var reopened = new SqlAuthDao();
        assertEquals(first, reopened.getAuth(first.authToken()));
        assertEquals(second, reopened.getAuth(second.authToken()));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"missing", "' OR '1'='1"})
    void missingTokenLookupAndDeletionLeaveExistingSessionIntact(String token) throws Exception {
        var existing = new AuthData("existing-token", "player");
        authorizations.createAuth(existing);
        assertNull(authorizations.getAuth(token));
        assertDoesNotThrow(() -> authorizations.deleteAuth(token));
        assertEquals(existing, authorizations.getAuth(existing.authToken()));
    }

    @Test
    void duplicateTokenCannotChangeSessionOwner() throws Exception {
        var original = new AuthData("token", "original-player");
        authorizations.createAuth(original);
        var error = assertThrows(DataAccessException.class,
                () -> authorizations.createAuth(new AuthData("token", "other-player")));
        assertInstanceOf(SQLException.class, error.getCause());
        assertEquals(original, authorizations.getAuth("token"));
    }

    @Test
    void deletingOneTokenPreservesOtherSessionsAndUserRecord() throws Exception {
        var users = new SqlUserDao();
        var user = new UserData("player", "stored-hash", "player@example.com");
        users.createUser(user);
        authorizations.createAuth(new AuthData("first", "player"));
        var sameUser = new AuthData("second", "player");
        var otherUser = new AuthData("third", "other-player");
        authorizations.createAuth(sameUser);
        authorizations.createAuth(otherUser);

        authorizations.deleteAuth("first");
        var reopened = new SqlAuthDao();
        assertNull(reopened.getAuth("first"));
        assertEquals(sameUser, reopened.getAuth("second"));
        assertEquals(otherUser, reopened.getAuth("third"));
        assertEquals(user, users.getUser("player"));
        assertDoesNotThrow(() -> reopened.deleteAuth("first"));
    }

    @Test
    void tokenKeysPreserveCaseSpacesUnicodeAndSqlPunctuation() throws Exception {
        for (String token : new String[]{"Token", "token", "token ", "令牌", "'; DROP TABLE authorizations; --"}) {
            var auth = new AuthData(token, "O'Brien 棋手");
            authorizations.createAuth(auth);
            assertEquals(auth, authorizations.getAuth(token));
        }
        authorizations.deleteAuth("token");
        assertNull(authorizations.getAuth("token"));
        assertNotNull(authorizations.getAuth("Token"));
        assertNotNull(authorizations.getAuth("token "));
    }

    @Test
    void nullRequiredFieldsAreStorageErrors() {
        var tokenError = assertThrows(DataAccessException.class,
                () -> authorizations.createAuth(new AuthData(null, "player")));
        var usernameError = assertThrows(DataAccessException.class,
                () -> authorizations.createAuth(new AuthData("token", null)));
        assertInstanceOf(SQLException.class, tokenError.getCause());
        assertInstanceOf(SQLException.class, usernameError.getCause());
    }

    @Test
    void oversizedTokenFailsWithoutInsertingTruncatedSession() throws Exception {
        assertThrows(DataAccessException.class,
                () -> authorizations.createAuth(new AuthData("x".repeat(65), "player")));
        assertNull(authorizations.getAuth("x".repeat(64)));
    }

    @Test
    void missingTableReportsStorageErrorsForAllOperations() throws Exception {
        try (var connection = DatabaseManager.getConnection(); var statement = connection.createStatement()) {
            statement.executeUpdate("DROP TABLE authorizations");
        }
        var readError = assertThrows(DataAccessException.class, () -> authorizations.getAuth("token"));
        var writeError = assertThrows(DataAccessException.class,
                () -> authorizations.createAuth(new AuthData("token", "player")));
        var deleteError = assertThrows(DataAccessException.class, () -> authorizations.deleteAuth("token"));
        assertInstanceOf(SQLException.class, readError.getCause());
        assertInstanceOf(SQLException.class, writeError.getCause());
        assertInstanceOf(SQLException.class, deleteError.getCause());
    }

    @Test
    void connectionFailureCannotSilentlyDeleteOrCreateSession() throws Exception {
        var existing = new AuthData("token", "player");
        authorizations.createAuth(existing);
        database.configureUnavailableServer();
        assertThrows(DataAccessException.class, () -> authorizations.getAuth("token"));
        assertThrows(DataAccessException.class,
                () -> authorizations.createAuth(new AuthData("new-token", "player")));
        assertThrows(DataAccessException.class, () -> authorizations.deleteAuth("token"));
        database.configureDatabase(database.databaseName());
        assertEquals(existing, authorizations.getAuth("token"));
        assertNull(authorizations.getAuth("new-token"));
    }
}
