package chess;

import java.util.ArrayList;
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
    private TeamColor currentTurn;

    private boolean whiteKingMoved;
    private boolean blackKingMoved;
    private boolean whiteRookKingsideMoved;
    private boolean whiteRookQueensideMoved;
    private boolean blackRookKingsideMoved;
    private boolean blackRookQueensideMoved;

    private ChessMove lastMove;

    public ChessGame() {
        board = new ChessBoard();
        board.resetBoard();
        currentTurn = TeamColor.WHITE;
    }

    /**
     * @return Which team's turn it is
     */
    public TeamColor getTeamTurn() {
        return currentTurn;
    }

    /**
     * Set's which teams turn it is
     *
     * @param team the team whose turn it is
     */
    public void setTeamTurn(TeamColor team) {
        this.currentTurn = team;
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

        TeamColor team = piece.getTeamColor();
        Collection<ChessMove> rawMoves = piece.pieceMoves(board, startPosition);
        Collection<ChessMove> legal = new ArrayList<>();

        for (ChessMove move : rawMoves) {
            if (!leavesKingInCheck(move, team)) {
                legal.add(move);
            }
        }

        if (piece.getPieceType() == ChessPiece.PieceType.KING) {
            addCastlingMoves(startPosition, team, legal);
        }

        if (piece.getPieceType() == ChessPiece.PieceType.PAWN) {
            addEnPassantMoves(startPosition, team, legal);
        }

        return legal;
    }

    private void addCastlingMoves(ChessPosition kingPos, TeamColor team, Collection<ChessMove> moves) {
        if (isInCheck(team)) return;

        int row = (team == TeamColor.WHITE) ? 1 : 8;
        boolean kingMoved = (team == TeamColor.WHITE) ? whiteKingMoved : blackKingMoved;
        if (kingMoved) return;
        if (kingPos.getRow() != row || kingPos.getColumn() != 5) return;

        // Kingside
        boolean rookKingsideMoved = (team == TeamColor.WHITE) ? whiteRookKingsideMoved : blackRookKingsideMoved;
        if (!rookKingsideMoved) {
            ChessPosition rookPos = new ChessPosition(row, 8);
            ChessPiece rook = board.getPiece(rookPos);
            if (rook != null && rook.getPieceType() == ChessPiece.PieceType.ROOK && rook.getTeamColor() == team) {
                if (board.getPiece(new ChessPosition(row, 6)) == null
                        && board.getPiece(new ChessPosition(row, 7)) == null) {
                    if (!isSquareAttacked(new ChessPosition(row, 6), team)
                            && !isSquareAttacked(new ChessPosition(row, 7), team)) {
                        moves.add(new ChessMove(kingPos, new ChessPosition(row, 7), null));
                    }
                }
            }
        }

        // Queenside
        boolean rookQueensideMoved = (team == TeamColor.WHITE) ? whiteRookQueensideMoved : blackRookQueensideMoved;
        if (!rookQueensideMoved) {
            ChessPosition rookPos = new ChessPosition(row, 1);
            ChessPiece rook = board.getPiece(rookPos);
            if (rook != null && rook.getPieceType() == ChessPiece.PieceType.ROOK && rook.getTeamColor() == team) {
                if (board.getPiece(new ChessPosition(row, 2)) == null
                        && board.getPiece(new ChessPosition(row, 3)) == null
                        && board.getPiece(new ChessPosition(row, 4)) == null) {
                    if (!isSquareAttacked(new ChessPosition(row, 4), team)
                            && !isSquareAttacked(new ChessPosition(row, 3), team)) {
                        moves.add(new ChessMove(kingPos, new ChessPosition(row, 3), null));
                    }
                }
            }
        }
    }

    private boolean isSquareAttacked(ChessPosition square, TeamColor defender) {
        TeamColor opponent = (defender == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;
        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                ChessPosition pos = new ChessPosition(row, col);
                ChessPiece piece = board.getPiece(pos);
                if (piece != null && piece.getTeamColor() == opponent) {
                    Collection<ChessMove> moves = piece.pieceMoves(board, pos);
                    for (ChessMove move : moves) {
                        if (move.getEndPosition().equals(square)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private void addEnPassantMoves(ChessPosition pawnPos, TeamColor team, Collection<ChessMove> moves) {
        if (lastMove == null) return;

        int enPassantRow = (team == TeamColor.WHITE) ? 5 : 4;
        if (pawnPos.getRow() != enPassantRow) return;

        ChessPiece lastPiece = board.getPiece(lastMove.getEndPosition());
        if (lastPiece == null || lastPiece.getPieceType() != ChessPiece.PieceType.PAWN) return;
        if (lastPiece.getTeamColor() == team) return;
        if (Math.abs(lastMove.getStartPosition().getRow() - lastMove.getEndPosition().getRow()) != 2) return;
        if (lastMove.getEndPosition().getRow() != pawnPos.getRow()) return;
        if (Math.abs(lastMove.getEndPosition().getColumn() - pawnPos.getColumn()) != 1) return;

        int direction = (team == TeamColor.WHITE) ? 1 : -1;
        ChessMove enPassantMove = new ChessMove(pawnPos,
                new ChessPosition(pawnPos.getRow() + direction, lastMove.getEndPosition().getColumn()), null);

        if (!leavesKingInCheck(enPassantMove, team)) {
            moves.add(enPassantMove);
        }
    }

    private boolean leavesKingInCheck(ChessMove move, TeamColor team) {
        ChessBoard saved = board;
        board = board.copy();

        ChessPiece piece = board.getPiece(move.getStartPosition());
        boolean isEnPassant = piece.getPieceType() == ChessPiece.PieceType.PAWN
                && move.getStartPosition().getColumn() != move.getEndPosition().getColumn()
                && board.getPiece(move.getEndPosition()) == null;

        board.addPiece(move.getStartPosition(), null);
        board.addPiece(move.getEndPosition(), piece);

        if (isEnPassant) {
            board.addPiece(new ChessPosition(move.getStartPosition().getRow(),
                    move.getEndPosition().getColumn()), null);
        }

        boolean inCheck = isInCheck(team);
        board = saved;
        return inCheck;
    }

    /**
     * Makes a move in a chess game
     *
     * @param move chess move to perform
     * @throws InvalidMoveException if move is invalid
     */
    public void makeMove(ChessMove move) throws InvalidMoveException {
        ChessPosition start = move.getStartPosition();
        ChessPiece piece = board.getPiece(start);

        if (piece == null) {
            throw new InvalidMoveException("No piece at start position");
        }
        if (piece.getTeamColor() != currentTurn) {
            throw new InvalidMoveException("Not your turn");
        }

        Collection<ChessMove> legal = validMoves(start);
        if (legal == null || !legal.contains(move)) {
            throw new InvalidMoveException("Invalid move");
        }

        // Track king/rook movement for castling
        if (start.getRow() == 1 && start.getColumn() == 5) whiteKingMoved = true;
        if (start.getRow() == 8 && start.getColumn() == 5) blackKingMoved = true;
        if (start.getRow() == 1 && start.getColumn() == 1) whiteRookQueensideMoved = true;
        if (start.getRow() == 1 && start.getColumn() == 8) whiteRookKingsideMoved = true;
        if (start.getRow() == 8 && start.getColumn() == 1) blackRookQueensideMoved = true;
        if (start.getRow() == 8 && start.getColumn() == 8) blackRookKingsideMoved = true;

        // Detect en passant before executing
        boolean isEnPassant = piece.getPieceType() == ChessPiece.PieceType.PAWN
                && start.getColumn() != move.getEndPosition().getColumn()
                && board.getPiece(move.getEndPosition()) == null;

        board.addPiece(start, null);
        if (move.getPromotionPiece() != null) {
            board.addPiece(move.getEndPosition(),
                    new ChessPiece(piece.getTeamColor(), move.getPromotionPiece()));
        } else {
            board.addPiece(move.getEndPosition(), piece);
        }

        // Handle en passant capture
        if (isEnPassant) {
            board.addPiece(new ChessPosition(start.getRow(), move.getEndPosition().getColumn()), null);
        }

        // Handle castling rook movement
        if (piece.getPieceType() == ChessPiece.PieceType.KING) {
            int colDiff = move.getEndPosition().getColumn() - start.getColumn();
            if (colDiff == 2) {
                ChessPosition rookFrom = new ChessPosition(start.getRow(), 8);
                ChessPosition rookTo = new ChessPosition(start.getRow(), 6);
                board.addPiece(rookTo, board.getPiece(rookFrom));
                board.addPiece(rookFrom, null);
            } else if (colDiff == -2) {
                ChessPosition rookFrom = new ChessPosition(start.getRow(), 1);
                ChessPosition rookTo = new ChessPosition(start.getRow(), 4);
                board.addPiece(rookTo, board.getPiece(rookFrom));
                board.addPiece(rookFrom, null);
            }
        }

        lastMove = move;
        currentTurn = (currentTurn == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;
    }

    /**
     * Determines if the given team is in check
     *
     * @param teamColor which team to check for check
     * @return True if the specified team is in check
     */
    public boolean isInCheck(TeamColor teamColor) {
        ChessPosition kingPos = findKing(teamColor);
        if (kingPos == null) return false;

        TeamColor opponent = (teamColor == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;
        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                ChessPosition pos = new ChessPosition(row, col);
                ChessPiece piece = board.getPiece(pos);
                if (piece != null && piece.getTeamColor() == opponent) {
                    Collection<ChessMove> moves = piece.pieceMoves(board, pos);
                    for (ChessMove move : moves) {
                        if (move.getEndPosition().equals(kingPos)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private ChessPosition findKing(TeamColor teamColor) {
        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                ChessPosition pos = new ChessPosition(row, col);
                ChessPiece piece = board.getPiece(pos);
                if (piece != null && piece.getTeamColor() == teamColor
                        && piece.getPieceType() == ChessPiece.PieceType.KING) {
                    return pos;
                }
            }
        }
        return null;
    }

    /**
     * Determines if the given team is in checkmate
     *
     * @param teamColor which team to check for checkmate
     * @return True if the specified team is in checkmate
     */
    public boolean isInCheckmate(TeamColor teamColor) {
        return isInCheck(teamColor) && !hasValidMoves(teamColor);
    }

    /**
     * Determines if the given team is in stalemate, which here is defined as having
     * no valid moves while not in check.
     *
     * @param teamColor which team to check for stalemate
     * @return True if the specified team is in stalemate, otherwise false
     */
    public boolean isInStalemate(TeamColor teamColor) {
        return !isInCheck(teamColor) && !hasValidMoves(teamColor);
    }

    private boolean hasValidMoves(TeamColor teamColor) {
        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                ChessPosition pos = new ChessPosition(row, col);
                ChessPiece piece = board.getPiece(pos);
                if (piece != null && piece.getTeamColor() == teamColor) {
                    Collection<ChessMove> moves = validMoves(pos);
                    if (moves != null && !moves.isEmpty()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Sets this game's chessboard with a given board
     *
     * @param board the new board to use
     */
    public void setBoard(ChessBoard board) {
        this.board = board;
        whiteKingMoved = false;
        blackKingMoved = false;
        whiteRookKingsideMoved = false;
        whiteRookQueensideMoved = false;
        blackRookKingsideMoved = false;
        blackRookQueensideMoved = false;
        lastMove = null;
    }

    /**
     * Gets the current chessboard
     *
     * @return the chessboard
     */
    public ChessBoard getBoard() {
        return board;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChessGame that = (ChessGame) o;
        return currentTurn == that.currentTurn && Objects.equals(board, that.board);
    }

    @Override
    public int hashCode() {
        return Objects.hash(board, currentTurn);
    }
}
