package dataaccess;

import model.AuthData;

import java.sql.SQLException;

public class MySqlAuthDAO implements AuthDAO {

    public MySqlAuthDAO() throws DataAccessException {
        configureDatabase();
    }

    @Override
    public void clear() throws DataAccessException {
        var sql = "TRUNCATE TABLE auths";
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("failed to clear auths", ex);
        }
    }

    @Override
    public void createAuth(AuthData auth) throws DataAccessException {
        var sql = "INSERT INTO auths (authToken, username) VALUES (?, ?)";
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, auth.authToken());
            stmt.setString(2, auth.username());
            stmt.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("failed to create auth", ex);
        }
    }

    @Override
    public AuthData getAuth(String authToken) throws DataAccessException {
        var sql = "SELECT authToken, username FROM auths WHERE authToken=?";
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, authToken);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new AuthData(rs.getString("authToken"), rs.getString("username"));
                }
                return null;
            }
        } catch (SQLException ex) {
            throw new DataAccessException("failed to get auth", ex);
        }
    }

    @Override
    public void deleteAuth(String authToken) throws DataAccessException {
        var sql = "DELETE FROM auths WHERE authToken=?";
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, authToken);
            stmt.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("failed to delete auth", ex);
        }
    }

    private void configureDatabase() throws DataAccessException {
        DatabaseManager.createDatabase();
        var createStatement = """
                CREATE TABLE IF NOT EXISTS auths (
                    authToken VARCHAR(256) NOT NULL,
                    username VARCHAR(256) NOT NULL,
                    PRIMARY KEY (authToken)
                )""";
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(createStatement)) {
            stmt.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("failed to configure database", ex);
        }
    }
}
