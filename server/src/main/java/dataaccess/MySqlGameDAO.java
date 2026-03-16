package dataaccess;

import chess.ChessGame;
import com.google.gson.Gson;
import model.GameData;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;

public class MySqlGameDAO implements GameDAO {
    private final Gson gson = new Gson();

    public MySqlGameDAO() throws DataAccessException {
        configureDatabase();
    }

    @Override
    public void clear() throws DataAccessException {
        var sql = "TRUNCATE TABLE games";
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("failed to clear games", ex);
        }
    }

    @Override
    public int generateId() throws DataAccessException {
        var sql = "INSERT INTO games (gameName, game) VALUES ('temp', '{}')";
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.executeUpdate();
            try (var keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    // Delete the temp row; createGame will insert the real one
                    try (var del = conn.prepareStatement("DELETE FROM games WHERE gameID=?")) {
                        del.setInt(1, id);
                        del.executeUpdate();
                    }
                    return id;
                }
            }
            throw new DataAccessException("failed to generate game id");
        } catch (SQLException ex) {
            throw new DataAccessException("failed to generate game id", ex);
        }
    }

    @Override
    public void createGame(GameData game) throws DataAccessException {
        var sql = "INSERT INTO games (gameID, whiteUsername, blackUsername, gameName, game) VALUES (?, ?, ?, ?, ?)";
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, game.gameID());
            stmt.setString(2, game.whiteUsername());
            stmt.setString(3, game.blackUsername());
            stmt.setString(4, game.gameName());
            stmt.setString(5, gson.toJson(game.game()));
            stmt.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("failed to create game", ex);
        }
    }

    @Override
    public GameData getGame(int gameID) throws DataAccessException {
        var sql = "SELECT gameID, whiteUsername, blackUsername, gameName, game FROM games WHERE gameID=?";
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, gameID);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return readGame(rs);
                }
                return null;
            }
        } catch (SQLException ex) {
            throw new DataAccessException("failed to get game", ex);
        }
    }

    @Override
    public Collection<GameData> listGames() throws DataAccessException {
        var sql = "SELECT gameID, whiteUsername, blackUsername, gameName, game FROM games";
        var games = new ArrayList<GameData>();
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql);
             var rs = stmt.executeQuery()) {
            while (rs.next()) {
                games.add(readGame(rs));
            }
            return games;
        } catch (SQLException ex) {
            throw new DataAccessException("failed to list games", ex);
        }
    }

    @Override
    public void updateGame(GameData game) throws DataAccessException {
        var sql = "UPDATE games SET whiteUsername=?, blackUsername=?, gameName=?, game=? WHERE gameID=?";
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, game.whiteUsername());
            stmt.setString(2, game.blackUsername());
            stmt.setString(3, game.gameName());
            stmt.setString(4, gson.toJson(game.game()));
            stmt.setInt(5, game.gameID());
            stmt.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("failed to update game", ex);
        }
    }

    private GameData readGame(ResultSet rs) throws SQLException {
        int gameID = rs.getInt("gameID");
        String whiteUsername = rs.getString("whiteUsername");
        String blackUsername = rs.getString("blackUsername");
        String gameName = rs.getString("gameName");
        ChessGame game = gson.fromJson(rs.getString("game"), ChessGame.class);
        return new GameData(gameID, whiteUsername, blackUsername, gameName, game);
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
