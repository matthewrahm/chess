package chess;

import java.util.ArrayList;
import java.util.Collection;

class KnightMoveCalculator extends PieceMoveCalculator {
    @Override
    public Collection<ChessMove> calculateMoves(ChessBoard board, ChessPosition position,
                                               ChessGame.TeamColor color) {
        Collection<ChessMove> moves = new ArrayList<>();
        int[][] jumps = {
            {2, 1}, {2, -1}, {-2, 1}, {-2, -1},
            {1, 2}, {1, -2}, {-1, 2}, {-1, -2}
        };

        for (int[] jump : jumps) {
            int row = position.getRow() + jump[0];
            int col = position.getColumn() + jump[1];
            addMoveIfValid(board, position, row, col, color, moves);
        }
        return moves;
    }
}
