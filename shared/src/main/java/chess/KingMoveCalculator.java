package chess;

import java.util.ArrayList;
import java.util.Collection;

class KingMoveCalculator extends PieceMoveCalculator {
    @Override
    public Collection<ChessMove> calculateMoves(ChessBoard board, ChessPosition position,
                                               ChessGame.TeamColor color) {
        Collection<ChessMove> moves = new ArrayList<>();
        int[][] directions = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1},
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
        };

        for (int[] direction : directions) {
            int row = position.getRow() + direction[0];
            int col = position.getColumn() + direction[1];
            addMoveIfValid(board, position, row, col, color, moves);
        }
        return moves;
    }
}
