package dataaccess;

import chess.ChessGame;
import model.AuthData;
import model.GameData;
import model.UserData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/** Each server instance owns its own in-memory store. */
public class MemoryDataAccess implements DataAccess {
    private final Map<String, UserData> users = new HashMap<>();
    private final Map<String, AuthData> authorizations = new HashMap<>();
    private final Map<Integer, GameData> games = new HashMap<>();
    private int nextGameID = 1;

    @Override
    public synchronized UserData getUser(String username) {
        return users.get(username);
    }

    @Override
    public synchronized void createUser(UserData user) throws DataAccessException {
        if (users.putIfAbsent(user.username(), user) != null) {
            throw new DataAccessException("Username already exists");
        }
    }

    @Override
    public synchronized AuthData getAuth(String authToken) {
        return authorizations.get(authToken);
    }

    @Override
    public synchronized void createAuth(AuthData auth) throws DataAccessException {
        if (authorizations.putIfAbsent(auth.authToken(), auth) != null) {
            throw new DataAccessException("Authorization token already exists");
        }
    }

    @Override
    public synchronized void deleteAuth(String authToken) {
        authorizations.remove(authToken);
    }

    @Override
    public synchronized void clear() {
        authorizations.clear();
        games.clear();
        users.clear();
    }

    @Override
    public synchronized Collection<GameData> listGames() {
        return new ArrayList<>(games.values());
    }

    @Override
    public synchronized int createGame(String gameName, ChessGame game) {
        int gameID = nextGameID++;
        games.put(gameID, new GameData(gameID, null, null, gameName, game));
        return gameID;
    }

    @Override
    public synchronized GameData getGame(int gameID) {
        return games.get(gameID);
    }

    @Override
    public synchronized void updateGame(GameData game) throws DataAccessException {
        if (games.replace(game.gameID(), game) == null) {
            throw new DataAccessException("Game does not exist");
        }
    }
}
