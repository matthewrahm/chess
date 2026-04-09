package websocket;

import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPosition;
import chess.InvalidMoveException;
import com.google.gson.Gson;
import dataaccess.DataAccessException;
import dataaccess.GameDAO;
import model.AuthData;
import model.GameData;
import service.AuthHelper;
import service.ServiceException;
import websocket.commands.MakeMoveCommand;
import websocket.commands.UserGameCommand;
import websocket.messages.ErrorMessage;
import websocket.messages.LoadGameMessage;
import websocket.messages.NotificationMessage;

import org.eclipse.jetty.websocket.api.Session;

import java.io.IOException;

public class WebSocketHandler {

    private final ConnectionManager connections = new ConnectionManager();
    private final AuthHelper authHelper;
    private final GameDAO gameDAO;
    private final Gson gson = new Gson();

    public WebSocketHandler(AuthHelper authHelper, GameDAO gameDAO) {
        this.authHelper = authHelper;
        this.gameDAO = gameDAO;
    }

    public void handleMessage(Session session, String message) {
        try {
            UserGameCommand command = gson.fromJson(message, UserGameCommand.class);

            AuthData auth;
            try {
                auth = authHelper.validateAuth(command.getAuthToken());
            } catch (ServiceException | DataAccessException e) {
                sendError(session, "Error: unauthorized");
                return;
            }

            String username = auth.username();

            switch (command.getCommandType()) {
                case CONNECT -> handleConnect(session, username, command);
                case MAKE_MOVE -> {
                    MakeMoveCommand moveCmd = gson.fromJson(message, MakeMoveCommand.class);
                    handleMakeMove(session, username, moveCmd);
                }
                case RESIGN -> handleResign(session, username, command);
                case LEAVE -> handleLeave(session, username, command);
            }
        } catch (Exception e) {
            sendError(session, "Error: " + e.getMessage());
        }
    }

    public void handleClose(Session session, int statusCode, String reason) {
        connections.removeBySession(session);
    }

    private void handleConnect(Session session, String username, UserGameCommand command)
            throws IOException, DataAccessException {
        int gameID = command.getGameID();
        GameData gameData = gameDAO.getGame(gameID);
        if (gameData == null) {
            sendError(session, "Error: game not found");
            return;
        }

        connections.add(gameID, username, session);

        var loadGame = new LoadGameMessage(gameData);
        session.getRemote().sendString(gson.toJson(loadGame));

        String role;
        if (username.equals(gameData.whiteUsername())) {
            role = "white player";
        } else if (username.equals(gameData.blackUsername())) {
            role = "black player";
        } else {
            role = "an observer";
        }

        var notification = new NotificationMessage(username + " joined as " + role);
        connections.broadcast(gameID, username, gson.toJson(notification));
    }

