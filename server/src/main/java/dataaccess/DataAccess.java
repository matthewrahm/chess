package dataaccess;

import chess.ChessGame;
import model.AuthData;
import model.GameData;
import model.UserData;

import java.util.Collection;

/** Storage operations shared by the memory and SQL implementations. */
public interface DataAccess {
    // Lookups return null when their key is absent, including a missing token.
    UserData getUser(String username) throws DataAccessException;

    void createUser(UserData user) throws DataAccessException;

    AuthData getAuth(String authToken) throws DataAccessException;

    void createAuth(AuthData auth) throws DataAccessException;

    void deleteAuth(String authToken) throws DataAccessException;

    void clear() throws DataAccessException;

    // Returns detached game snapshots in a separate collection, never null. Ordering is unspecified.
    Collection<GameData> listGames() throws DataAccessException;

    // Stores a snapshot of the supplied game with both player seats empty.
    int createGame(String gameName, ChessGame game) throws DataAccessException;

    // Returns a detached snapshot, or null if the ID does not exist.
    GameData getGame(int gameID) throws DataAccessException;

    // Replaces an existing game with a snapshot; an unknown ID is a storage error.
    void updateGame(GameData game) throws DataAccessException;
}
