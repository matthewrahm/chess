package dataaccess.sql;

import chess.ChessGame;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import dataaccess.DataAccessException;

/** Keeps the complete ChessGame, including move history flags, in one JSON value. */
final class GameJson {
    private static final Gson GSON = new Gson();

    private GameJson() {
    }

    static String encode(ChessGame game) throws DataAccessException {
        requireGame(game);
        return GSON.toJson(game);
    }

    static ChessGame decode(String json) throws DataAccessException {
        try {
            ChessGame game = GSON.fromJson(json, ChessGame.class);
            requireGame(game);
            return game;
        } catch (JsonParseException exception) {
            throw new DataAccessException("Unable to read stored game JSON", exception);
        }
    }

    private static void requireGame(ChessGame game) throws DataAccessException {
        if (game == null || game.getBoard() == null || game.getTeamTurn() == null) {
            throw new DataAccessException("A stored game must contain a board and team turn");
        }
    }
}
