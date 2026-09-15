package chess;

import java.util.Collection;

abstract class PieceMoveCalculator {
    public abstract Collection<ChessMove> calculateMoves(ChessBoard board, ChessPosition position,
                                                        ChessGame.TeamColor color);

    protected void addMoveIfValid(ChessBoard board, ChessPosition from, int row, int col,
                                  ChessGame.TeamColor color, Collection<ChessMove> moves) {
        if (row < 1 || row > 8 || col < 1 || col > 8) {
            return;
        }

        ChessPosition to = new ChessPosition(row, col);
        ChessPiece target = board.getPiece(to);
        if (target == null || target.getTeamColor() != color) {
            moves.add(new ChessMove(from, to, null));
        }
    }
}
