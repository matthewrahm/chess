package dataaccess;

import model.AuthData;

import java.sql.SQLException;

public class MySqlAuthDAO implements AuthDAO {

    public MySqlAuthDAO() throws DataAccessException {
        configureDatabase();
    }

    @Override
    public void clear() throws DataAccessException {
        throw new DataAccessException("not implemented");
    }

    @Override
    public void createAuth(AuthData auth) throws DataAccessException {
        throw new DataAccessException("not implemented");
    }

    @Override
    public AuthData getAuth(String authToken) throws DataAccessException {
        throw new DataAccessException("not implemented");
    }

    @Override
    public void deleteAuth(String authToken) throws DataAccessException {
        throw new DataAccessException("not implemented");
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
