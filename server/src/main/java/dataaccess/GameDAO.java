package dataaccess;

import model.GameData;

import java.util.Collection;

public interface GameDAO {
    void clear() throws DataAccessException;
    void createGame(GameData game) throws DataAccessException;
    GameData getGame(int gameID) throws DataAccessException;
    Collection<GameData> listGames() throws DataAccessException;
    void updateGame(GameData game) throws DataAccessException;
}
