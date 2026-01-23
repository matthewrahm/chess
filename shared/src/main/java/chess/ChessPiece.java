package chess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * Represents a single chess piece
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessPiece {

    private final ChessGame.TeamColor pieceColor;
    private final PieceType type;

    public ChessPiece(ChessGame.TeamColor pieceColor, ChessPiece.PieceType type) {
        this.pieceColor = pieceColor;
        this.type = type;
    }

    /**
     * The various different chess piece options
     */
    public enum PieceType {
        KING,
        QUEEN,
        BISHOP,
        KNIGHT,
        ROOK,
        PAWN
    }

    /**
     * @return Which team this chess piece belongs to
     */
    public ChessGame.TeamColor getTeamColor() {
        return pieceColor;
    }

    /**
     * @return which type of chess piece this piece is
     */
    public PieceType getPieceType() {
        return type;
    }

    /**
     * Calculates all the positions a chess piece can move to
     * Does not take into account moves that are illegal due to leaving the king in
     * danger
     *
     * @return Collection of valid moves
     */
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {
        return switch (type) {
            case KING -> getKingMoves(board, myPosition);
            case QUEEN -> getQueenMoves(board, myPosition);
            case BISHOP -> getBishopMoves(board, myPosition);
            case KNIGHT -> getKnightMoves(board, myPosition);
            case ROOK -> getRookMoves(board, myPosition);
            case PAWN -> new ArrayList<>(); // TODO: implement later
        };
    }

    private Collection<ChessMove> getKingMoves(ChessBoard board, ChessPosition myPosition) {
        Collection<ChessMove> moves = new ArrayList<>();
        int row = myPosition.getRow();
        int col = myPosition.getColumn();

        // King can move one square in any direction
        int[][] directions = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1},  // orthogonal
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1} // diagonal
        };

        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];
            addMoveIfValid(board, myPosition, newRow, newCol, moves);
        }

        return moves;
    }

    private Collection<ChessMove> getRookMoves(ChessBoard board, ChessPosition myPosition) {
        Collection<ChessMove> moves = new ArrayList<>();
        // Rook moves horizontally and vertically
        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        addSlidingMoves(board, myPosition, directions, moves);
        return moves;
    }

    private Collection<ChessMove> getBishopMoves(ChessBoard board, ChessPosition myPosition) {
        Collection<ChessMove> moves = new ArrayList<>();
        // Bishop moves diagonally
        int[][] directions = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
        addSlidingMoves(board, myPosition, directions, moves);
        return moves;
    }

    private Collection<ChessMove> getQueenMoves(ChessBoard board, ChessPosition myPosition) {
        Collection<ChessMove> moves = new ArrayList<>();
        // Queen moves like rook + bishop (all 8 directions)
        int[][] directions = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1},  // orthogonal (rook)
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1} // diagonal (bishop)
        };
        addSlidingMoves(board, myPosition, directions, moves);
        return moves;
    }

    private Collection<ChessMove> getKnightMoves(ChessBoard board, ChessPosition myPosition) {
        Collection<ChessMove> moves = new ArrayList<>();
        int row = myPosition.getRow();
        int col = myPosition.getColumn();

        // Knight moves in L-shape: 2 squares one direction, 1 square perpendicular
        int[][] jumps = {
            {2, 1}, {2, -1}, {-2, 1}, {-2, -1},
            {1, 2}, {1, -2}, {-1, 2}, {-1, -2}
        };

        for (int[] jump : jumps) {
            int newRow = row + jump[0];
            int newCol = col + jump[1];
            addMoveIfValid(board, myPosition, newRow, newCol, moves);
        }

        return moves;
    }

    /**
     * Helper for sliding pieces (rook, bishop, queen) - moves in a direction until blocked
     */
    private void addSlidingMoves(ChessBoard board, ChessPosition from, int[][] directions, Collection<ChessMove> moves) {
        int row = from.getRow();
        int col = from.getColumn();

        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];

            while (newRow >= 1 && newRow <= 8 && newCol >= 1 && newCol <= 8) {
                ChessPosition to = new ChessPosition(newRow, newCol);
                ChessPiece pieceAtTarget = board.getPiece(to);

                if (pieceAtTarget == null) {
                    // Empty square - can move here and continue
                    moves.add(new ChessMove(from, to, null));
                } else if (pieceAtTarget.getTeamColor() != this.pieceColor) {
                    // Enemy piece - can capture but must stop
                    moves.add(new ChessMove(from, to, null));
                    break;
                } else {
                    // Friendly piece - blocked, stop
                    break;
                }

                newRow += dir[0];
                newCol += dir[1];
            }
        }
    }

    /**
     * Helper method to add a move if the target square is valid (on board and not occupied by friendly piece)
     */
    private void addMoveIfValid(ChessBoard board, ChessPosition from, int toRow, int toCol, Collection<ChessMove> moves) {
        if (toRow < 1 || toRow > 8 || toCol < 1 || toCol > 8) {
            return; // Off the board
        }

        ChessPosition to = new ChessPosition(toRow, toCol);
        ChessPiece pieceAtTarget = board.getPiece(to);

        if (pieceAtTarget == null || pieceAtTarget.getTeamColor() != this.pieceColor) {
            // Empty square or enemy piece - valid move
            moves.add(new ChessMove(from, to, null));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChessPiece that = (ChessPiece) o;
        return pieceColor == that.pieceColor && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pieceColor, type);
    }

    @Override
    public String toString() {
        return pieceColor + " " + type;
    }
}
