package dataaccess;

import model.GameData;

import java.sql.SQLException;
import java.util.Collection;

public class MySqlGameDAO implements GameDAO {

    public MySqlGameDAO() throws DataAccessException {
        configureDatabase();
    }

    @Override
    public void clear() throws DataAccessException {
        throw new DataAccessException("not implemented");
    }

    @Override
    public int generateId() throws DataAccessException {
        throw new DataAccessException("not implemented");
    }

    @Override
    public void createGame(GameData game) throws DataAccessException {
        throw new DataAccessException("not implemented");
    }

    @Override
    public GameData getGame(int gameID) throws DataAccessException {
        throw new DataAccessException("not implemented");
    }

    @Override
    public Collection<GameData> listGames() throws DataAccessException {
        throw new DataAccessException("not implemented");
    }

    @Override
    public void updateGame(GameData game) throws DataAccessException {
        throw new DataAccessException("not implemented");
    }

    private void configureDatabase() throws DataAccessException {
        DatabaseManager.createDatabase();
        var createStatement = """
                CREATE TABLE IF NOT EXISTS games (
                    gameID INT NOT NULL AUTO_INCREMENT,
                    whiteUsername VARCHAR(256),
                    blackUsername VARCHAR(256),
                    gameName VARCHAR(256) NOT NULL,
                    game TEXT NOT NULL,
                    PRIMARY KEY (gameID)
                )""";
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(createStatement)) {
            stmt.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("failed to configure database", ex);
        }
    }
}
