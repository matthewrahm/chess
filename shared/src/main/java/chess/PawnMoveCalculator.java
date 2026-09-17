package chess;

import chess.ChessPiece.PieceType;
import java.util.ArrayList;
import java.util.Collection;

class PawnMoveCalculator extends PieceMoveCalculator {
    @Override
    public Collection<ChessMove> calculateMoves(ChessBoard board, ChessPosition position,
                                               ChessGame.TeamColor color) {
        Collection<ChessMove> moves = new ArrayList<>();
        int row = position.getRow();
        int col = position.getColumn();
        int direction = (color == ChessGame.TeamColor.WHITE) ? 1 : -1;
        int startRow = (color == ChessGame.TeamColor.WHITE) ? 2 : 7;
        int promotionRow = (color == ChessGame.TeamColor.WHITE) ? 8 : 1;
        int newRow = row + direction;

        if (newRow >= 1 && newRow <= 8) {
            ChessPosition oneAhead = new ChessPosition(newRow, col);
            if (board.getPiece(oneAhead) == null) {
                addPawnMove(position, oneAhead, newRow == promotionRow, moves);

                if (row == startRow) {
                    ChessPosition twoAhead = new ChessPosition(row + 2 * direction, col);
                    if (board.getPiece(twoAhead) == null) {
                        moves.add(new ChessMove(position, twoAhead, null));
                    }
                }
            }
        }

        int[] captureCols = {col - 1, col + 1};
        for (int captureCol : captureCols) {
            if (captureCol >= 1 && captureCol <= 8 && newRow >= 1 && newRow <= 8) {
                ChessPosition capturePos = new ChessPosition(newRow, captureCol);
                ChessPiece target = board.getPiece(capturePos);
                if (target != null && target.getTeamColor() != color) {
                    addPawnMove(position, capturePos, newRow == promotionRow, moves);
                }
            }
        }
        return moves;
    }

    private void addPawnMove(ChessPosition from, ChessPosition to, boolean isPromotion,
                             Collection<ChessMove> moves) {
        if (isPromotion) {
            moves.add(new ChessMove(from, to, PieceType.QUEEN));
            moves.add(new ChessMove(from, to, PieceType.ROOK));
            moves.add(new ChessMove(from, to, PieceType.BISHOP));
            moves.add(new ChessMove(from, to, PieceType.KNIGHT));
        } else {
            moves.add(new ChessMove(from, to, null));
        }
    }
}
