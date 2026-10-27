package dataaccess.sql;

import dataaccess.DataAccessException;
import dataaccess.DatabaseManager;

import java.sql.Connection;
import java.sql.SQLException;

/** Deletes application records atomically while retaining tables and generated-ID sequences. */
public final class SqlDatabaseCleaner {
    public void clear() throws DataAccessException {
        try (var connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                deleteRecords(connection);
                connection.commit();
            } catch (SQLException failure) {
                rollback(connection, failure);
                throw failure;
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to clear database", exception);
        }
    }

    private void deleteRecords(Connection connection) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM authorizations");
            statement.executeUpdate("DELETE FROM games");
            statement.executeUpdate("DELETE FROM users");
        }
    }

    private void rollback(Connection connection, SQLException failure) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            failure.addSuppressed(rollbackFailure);
        }
    }
}
