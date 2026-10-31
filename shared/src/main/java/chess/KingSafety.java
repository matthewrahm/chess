package chess;

import chess.ChessGame.TeamColor;

/** Detects attacks on a king without changing the board or game history. */
final class KingSafety {
    private KingSafety() {
    }

    static boolean isInCheck(ChessBoard board, TeamColor teamColor) {
        ChessPosition king = findKing(board, teamColor);
        if (king == null) return false;

        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                ChessPosition position = new ChessPosition(row, col);
                ChessPiece piece = board.getPiece(position);
                if (piece == null || piece.getTeamColor() == teamColor) continue;

                for (ChessMove move : piece.pieceMoves(board, position)) {
                    if (move.getEndPosition().equals(king)) return true;
                }
            }
        }
        return false;
    }

    private static ChessPosition findKing(ChessBoard board, TeamColor teamColor) {
        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                ChessPosition position = new ChessPosition(row, col);
                ChessPiece piece = board.getPiece(position);
                if (piece != null && piece.getTeamColor() == teamColor
                        && piece.getPieceType() == ChessPiece.PieceType.KING) {
                    return position;
                }
            }
        }
        return null;
    }

}
