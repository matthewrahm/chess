package service;

import chess.ChessGame;
import dataaccess.DataAccessException;
import dataaccess.MemoryGameDAO;
import model.AuthData;
import model.GameData;

import java.util.Collection;

/** Service handling game creation, listing, and join operations. */
public class GameService {
    private final MemoryGameDAO gameDAO;
    private final AuthHelper authHelper;

    public GameService(MemoryGameDAO gameDAO, AuthHelper authHelper) {
        this.gameDAO = gameDAO;
        this.authHelper = authHelper;
    }

    public Collection<GameData> listGames(String authToken) throws ServiceException, DataAccessException {
        authHelper.validateAuth(authToken);
        return gameDAO.listGames();
    }

    public int createGame(String authToken, String gameName) throws ServiceException, DataAccessException {
        authHelper.validateAuth(authToken);

        if (gameName == null || gameName.isEmpty()) {
            throw new ServiceException(400, "Error: bad request");
        }

        int gameID = gameDAO.generateId();
        GameData game = new GameData(gameID, null, null, gameName, new ChessGame());
        gameDAO.createGame(game);
        return gameID;
    }

    public void joinGame(String authToken, String playerColor, Integer gameID) throws ServiceException, DataAccessException {
        AuthData auth = authHelper.validateAuth(authToken);

        if (playerColor == null || playerColor.isEmpty() ||
                (!playerColor.equals("WHITE") && !playerColor.equals("BLACK"))) {
            throw new ServiceException(400, "Error: bad request");
        }

        if (gameID == null) {
            throw new ServiceException(400, "Error: bad request");
        }

        GameData game = gameDAO.getGame(gameID);
        if (game == null) {
            throw new ServiceException(400, "Error: bad request");
        }

        String username = auth.username();

        if (playerColor.equals("WHITE")) {
            if (game.whiteUsername() != null) {
                throw new ServiceException(403, "Error: already taken");
            }
            gameDAO.updateGame(new GameData(game.gameID(), username, game.blackUsername(), game.gameName(), game.game()));
        } else {
            if (game.blackUsername() != null) {
                throw new ServiceException(403, "Error: already taken");
            }
            gameDAO.updateGame(new GameData(game.gameID(), game.whiteUsername(), username, game.gameName(), game.game()));
        }
    }
}
