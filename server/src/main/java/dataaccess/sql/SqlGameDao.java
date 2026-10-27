package dataaccess.sql;

import chess.ChessGame;
import dataaccess.DataAccessException;
import dataaccess.DatabaseManager;
import model.GameData;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;

/** Persists game metadata and independent snapshots of the complete chess state. */
public final class SqlGameDao {
    private static final String SELECT_GAMES =
            "SELECT game_id, white_username, black_username, game_name, game_json FROM games";

    public int createGame(String gameName, ChessGame game) throws DataAccessException {
        String json = GameJson.encode(game);
        String sql = "INSERT INTO games (game_name, game_json) VALUES (?, ?)";
        try (var connection = DatabaseManager.getConnection();
             var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, gameName);
            statement.setString(2, json);
            statement.executeUpdate();
            try (var keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new DataAccessException("Database did not return a game ID");
                }
                return keys.getInt(1);
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to create game", exception);
        }
    }

    public GameData getGame(int gameID) throws DataAccessException {
        try (var connection = DatabaseManager.getConnection();
             var statement = connection.prepareStatement(SELECT_GAMES + " WHERE game_id = ?")) {
            statement.setInt(1, gameID);
            try (var results = statement.executeQuery()) {
                return results.next() ? readGame(results) : null;
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to read game", exception);
        }
    }

    public Collection<GameData> listGames() throws DataAccessException {
        try (var connection = DatabaseManager.getConnection();
             var statement = connection.prepareStatement(SELECT_GAMES);
             var results = statement.executeQuery()) {
            var games = new ArrayList<GameData>();
            while (results.next()) {
                games.add(readGame(results));
            }
            return games;
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to list games", exception);
        }
    }

    public void updateGame(GameData game) throws DataAccessException {
        String json = GameJson.encode(game.game());
        String sql = """
                UPDATE games SET white_username = ?, black_username = ?, game_name = ?, game_json = ?
                WHERE game_id = ?
                """;
        try (var connection = DatabaseManager.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, game.whiteUsername());
            statement.setString(2, game.blackUsername());
            statement.setString(3, game.gameName());
            statement.setString(4, json);
            statement.setInt(5, game.gameID());
            if (statement.executeUpdate() == 0) {
                throw new DataAccessException("Game does not exist");
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to update game", exception);
        }
    }

    private GameData readGame(ResultSet results) throws SQLException, DataAccessException {
        return new GameData(results.getInt("game_id"), results.getString("white_username"),
                results.getString("black_username"), results.getString("game_name"),
                GameJson.decode(results.getString("game_json")));
    }
}
