package chess;

import java.util.Collection;

class RookMoveCalculator extends PieceMoveCalculator {
    @Override
    public Collection<ChessMove> calculateMoves(ChessBoard board, ChessPosition position,
                                               ChessGame.TeamColor color) {
        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        return SlidingMoves.calculateMoves(board, position, color, directions);
    }
}
