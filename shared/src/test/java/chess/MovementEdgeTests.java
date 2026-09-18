package chess;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MovementEdgeTests {
    @Test
    public void resetRestoresChangedBoard() {
        ChessBoard board = new ChessBoard();
        board.resetBoard();
        ChessBoard startingBoard = board.copy();
        board.addPiece(new ChessPosition(1, 1), null);
        board.addPiece(new ChessPosition(8, 5), null);
        board.addPiece(new ChessPosition(4, 4),
                new ChessPiece(ChessGame.TeamColor.WHITE, ChessPiece.PieceType.QUEEN));

        board.resetBoard();
        assertEquals(startingBoard, board);
        board.resetBoard();
        assertEquals(startingBoard, board);
    }

    @Test
    public void findingMovesDoesNotChangeBoard() {
        for (ChessGame.TeamColor color : ChessGame.TeamColor.values()) {
            for (ChessPiece.PieceType type : ChessPiece.PieceType.values()) {
                ChessBoard board = new ChessBoard();
                ChessPosition position = new ChessPosition(4, 4);
                ChessPiece piece = new ChessPiece(color, type);
                ChessGame.TeamColor opponent = color == ChessGame.TeamColor.WHITE
                        ? ChessGame.TeamColor.BLACK : ChessGame.TeamColor.WHITE;
                board.addPiece(position, piece);
                board.addPiece(new ChessPosition(5, 5), new ChessPiece(color, ChessPiece.PieceType.PAWN));
                board.addPiece(new ChessPosition(3, 3), new ChessPiece(opponent, ChessPiece.PieceType.PAWN));
                ChessBoard original = board.copy();

                var moves = new HashSet<>(piece.pieceMoves(board, position));
                assertEquals(original, board);
                assertEquals(moves, new HashSet<>(piece.pieceMoves(board, position)));
                assertEquals(original, board);
            }
        }
    }

    @Test
    public void pawnsAtLastRankHaveNoMoves() {
        for (ChessGame.TeamColor color : ChessGame.TeamColor.values()) {
            int row = color == ChessGame.TeamColor.WHITE ? 8 : 1;
            for (int col = 1; col <= 8; col++) {
                ChessBoard board = new ChessBoard();
                ChessPosition position = new ChessPosition(row, col);
                ChessPiece pawn = new ChessPiece(color, ChessPiece.PieceType.PAWN);
                board.addPiece(position, pawn);
                assertTrue(pawn.pieceMoves(board, position).isEmpty());
            }
        }
    }

    @Test
    public void pieceMovesDoesNotFilterPinnedPieces() {
        ChessBoard board = new ChessBoard();
        ChessPosition position = new ChessPosition(4, 2);
        ChessPiece rook = new ChessPiece(ChessGame.TeamColor.WHITE, ChessPiece.PieceType.ROOK);
        board.addPiece(position, rook);
        board.addPiece(new ChessPosition(4, 1),
                new ChessPiece(ChessGame.TeamColor.WHITE, ChessPiece.PieceType.KING));
        board.addPiece(new ChessPosition(4, 8),
                new ChessPiece(ChessGame.TeamColor.BLACK, ChessPiece.PieceType.ROOK));

        assertTrue(rook.pieceMoves(board, position).contains(
                new ChessMove(position, new ChessPosition(5, 2), null)));
    }
}
