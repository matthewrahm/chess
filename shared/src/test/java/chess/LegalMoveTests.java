package chess;

import chess.ChessGame.TeamColor;
import chess.ChessPiece.PieceType;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Set;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

public class LegalMoveTests {
    @Test
    public void emptySquareReturnsNull() {
        assertNull(new ChessGame().validMoves(new ChessPosition(4, 4)));
    }

    @Test
    public void checkingEveryStartingPieceDoesNotChangeGame() {
        ChessGame game = new ChessGame();
        ChessBoard original = game.getBoard();
        ChessBoard snapshot = original.copy();
        game.setTeamTurn(TeamColor.BLACK);
        int whiteMoves = 0;
        int blackMoves = 0;
        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                ChessPosition position = new ChessPosition(row, col);
                ChessPiece piece = original.getPiece(position);
                if (piece == null) continue;
                if (piece.getTeamColor() == TeamColor.WHITE) {
                    whiteMoves += game.validMoves(position).size();
                } else {
                    blackMoves += game.validMoves(position).size();
                }
            }
        }
        assertEquals(20, whiteMoves);
        assertEquals(20, blackMoves);
        assertSame(original, game.getBoard());
        assertEquals(snapshot, original);
        assertEquals(TeamColor.BLACK, game.getTeamTurn());
    }

    @Test
    public void kingCannotCapturePieceProtectedByPawn() {
        ChessGame game = new ChessGame();
        ChessBoard board = new ChessBoard();
        ChessPosition start = new ChessPosition(4, 4);
        ChessPosition target = new ChessPosition(5, 4);
        board.addPiece(start, new ChessPiece(TeamColor.WHITE, PieceType.KING));
        board.addPiece(target, new ChessPiece(TeamColor.BLACK, PieceType.BISHOP));
        board.addPiece(new ChessPosition(6, 3), new ChessPiece(TeamColor.BLACK, PieceType.PAWN));
        game.setBoard(board);
        ChessBoard snapshot = board.copy();

        assertFalse(game.validMoves(start).contains(new ChessMove(start, target, null)));
        assertEquals(snapshot, board);
    }

    @Test
    public void promotionCaptureCanRemoveCheckForEitherColor() {
        for (TeamColor color : TeamColor.values()) {
            TeamColor opponent = color == TeamColor.WHITE ? TeamColor.BLACK : TeamColor.WHITE;
            int lastRow = color == TeamColor.WHITE ? 8 : 1;
            ChessPosition start = new ChessPosition(color == TeamColor.WHITE ? 7 : 2, 4);
            ChessPosition target = new ChessPosition(lastRow, 5);
            ChessGame game = new ChessGame();
            ChessBoard board = new ChessBoard();
            board.addPiece(new ChessPosition(4, 5), new ChessPiece(color, PieceType.KING));
            board.addPiece(start, new ChessPiece(color, PieceType.PAWN));
            board.addPiece(target, new ChessPiece(opponent, PieceType.ROOK));
            game.setBoard(board);
            game.setTeamTurn(opponent);
            ChessBoard snapshot = board.copy();
            assertTrue(game.isInCheck(color));

            Collection<ChessMove> moves = game.validMoves(start);
            Set<ChessMove> expected = new HashSet<>();
            for (PieceType promotion : new PieceType[]{PieceType.QUEEN, PieceType.ROOK,
                    PieceType.BISHOP, PieceType.KNIGHT}) {
                expected.add(new ChessMove(start, target, promotion));
            }
            assertEquals(expected, new HashSet<>(moves));
            assertEquals(4, moves.size());
            assertEquals(snapshot, board);
            assertEquals(opponent, game.getTeamTurn());
        }
    }
}
