package service;

import chess.ChessGame;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.AuthData;
import model.GameData;
import request.CreateGameRequest;
import request.JoinGameRequest;
import request.ListGamesRequest;
import result.CreateGameResult;
import result.GameSummary;
import result.ListGamesResult;

import static service.ServiceException.Code.*;

public class GameService {
    private final DataAccess dataAccess;

    public GameService(DataAccess dataAccess) {
        this.dataAccess = dataAccess;
    }

    public ListGamesResult listGames(ListGamesRequest request) throws ServiceException, DataAccessException {
        requireAuthorization(request == null ? null : request.authToken());
        var summaries = dataAccess.listGames().stream()
                .map(game -> new GameSummary(game.gameID(), game.whiteUsername(), game.blackUsername(), game.gameName()))
                .toList();
        return new ListGamesResult(summaries);
    }

    public CreateGameResult createGame(CreateGameRequest request) throws ServiceException, DataAccessException {
        if (request == null) {
            throw new ServiceException(BAD_REQUEST);
        }
        requireAuthorization(request.authToken());
        if (request.gameName() == null || request.gameName().isBlank()) {
            throw new ServiceException(BAD_REQUEST);
        }
        int gameID = dataAccess.createGame(request.gameName(), new ChessGame());
        return new CreateGameResult(gameID);
    }

    public void joinGame(JoinGameRequest request) throws ServiceException, DataAccessException {
        if (request == null) {
            throw new ServiceException(BAD_REQUEST);
        }
        // Keep the seat check and update together across services sharing this store.
        synchronized (dataAccess) {
            AuthData auth = requireAuthorization(request.authToken());
            if (request.gameID() == null
                    || !("WHITE".equals(request.playerColor()) || "BLACK".equals(request.playerColor()))) {
                throw new ServiceException(BAD_REQUEST);
            }
            GameData game = dataAccess.getGame(request.gameID());
            if (game == null) {
                throw new ServiceException(BAD_REQUEST);
            }
            boolean joiningWhite = "WHITE".equals(request.playerColor());
            String occupiedBy = joiningWhite ? game.whiteUsername() : game.blackUsername();
            if (occupiedBy != null) {
                throw new ServiceException(ALREADY_TAKEN);
            }
            var joined = new GameData(game.gameID(),
                    joiningWhite ? auth.username() : game.whiteUsername(),
                    joiningWhite ? game.blackUsername() : auth.username(),
                    game.gameName(), game.game());
            dataAccess.updateGame(joined);
        }
    }

    private AuthData requireAuthorization(String authToken) throws ServiceException, DataAccessException {
        if (authToken == null || authToken.isBlank()) {
            throw new ServiceException(UNAUTHORIZED);
        }
        AuthData auth = dataAccess.getAuth(authToken);
        if (auth == null) {
            throw new ServiceException(UNAUTHORIZED);
        }
        return auth;
    }
}
