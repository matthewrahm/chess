package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.GameData;
import websocket.commands.UserGameCommand;

/** Reads authorized game snapshots; transport and subscriptions belong elsewhere. */
public class GameplayService {
    private final DataAccess dataAccess;

    public GameplayService(DataAccess dataAccess) {
        this.dataAccess = dataAccess;
    }

    public GameAccess authorize(UserGameCommand command) throws GameplayException, DataAccessException {
        synchronized (dataAccess) {
            if (command == null || command.getAuthToken() == null || command.getAuthToken().isBlank()) {
                throw new GameplayException("Unauthorized");
            }
            var auth = dataAccess.getAuth(command.getAuthToken());
            if (auth == null) {
                throw new GameplayException("Unauthorized");
            }
            if (command.getGameID() == null || command.getGameID() <= 0) {
                throw new GameplayException("Invalid game ID");
            }
            var game = dataAccess.getGame(command.getGameID());
            if (game == null) {
                throw new GameplayException("Game does not exist");
            }
            return new GameAccess(auth.username(), game);
        }
    }

    public record GameAccess(String username, GameData game) { }
}
