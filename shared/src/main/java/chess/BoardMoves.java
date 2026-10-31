package chess;

/** Applies an already validated move, either to the live board or a copy. */
final class BoardMoves {
    private BoardMoves() {
    }

    static ChessBoard simulate(ChessBoard board, ChessMove move) {
        ChessBoard copy = board.copy();
        apply(copy, move);
        return copy;
    }

    static void apply(ChessBoard board, ChessMove move) {
        ChessPiece piece = board.getPiece(move.getStartPosition());
        board.addPiece(move.getStartPosition(), null);
        removeEnPassantCapture(board, move, piece);
        moveCastlingRook(board, move, piece);
        if (move.getPromotionPiece() != null) {
            piece = new ChessPiece(piece.getTeamColor(), move.getPromotionPiece());
        }
        board.addPiece(move.getEndPosition(), piece);
    }

    private static void removeEnPassantCapture(ChessBoard board, ChessMove move, ChessPiece piece) {
        if (piece.getPieceType() == ChessPiece.PieceType.PAWN
                && move.getStartPosition().getColumn() != move.getEndPosition().getColumn()
                && board.getPiece(move.getEndPosition()) == null) {
            board.addPiece(new ChessPosition(move.getStartPosition().getRow(),
                    move.getEndPosition().getColumn()), null);
        }
    }

    private static void moveCastlingRook(ChessBoard board, ChessMove move, ChessPiece piece) {
        if (piece.getPieceType() == ChessPiece.PieceType.KING
                && Math.abs(move.getEndPosition().getColumn() - move.getStartPosition().getColumn()) == 2) {
            int row = move.getStartPosition().getRow();
            boolean kingside = move.getEndPosition().getColumn() == 7;
            ChessPosition rookStart = new ChessPosition(row, kingside ? 8 : 1);
            ChessPosition rookEnd = new ChessPosition(row, kingside ? 6 : 4);
            board.addPiece(rookEnd, board.getPiece(rookStart));
            board.addPiece(rookStart, null);
        }
    }
}
