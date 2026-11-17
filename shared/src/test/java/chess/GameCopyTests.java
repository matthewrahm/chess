package chess;

import org.junit.jupiter.api.Test;

import static chess.ChessGame.TeamColor.*;
import static chess.ChessPiece.PieceType.*;
import static org.junit.jupiter.api.Assertions.*;

class GameCopyTests {
    @Test
    void movesOnCopyLeaveOriginalBoardTurnAndEndStateUnchanged() throws Exception {
        var original = new ChessGame();
        var copied = original.copy();
        assertEquals(original, copied);
        assertNotSame(original.getBoard(), copied.getBoard());
        copied.makeMove(move(2, 5, 4, 5));
        copied.endGame();
        assertEquals(new ChessGame(), original);
        original.getBoard().addPiece(new ChessPosition(1, 1), null);
        assertNotNull(copied.getBoard().getPiece(new ChessPosition(1, 1)));
    }

    @Test
    void castlingRightsArePreservedAndIndependentForBothColors() throws Exception {
        var original = castlingPosition();
        var copied = original.copy();
        copied.makeMove(move(1, 8, 2, 8));
        copied.makeMove(move(8, 8, 7, 8));
        copied.makeMove(move(2, 8, 1, 8));
        copied.makeMove(move(7, 8, 8, 8));
        var restored = copied.copy();
        for (int row : new int[]{1, 8}) {
            var king = new ChessPosition(row, 5);
            assertTrue(original.validMoves(king).contains(move(row, 5, row, 7)));
            assertFalse(restored.validMoves(king).contains(move(row, 5, row, 7)));
            assertTrue(restored.validMoves(king).contains(move(row, 5, row, 3)));
        }
        assertEquals(original.getBoard(), copied.getBoard());
        assertNotEquals(original, copied);
        assertEquals(copied, restored);
        assertEquals(copied.hashCode(), restored.hashCode());
    }

    @Test
    void enPassantOpportunitySurvivesCopyAndExpiresIndependently() throws Exception {
        var original = new ChessGame();
        original.makeMove(move(2, 5, 4, 5));
        original.makeMove(move(7, 1, 6, 1));
        original.makeMove(move(4, 5, 5, 5));
        original.makeMove(move(7, 4, 5, 4));
        var copied = original.copy();
        var pawn = new ChessPosition(5, 5);
        var capture = move(5, 5, 6, 4);
        assertTrue(copied.validMoves(pawn).contains(capture));
        copied.makeMove(move(2, 1, 3, 1));
        copied.makeMove(move(6, 1, 5, 1));
        assertFalse(copied.validMoves(pawn).contains(capture));
        assertTrue(original.validMoves(pawn).contains(capture));
        original.copy().makeMove(capture);
        assertNotNull(original.getBoard().getPiece(new ChessPosition(5, 4)));
    }

    private ChessGame castlingPosition() {
        var game = new ChessGame();
        var board = new ChessBoard();
        for (var color : ChessGame.TeamColor.values()) {
            int row = color == WHITE ? 1 : 8;
            board.addPiece(new ChessPosition(row, 5), new ChessPiece(color, KING));
            board.addPiece(new ChessPosition(row, 1), new ChessPiece(color, ROOK));
            board.addPiece(new ChessPosition(row, 8), new ChessPiece(color, ROOK));
        }
        game.setBoard(board);
        return game;
    }

    private ChessMove move(int r1, int c1, int r2, int c2) {
        return new ChessMove(new ChessPosition(r1, c1), new ChessPosition(r2, c2), null);
    }
}
