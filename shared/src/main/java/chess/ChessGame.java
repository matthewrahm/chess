package chess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

/**
 * For a class that can manage a chess game, making moves on a board
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessGame {

    private ChessBoard board;
    private TeamColor teamTurn;
    private boolean[][] castlingRights = {{true, true}, {true, true}};

    public ChessGame() {
        board = new ChessBoard();
        board.resetBoard();
        teamTurn = TeamColor.WHITE;
    }

    /**
     * @return Which team's turn it is
     */
    public TeamColor getTeamTurn() {
        return teamTurn;
    }

    /**
     * Set's which teams turn it is
     *
     * @param team the team whose turn it is
     */
    public void setTeamTurn(TeamColor team) {
        teamTurn = team;
    }

    /**
     * Enum identifying the 2 possible teams in a chess game
     */
    public enum TeamColor {
        WHITE,
        BLACK
    }

    /**
     * Gets a valid moves for a piece at the given location
     *
     * @param startPosition the piece to get valid moves for
     * @return Set of valid moves for requested piece, or null if no piece at
     * startPosition
     */
    public Collection<ChessMove> validMoves(ChessPosition startPosition) {
        ChessPiece piece = board.getPiece(startPosition);
        if (piece == null) return null;

        Collection<ChessMove> candidates = new ArrayList<>(piece.pieceMoves(board, startPosition));
        if (piece.getPieceType() == ChessPiece.PieceType.KING) {
            addCastlingMoves(startPosition, piece.getTeamColor(), candidates);
        }
        Collection<ChessMove> moves = new ArrayList<>();
        for (ChessMove move : candidates) {
            ChessBoard nextBoard = simulateMove(move);
            if (!isInCheck(nextBoard, piece.getTeamColor())) {
                moves.add(move);
            }
        }
        return moves;
    }

    /**
     * Makes a move in a chess game
     *
     * @param move chess move to perform
     * @throws InvalidMoveException if move is invalid
     */
    public void makeMove(ChessMove move) throws InvalidMoveException {
        if (move == null || !isOnBoard(move.getStartPosition()) || !isOnBoard(move.getEndPosition())) {
            throw new InvalidMoveException("Move must use positions on the board");
        }
        ChessPiece piece = board.getPiece(move.getStartPosition());
        if (piece == null) {
            throw new InvalidMoveException("No piece at the starting position");
        }
        if (piece.getTeamColor() != teamTurn) {
            throw new InvalidMoveException("It is not this team's turn");
        }
        if (!validMoves(move.getStartPosition()).contains(move)) {
            throw new InvalidMoveException("Move is not legal");
        }
        updateCastlingRights(move, piece);
        applyMove(board, move);
        teamTurn = teamTurn == TeamColor.WHITE ? TeamColor.BLACK : TeamColor.WHITE;
    }

    private boolean isOnBoard(ChessPosition position) {
        return position != null && position.getRow() >= 1 && position.getRow() <= 8
                && position.getColumn() >= 1 && position.getColumn() <= 8;
    }

    /**
     * Determines if the given team is in check
     *
     * @param teamColor which team to check for check
     * @return True if the specified team is in check
     */
    public boolean isInCheck(TeamColor teamColor) {
        return isInCheck(board, teamColor);
    }

    private boolean isInCheck(ChessBoard board, TeamColor teamColor) {
        ChessPosition king = findKing(board, teamColor);
        if (king == null) return false;

        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                ChessPosition position = new ChessPosition(row, col);
                ChessPiece piece = board.getPiece(position);
                if (piece == null || piece.getTeamColor() == teamColor) continue;

                for (ChessMove move : piece.pieceMoves(board, position)) {
                    if (move.getEndPosition().equals(king)) return true;
                }
            }
        }
        return false;
    }

    /**
     * Determines if the given team is in checkmate
     *
     * @param teamColor which team to check for checkmate
     * @return True if the specified team is in checkmate
     */
    public boolean isInCheckmate(TeamColor teamColor) {
        return isInCheck(teamColor) && !hasLegalMove(teamColor);
    }

    private boolean hasLegalMove(TeamColor teamColor) {
        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                ChessPosition position = new ChessPosition(row, col);
                ChessPiece piece = board.getPiece(position);
                if (piece != null && piece.getTeamColor() == teamColor
                        && !validMoves(position).isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Determines if the given team is in stalemate, which here is defined as having
     * no valid moves while not in check.
     *
     * @param teamColor which team to check for stalemate
     * @return True if the specified team is in stalemate, otherwise false
     */
    public boolean isInStalemate(TeamColor teamColor) {
        return !isInCheck(teamColor) && !hasLegalMove(teamColor);
    }

    /**
     * Sets this game's chessboard with a given board
     *
     * @param board the new board to use
     */
    public void setBoard(ChessBoard board) {
        this.board = board;
        castlingRights = new boolean[][]{{true, true}, {true, true}};
    }

    /**
     * Gets the current chessboard
     *
     * @return the chessboard
     */
    public ChessBoard getBoard() {
        return board;
    }

    ChessBoard simulateMove(ChessMove move) {
        ChessBoard nextBoard = board.copy();
        applyMove(nextBoard, move);
        return nextBoard;
    }

    private void applyMove(ChessBoard targetBoard, ChessMove move) {
        ChessPiece piece = targetBoard.getPiece(move.getStartPosition());
        targetBoard.addPiece(move.getStartPosition(), null);
        if (piece.getPieceType() == ChessPiece.PieceType.KING
                && Math.abs(move.getEndPosition().getColumn() - move.getStartPosition().getColumn()) == 2) {
            int row = move.getStartPosition().getRow();
            boolean kingside = move.getEndPosition().getColumn() == 7;
            ChessPosition rookStart = new ChessPosition(row, kingside ? 8 : 1);
            ChessPosition rookEnd = new ChessPosition(row, kingside ? 6 : 4);
            targetBoard.addPiece(rookEnd, targetBoard.getPiece(rookStart));
            targetBoard.addPiece(rookStart, null);
        }
        if (move.getPromotionPiece() != null) {
            piece = new ChessPiece(piece.getTeamColor(), move.getPromotionPiece());
        }
        targetBoard.addPiece(move.getEndPosition(), piece);
    }

    private void addCastlingMoves(ChessPosition start, TeamColor color, Collection<ChessMove> moves) {
        int row = color == TeamColor.WHITE ? 1 : 8;
        if (!start.equals(new ChessPosition(row, 5)) || isInCheck(color)) return;

        for (int side = 0; side < 2; side++) {
            if (!castlingRights[color.ordinal()][side]) continue;
            int rookCol = side == 0 ? 1 : 8;
            ChessPiece rook = board.getPiece(new ChessPosition(row, rookCol));
            if (rook == null || rook.getTeamColor() != color
                    || rook.getPieceType() != ChessPiece.PieceType.ROOK) continue;
            int direction = side == 0 ? -1 : 1;
            boolean clear = true;
            for (int col = 5 + direction; col != rookCol; col += direction) {
                if (board.getPiece(new ChessPosition(row, col)) != null) {
                    clear = false;
                    break;
                }
            }
            if (!clear) continue;
            ChessMove step = new ChessMove(start, new ChessPosition(row, 5 + direction), null);
            if (!isInCheck(simulateMove(step), color)) {
                moves.add(new ChessMove(start, new ChessPosition(row, 5 + 2 * direction), null));
            }
        }
    }

    private void updateCastlingRights(ChessMove move, ChessPiece piece) {
        if (piece.getPieceType() == ChessPiece.PieceType.KING) {
            Arrays.fill(castlingRights[piece.getTeamColor().ordinal()], false);
        }
        for (TeamColor color : TeamColor.values()) {
            int row = color == TeamColor.WHITE ? 1 : 8;
            for (int side = 0; side < 2; side++) {
                ChessPosition corner = new ChessPosition(row, side == 0 ? 1 : 8);
                if (move.getStartPosition().equals(corner) || move.getEndPosition().equals(corner)) {
                    castlingRights[color.ordinal()][side] = false;
                }
            }
        }
    }

    private ChessPosition findKing(ChessBoard board, TeamColor teamColor) {
        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                ChessPosition position = new ChessPosition(row, col);
                ChessPiece piece = board.getPiece(position);
                if (piece != null && piece.getTeamColor() == teamColor
                        && piece.getPieceType() == ChessPiece.PieceType.KING) {
                    return position;
                }
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChessGame that = (ChessGame) o;
        return teamTurn == that.teamTurn && Objects.equals(board, that.board)
                && Arrays.deepEquals(castlingRights, that.castlingRights);
    }

    @Override
    public int hashCode() {
        return Objects.hash(board, teamTurn, Arrays.deepHashCode(castlingRights));
    }
}
