package dataaccess.sql;

import dataaccess.DataAccessException;
import dataaccess.DatabaseManager;
import model.AuthData;

import java.sql.SQLException;

/** Stores independent login sessions; deleting one token preserves other sessions. */
public final class SqlAuthDao {
    public AuthData getAuth(String authToken) throws DataAccessException {
        String sql = "SELECT auth_token, username FROM authorizations WHERE auth_token = ?";
        try (var connection = DatabaseManager.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, authToken);
            try (var results = statement.executeQuery()) {
                if (!results.next()) {
                    return null;
                }
                return new AuthData(results.getString("auth_token"), results.getString("username"));
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to read authorization", exception);
        }
    }

    public void createAuth(AuthData auth) throws DataAccessException {
        String sql = "INSERT INTO authorizations (auth_token, username) VALUES (?, ?)";
        try (var connection = DatabaseManager.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, auth.authToken());
            statement.setString(2, auth.username());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to create authorization", exception);
        }
    }

    public void deleteAuth(String authToken) throws DataAccessException {
        String sql = "DELETE FROM authorizations WHERE auth_token = ?";
        try (var connection = DatabaseManager.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, authToken);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to delete authorization", exception);
        }
    }
}
