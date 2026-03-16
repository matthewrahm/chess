package dataaccess;

import model.UserData;

import java.sql.SQLException;

public class MySqlUserDAO implements UserDAO {

    public MySqlUserDAO() throws DataAccessException {
        configureDatabase();
    }

    @Override
    public void clear() throws DataAccessException {
        var sql = "TRUNCATE TABLE users";
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("failed to clear users", ex);
        }
    }

    @Override
    public void createUser(UserData user) throws DataAccessException {
        var sql = "INSERT INTO users (username, password, email) VALUES (?, ?, ?)";
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.username());
            stmt.setString(2, user.password());
            stmt.setString(3, user.email());
            stmt.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("failed to create user", ex);
        }
    }

    @Override
    public UserData getUser(String username) throws DataAccessException {
        var sql = "SELECT username, password, email FROM users WHERE username=?";
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new UserData(rs.getString("username"), rs.getString("password"), rs.getString("email"));
                }
                return null;
            }
        } catch (SQLException ex) {
            throw new DataAccessException("failed to get user", ex);
        }
    }

    private void configureDatabase() throws DataAccessException {
        DatabaseManager.createDatabase();
        var createStatement = """
                CREATE TABLE IF NOT EXISTS users (
                    username VARCHAR(256) NOT NULL,
                    password VARCHAR(256) NOT NULL,
                    email VARCHAR(256) NOT NULL,
                    PRIMARY KEY (username)
                )""";
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(createStatement)) {
            stmt.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("failed to configure database", ex);
        }
    }
}
