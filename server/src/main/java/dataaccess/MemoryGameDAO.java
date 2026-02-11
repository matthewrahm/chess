package dataaccess;

import model.GameData;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class MemoryGameDAO implements GameDAO {
    private final Map<Integer, GameData> games = new HashMap<>();
    private int nextId = 1;

    @Override
    public void clear() {
        games.clear();
        nextId = 1;
    }

    @Override
    public void createGame(GameData game) {
        games.put(game.gameID(), game);
    }

    public int generateId() {
        return nextId++;
    }

    @Override
    public GameData getGame(int gameID) {
        return games.get(gameID);
    }

    @Override
    public Collection<GameData> listGames() {
        return games.values();
    }

    @Override
    public void updateGame(GameData game) {
        games.put(game.gameID(), game);
    }
}
