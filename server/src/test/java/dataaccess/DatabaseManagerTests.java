package dataaccess;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseManagerTests {
    @Test
    void createsDatabaseAndSelectsItOnIndependentConnections() throws Exception {
        try (var database = new DatabaseTestSupport()) {
            DatabaseManager.createDatabase();
            DatabaseManager.createDatabase();
            try (var first = DatabaseManager.getConnection();
                 var second = DatabaseManager.getConnection();
                 var statement = second.prepareStatement("SELECT DATABASE(), 1 + 1");
                 var result = statement.executeQuery()) {
                assertNotSame(first, second);
                assertEquals(database.databaseName(), first.getCatalog());
                assertTrue(result.next());
                assertEquals(database.databaseName(), result.getString(1));
                assertEquals(2, result.getInt(2));
                assertFalse(result.next());
            }
        }
    }

    @Test
    void unavailableServerProducesStorageErrorsAndConfigurationCanRecover() throws Exception {
        try (var database = new DatabaseTestSupport()) {
            database.configureUnavailableServer();
            var creation = assertThrows(DataAccessException.class, DatabaseManager::createDatabase);
            var connection = assertThrows(DataAccessException.class, DatabaseManager::getConnection);
            assertInstanceOf(SQLException.class, creation.getCause());
            assertInstanceOf(SQLException.class, connection.getCause());
            database.configureDatabase(database.databaseName());
            assertDoesNotThrow(DatabaseManager::createDatabase);
        }
    }

    @Test
    void missingCatalogProducesStorageErrorInsteadOfUsingAnotherDatabase() throws Exception {
        try (var database = new DatabaseTestSupport()) {
            var error = assertThrows(DataAccessException.class, DatabaseManager::getConnection);
            assertInstanceOf(SQLException.class, error.getCause());
            assertTrue(error.getCause().getMessage().contains(database.databaseName()));
        }
    }
}