    private void handleMakeMove(Session session, String username, MakeMoveCommand command)
            throws IOException, DataAccessException {
        int gameID = command.getGameID();
        GameData gameData = gameDAO.getGame(gameID);

        if (gameData == null) {
            sendError(session, "Error: game not found");
            return;
        }

        ChessGame game = gameData.game();

        if (game.isOver()) {
            sendError(session, "Error: game is already over");
            return;
        }

        ChessGame.TeamColor playerColor = getPlayerColor(username, gameData);
        if (playerColor == null) {
            sendError(session, "Error: observers cannot make moves");
            return;
        }

        if (game.getTeamTurn() != playerColor) {
            sendError(session, "Error: it is not your turn");
            return;
        }

        ChessMove move = command.getMove();
        if (move == null) {
            sendError(session, "Error: missing move");
            return;
        }

        var piece = game.getBoard().getPiece(move.getStartPosition());
        if (piece == null || piece.getTeamColor() != playerColor) {
            sendError(session, "Error: you can only move your own pieces");
            return;
        }

        try {
            game.makeMove(move);
        } catch (InvalidMoveException e) {
            sendError(session, "Error: invalid move");
            return;
        }

        var updatedGameData = new GameData(
                gameData.gameID(), gameData.whiteUsername(), gameData.blackUsername(),
                gameData.gameName(), game);
        gameDAO.updateGame(updatedGameData);

        var loadGame = new LoadGameMessage(updatedGameData);
        String loadGameJson = gson.toJson(loadGame);
        connections.broadcast(gameID, null, loadGameJson);

        String moveDesc = describeMove(move);
        var moveNotification = new NotificationMessage(username + " moved " + moveDesc);
        connections.broadcast(gameID, username, gson.toJson(moveNotification));

        ChessGame.TeamColor opponent = (playerColor == ChessGame.TeamColor.WHITE)
                ? ChessGame.TeamColor.BLACK : ChessGame.TeamColor.WHITE;

        if (game.isInCheckmate(opponent)) {
            game.setOver(true);
            gameDAO.updateGame(new GameData(
                    gameData.gameID(), gameData.whiteUsername(), gameData.blackUsername(),
                    gameData.gameName(), game));
            String opponentName = getPlayerName(opponent, gameData);
            var notification = new NotificationMessage(opponentName + " is in checkmate");
            connections.broadcast(gameID, null, gson.toJson(notification));
        } else if (game.isInStalemate(opponent)) {
            game.setOver(true);
            gameDAO.updateGame(new GameData(
                    gameData.gameID(), gameData.whiteUsername(), gameData.blackUsername(),
                    gameData.gameName(), game));
            var notification = new NotificationMessage("Game ended in stalemate");
            connections.broadcast(gameID, null, gson.toJson(notification));
        } else if (game.isInCheck(opponent)) {
            String opponentName = getPlayerName(opponent, gameData);
            var notification = new NotificationMessage(opponentName + " is in check");
            connections.broadcast(gameID, null, gson.toJson(notification));
        }
    }

    private void handleResign(Session session, String username, UserGameCommand command)
            throws IOException, DataAccessException {
        int gameID = command.getGameID();
        GameData gameData = gameDAO.getGame(gameID);

        if (gameData == null) {
            sendError(session, "Error: game not found");
            return;
        }

        if (getPlayerColor(username, gameData) == null) {
            sendError(session, "Error: observers cannot resign");
            return;
        }

        if (gameData.game().isOver()) {
            sendError(session, "Error: game is already over");
            return;
        }

        gameData.game().setOver(true);
        gameDAO.updateGame(gameData);

        var notification = new NotificationMessage(username + " resigned");
        connections.broadcast(gameID, null, gson.toJson(notification));
    }

    private void handleLeave(Session session, String username, UserGameCommand command)
            throws IOException, DataAccessException {
        int gameID = command.getGameID();
        GameData gameData = gameDAO.getGame(gameID);

        if (gameData != null) {
            if (username.equals(gameData.whiteUsername())) {
                gameDAO.updateGame(new GameData(
                        gameData.gameID(), null, gameData.blackUsername(),
                        gameData.gameName(), gameData.game()));
            } else if (username.equals(gameData.blackUsername())) {
                gameDAO.updateGame(new GameData(
                        gameData.gameID(), gameData.whiteUsername(), null,
                        gameData.gameName(), gameData.game()));
            }
        }

        connections.remove(gameID, username);

        var notification = new NotificationMessage(username + " left the game");
        connections.broadcast(gameID, null, gson.toJson(notification));
    }

    private ChessGame.TeamColor getPlayerColor(String username, GameData game) {
        if (username.equals(game.whiteUsername())) {
            return ChessGame.TeamColor.WHITE;
        }
        if (username.equals(game.blackUsername())) {
            return ChessGame.TeamColor.BLACK;
        }
        return null;
    }

    private String getPlayerName(ChessGame.TeamColor color, GameData game) {
        return (color == ChessGame.TeamColor.WHITE) ? game.whiteUsername() : game.blackUsername();
    }

    private String describeMove(ChessMove move) {
        return positionToString(move.getStartPosition()) + " to " + positionToString(move.getEndPosition());
    }

    private String positionToString(ChessPosition pos) {
        char col = (char) ('a' + pos.getColumn() - 1);
        return "" + col + pos.getRow();
    }

    private void sendError(Session session, String errorMessage) {
        try {
            var error = new ErrorMessage(errorMessage);
            session.getRemote().sendString(gson.toJson(error));
        } catch (IOException e) {
            // Connection likely closed, nothing to do
        }
    }
}
