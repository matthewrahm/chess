package chess;

import java.util.Arrays;

/**
 * A chessboard that can hold and rearrange chess pieces.
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessBoard {

    private final ChessPiece[][] squares = new ChessPiece[8][8];

    public ChessBoard() {
    }

    /**
     * Adds a chess piece to the chessboard
     *
     * @param position where to add the piece to
     * @param piece    the piece to add
     */
    public void addPiece(ChessPosition position, ChessPiece piece) {
        squares[position.getRow() - 1][position.getColumn() - 1] = piece;
    }

    /**
     * Gets a chess piece on the chessboard
     *
     * @param position The position to get the piece from
     * @return Either the piece at the position, or null if no piece is at that
     * position
     */
    public ChessPiece getPiece(ChessPosition position) {
        return squares[position.getRow() - 1][position.getColumn() - 1];
    }

    /**
     * Sets the board to the default starting board
     * (How the game of chess normally starts)
     */
    public void resetBoard() {
        for (ChessPiece[] row : squares) {
            Arrays.fill(row, null);
        }

        // Back row piece order: Rook, Knight, Bishop, Queen, King, Bishop, Knight, Rook
        ChessPiece.PieceType[] backRow = {
            ChessPiece.PieceType.ROOK,
            ChessPiece.PieceType.KNIGHT,
            ChessPiece.PieceType.BISHOP,
            ChessPiece.PieceType.QUEEN,
            ChessPiece.PieceType.KING,
            ChessPiece.PieceType.BISHOP,
            ChessPiece.PieceType.KNIGHT,
            ChessPiece.PieceType.ROOK
        };

        placeStartingPieces(ChessGame.TeamColor.WHITE, backRow);
        placeStartingPieces(ChessGame.TeamColor.BLACK, backRow);
    }

    private void placeStartingPieces(ChessGame.TeamColor color, ChessPiece.PieceType[] backRow) {
        int pieceRow = color == ChessGame.TeamColor.WHITE ? 1 : 8;
        int pawnRow = color == ChessGame.TeamColor.WHITE ? 2 : 7;
        for (int col = 1; col <= 8; col++) {
            addPiece(new ChessPosition(pieceRow, col), new ChessPiece(color, backRow[col - 1]));
            addPiece(new ChessPosition(pawnRow, col), new ChessPiece(color, ChessPiece.PieceType.PAWN));
        }
    }

    public ChessBoard copy() {
        ChessBoard clone = new ChessBoard();
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                clone.squares[row][col] = this.squares[row][col];
            }
        }
        return clone;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChessBoard that = (ChessBoard) o;
        return Arrays.deepEquals(squares, that.squares);
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(squares);
    }
}
