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
            case KING -> new KingMoveCalculator().calculateMoves(board, myPosition, pieceColor);
            case QUEEN -> getQueenMoves(board, myPosition);
            case BISHOP -> getBishopMoves(board, myPosition);
            case KNIGHT -> new KnightMoveCalculator().calculateMoves(board, myPosition, pieceColor);
            case ROOK -> getRookMoves(board, myPosition);
            case PAWN -> getPawnMoves(board, myPosition);
        };
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

    private Collection<ChessMove> getPawnMoves(ChessBoard board, ChessPosition myPosition) {
        Collection<ChessMove> moves = new ArrayList<>();
        int row = myPosition.getRow();
        int col = myPosition.getColumn();

        // Direction depends on color: white moves up (+1), black moves down (-1)
        int direction = (pieceColor == ChessGame.TeamColor.WHITE) ? 1 : -1;
        int startRow = (pieceColor == ChessGame.TeamColor.WHITE) ? 2 : 7;
        int promotionRow = (pieceColor == ChessGame.TeamColor.WHITE) ? 8 : 1;

        int newRow = row + direction;

        // Forward move (1 square)
        if (newRow >= 1 && newRow <= 8) {
            ChessPosition oneAhead = new ChessPosition(newRow, col);
            if (board.getPiece(oneAhead) == null) {
                addPawnMove(myPosition, oneAhead, newRow == promotionRow, moves);

                // Double move from starting position
                if (row == startRow) {
                    int twoAheadRow = row + (2 * direction);
                    ChessPosition twoAhead = new ChessPosition(twoAheadRow, col);
                    if (board.getPiece(twoAhead) == null) {
                        moves.add(new ChessMove(myPosition, twoAhead, null));
                    }
                }
            }
        }

        // Diagonal captures
        int[] captureCols = {col - 1, col + 1};
        for (int captureCol : captureCols) {
            if (captureCol >= 1 && captureCol <= 8 && newRow >= 1 && newRow <= 8) {
                ChessPosition capturePos = new ChessPosition(newRow, captureCol);
                ChessPiece pieceAtTarget = board.getPiece(capturePos);
                if (pieceAtTarget != null && pieceAtTarget.getTeamColor() != this.pieceColor) {
                    addPawnMove(myPosition, capturePos, newRow == promotionRow, moves);
                }
            }
        }

        return moves;
    }

    /**
     * Helper to add pawn moves, handling promotion
     */
    private void addPawnMove(ChessPosition from, ChessPosition to, boolean isPromotion, Collection<ChessMove> moves) {
        if (isPromotion) {
            // Must promote to one of these pieces
            moves.add(new ChessMove(from, to, PieceType.QUEEN));
            moves.add(new ChessMove(from, to, PieceType.ROOK));
            moves.add(new ChessMove(from, to, PieceType.BISHOP));
            moves.add(new ChessMove(from, to, PieceType.KNIGHT));
        } else {
            moves.add(new ChessMove(from, to, null));
        }
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
}
