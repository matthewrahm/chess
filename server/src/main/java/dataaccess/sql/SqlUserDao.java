package dataaccess.sql;

import dataaccess.DataAccessException;
import dataaccess.DatabaseManager;
import dataaccess.DuplicateUserException;
import model.UserData;

import java.sql.SQLException;

/** Persists users whose passwords have already been hashed by UserService. */
public final class SqlUserDao {
    private static final int DUPLICATE_KEY = 1062;

    public UserData getUser(String username) throws DataAccessException {
        String sql = "SELECT username, password_hash, email FROM users WHERE username = ?";
        try (var connection = DatabaseManager.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (var results = statement.executeQuery()) {
                if (!results.next()) {
                    return null;
                }
                return new UserData(results.getString("username"), results.getString("password_hash"),
                        results.getString("email"));
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to read user", exception);
        }
    }

    public void createUser(UserData user) throws DataAccessException {
        String sql = "INSERT INTO users (username, password_hash, email) VALUES (?, ?, ?)";
        try (var connection = DatabaseManager.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.username());
            statement.setString(2, user.password());
            statement.setString(3, user.email());
            statement.executeUpdate();
        } catch (SQLException exception) {
            if (exception.getErrorCode() == DUPLICATE_KEY) {
                throw new DuplicateUserException();
            }
            throw new DataAccessException("Unable to create user", exception);
        }
    }
}
