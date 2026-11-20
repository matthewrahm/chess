package service;

import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPiece;
import chess.ChessPosition;
import chess.InvalidMoveException;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.GameData;
import websocket.commands.UserGameCommand;
import websocket.commands.MakeMoveCommand;

import java.util.Locale;

/** Authorizes and persists gameplay under the store monitor shared with HTTP joins. */
public class GameplayService {
    private final DataAccess dataAccess;

    public GameplayService(DataAccess dataAccess) {
        this.dataAccess = dataAccess;
    }

    public MoveResult makeMove(MakeMoveCommand command, String subscribedUsername)
            throws GameplayException, DataAccessException {
        synchronized (dataAccess) {
            var access = authorize(command);
            if (!access.username().equals(subscribedUsername)) {
                throw new GameplayException("Connect as this user first");
            }
            var game = access.game().game();
            if (game.isGameOver()) {
                throw new GameplayException("The game is over");
            }
            var move = command.getMove();
            var piece = requireOwnedPiece(access, move);
            try {
                game.makeMove(move);
            } catch (InvalidMoveException exception) {
                throw new GameplayException(exception.getMessage());
            }
            String statusNotice = updateStatus(access.game());
            dataAccess.updateGame(access.game());
            return new MoveResult(access.game(), moveNotice(access.username(), piece, move), statusNotice);
        }
    }

    private ChessPiece requireOwnedPiece(GameAccess access, ChessMove move) throws GameplayException {
        if (move == null || !onBoard(move.getStartPosition()) || !onBoard(move.getEndPosition())) {
            throw new GameplayException("Move must use positions on the board");
        }
        var piece = access.game().game().getBoard().getPiece(move.getStartPosition());
        if (piece == null) {
            throw new GameplayException("No piece at the starting position");
        }
        String owner = piece.getTeamColor() == ChessGame.TeamColor.WHITE
                ? access.game().whiteUsername() : access.game().blackUsername();
        if (!access.username().equals(owner)) {
            throw new GameplayException("You may only move your own pieces as a player");
        }
        return piece;
    }

    private boolean onBoard(ChessPosition position) {
        return position != null && position.getRow() >= 1 && position.getRow() <= 8
                && position.getColumn() >= 1 && position.getColumn() <= 8;
    }

    private String moveNotice(String username, ChessPiece piece, ChessMove move) {
        String notice = username + " moved " + piece.getPieceType().name().toLowerCase(Locale.ROOT)
                + " from " + square(move.getStartPosition()) + " to " + square(move.getEndPosition());
        if (move.getPromotionPiece() != null) {
            notice += " and promoted to " + move.getPromotionPiece().name().toLowerCase(Locale.ROOT);
        }
        return notice + ".";
    }

    private String square(ChessPosition position) {
        return "" + (char) ('a' + position.getColumn() - 1) + position.getRow();
    }

    private String updateStatus(GameData data) {
        var game = data.game();
        var next = game.getTeamTurn();
        String username = next == ChessGame.TeamColor.WHITE ? data.whiteUsername() : data.blackUsername();
        String player = next.name().toLowerCase(Locale.ROOT);
        if (username != null) {
            player = username + " (" + player + ")";
        }
        if (game.isInCheckmate(next)) {
            game.endGame();
            return player + " is in checkmate. Game over.";
        }
        if (game.isInStalemate(next)) {
            game.endGame();
            return player + " is in stalemate. Game over: draw.";
        }
        return game.isInCheck(next) ? player + " is in check." : null;
    }

    public record MoveResult(GameData game, String moveNotice, String statusNotice) { }

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

    public record GameAccess(String username, GameData game) {
        public String connectionNotice() {
            if (username.equals(game.whiteUsername())) {
                return username + " connected as white.";
            }
            if (username.equals(game.blackUsername())) {
                return username + " connected as black.";
            }
            return username + " connected as an observer.";
        }
    }
}
