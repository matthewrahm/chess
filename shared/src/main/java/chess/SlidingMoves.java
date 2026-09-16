package chess;

import java.util.ArrayList;
import java.util.Collection;

class SlidingMoves {
    static Collection<ChessMove> calculateMoves(ChessBoard board, ChessPosition position,
                                                 ChessGame.TeamColor color, int[][] directions) {
        Collection<ChessMove> moves = new ArrayList<>();
        for (int[] direction : directions) {
            int row = position.getRow() + direction[0];
            int col = position.getColumn() + direction[1];

            while (row >= 1 && row <= 8 && col >= 1 && col <= 8) {
                ChessPosition to = new ChessPosition(row, col);
                ChessPiece target = board.getPiece(to);

                if (target == null || target.getTeamColor() != color) {
                    moves.add(new ChessMove(position, to, null));
                }
                if (target != null) {
                    break;
                }

                row += direction[0];
                col += direction[1];
            }
        }
        return moves;
    }
}
