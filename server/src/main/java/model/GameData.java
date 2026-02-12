package model;

import chess.ChessGame;

/**
 * Represents a chess game with its players and state.
 *
 * @param gameID unique identifier for the game
 * @param whiteUsername username of the white player, or null if open
 * @param blackUsername username of the black player, or null if open
 * @param gameName display name for the game
 * @param game the chess game state
 */
public record GameData(int gameID, String whiteUsername, String blackUsername, String gameName, ChessGame game) {
}
