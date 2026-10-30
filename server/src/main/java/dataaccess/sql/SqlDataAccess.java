package dataaccess.sql;

import chess.ChessGame;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.AuthData;
import model.GameData;
import model.UserData;

import java.util.Collection;

/** Adapts the SQL stores to the storage interface used by the services. */
public final class SqlDataAccess implements DataAccess {
    private final SqlUserDao users = new SqlUserDao();
    private final SqlAuthDao authorizations = new SqlAuthDao();
    private final SqlGameDao games = new SqlGameDao();
    private final SqlDatabaseCleaner cleaner = new SqlDatabaseCleaner();

    public SqlDataAccess() throws DataAccessException {
        DatabaseSchema.initialize();
    }

    @Override
    public UserData getUser(String username) throws DataAccessException {
        return users.getUser(username);
    }

    @Override
    public void createUser(UserData user) throws DataAccessException {
        users.createUser(user);
    }

    @Override
    public AuthData getAuth(String authToken) throws DataAccessException {
        return authorizations.getAuth(authToken);
    }

    @Override
    public void createAuth(AuthData auth) throws DataAccessException {
        authorizations.createAuth(auth);
    }

    @Override
    public void deleteAuth(String authToken) throws DataAccessException {
        authorizations.deleteAuth(authToken);
    }

    @Override
    public void clear() throws DataAccessException {
        cleaner.clear();
    }

    @Override
    public Collection<GameData> listGames() throws DataAccessException {
        return games.listGames();
    }

    @Override
    public int createGame(String gameName, ChessGame game) throws DataAccessException {
        return games.createGame(gameName, game);
    }

    @Override
    public GameData getGame(int gameID) throws DataAccessException {
        return games.getGame(gameID);
    }

    @Override
    public void updateGame(GameData game) throws DataAccessException {
        games.updateGame(game);
    }
}
