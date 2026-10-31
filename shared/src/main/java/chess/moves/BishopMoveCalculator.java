package chess.moves;

import chess.*;

import java.util.Collection;

public class BishopMoveCalculator extends PieceMoveCalculator {
    @Override
    public Collection<ChessMove> calculateMoves(ChessBoard board, ChessPosition position,
                                               ChessGame.TeamColor color) {
        int[][] directions = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
        return SlidingMoves.calculateMoves(board, position, color, directions);
    }
}
