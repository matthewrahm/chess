package chess;

import chess.ChessGame.TeamColor;
import chess.ChessPiece.PieceType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EnPassantEdgeTests {
    @Test
    public void captureCannotExposeKingAlongRank() throws InvalidMoveException {
        ChessGame game = setup();
        ChessBoard board = game.getBoard();
        board.addPiece(new ChessPosition(1, 5), null);
        board.addPiece(new ChessPosition(5, 1), new ChessPiece(TeamColor.WHITE, PieceType.KING));
        board.addPiece(new ChessPosition(5, 8), new ChessPiece(TeamColor.BLACK, PieceType.ROOK));
        game.makeMove(move(7, 3, 5, 3));
        ChessBoard snapshot = board.copy();
        assertFalse(game.isInCheck(TeamColor.WHITE));
        assertFalse(game.validMoves(new ChessPosition(5, 2)).contains(capture()));
        assertThrows(InvalidMoveException.class, () -> game.makeMove(capture()));
        assertEquals(snapshot, board);
        assertEquals(TeamColor.WHITE, game.getTeamTurn());
    }

    @Test
    public void captureCanRemovePawnCheck() throws InvalidMoveException {
        ChessGame game = setup();
        game.getBoard().addPiece(new ChessPosition(1, 5), null);
        game.getBoard().addPiece(new ChessPosition(4, 4), new ChessPiece(TeamColor.WHITE, PieceType.KING));
        game.makeMove(move(7, 3, 5, 3));
        assertTrue(game.isInCheck(TeamColor.WHITE));
        assertTrue(game.validMoves(new ChessPosition(5, 2)).contains(capture()));
        assertFalse(game.isInCheckmate(TeamColor.WHITE));
        game.makeMove(capture());
        assertFalse(game.isInCheck(TeamColor.WHITE));
        assertNull(game.getBoard().getPiece(new ChessPosition(5, 3)));
        assertEquals(new ChessPiece(TeamColor.WHITE, PieceType.PAWN),
                game.getBoard().getPiece(new ChessPosition(6, 3)));
    }

    @Test
    public void queriesAndRejectedMovesPreserveOpportunity() throws InvalidMoveException {
        ChessGame game = setup();
        game.makeMove(move(7, 3, 5, 3));
        ChessGame same = setup();
        same.makeMove(move(7, 3, 5, 3));
        for (int i = 0; i < 3; i++) {
            assertTrue(game.validMoves(new ChessPosition(5, 2)).contains(capture()));
            game.isInCheckmate(TeamColor.WHITE);
            game.isInStalemate(TeamColor.WHITE);
        }
        assertThrows(InvalidMoveException.class, () -> game.makeMove(move(5, 2, 7, 2)));
        assertEquals(same, game);
        assertEquals(same.hashCode(), game.hashCode());
        assertDoesNotThrow(() -> game.makeMove(capture()));
    }

    @Test
    public void freshPositionAndSingleStepDoNotEnableCapture() throws InvalidMoveException {
        ChessGame game = setup();
        game.getBoard().addPiece(new ChessPosition(7, 3), null);
        game.getBoard().addPiece(new ChessPosition(6, 3), new ChessPiece(TeamColor.BLACK, PieceType.PAWN));
        game.makeMove(move(6, 3, 5, 3));
        assertFalse(game.validMoves(new ChessPosition(5, 2)).contains(capture()));

        game = setup();
        game.makeMove(move(7, 3, 5, 3));
        ChessGame fresh = new ChessGame();
        fresh.setBoard(game.getBoard().copy());
        assertNotEquals(fresh, game);
        assertFalse(fresh.validMoves(new ChessPosition(5, 2)).contains(capture()));
        game.setBoard(game.getBoard().copy());
        assertEquals(fresh, game);
        assertFalse(game.validMoves(new ChessPosition(5, 2)).contains(capture()));
    }

    @Test
    public void laterMoveExpiresOpportunityEvenAfterTurnReturns() throws InvalidMoveException {
        ChessGame game = setup();
        game.makeMove(move(7, 3, 5, 3));
        game.makeMove(move(1, 5, 1, 4));
        game.makeMove(move(8, 5, 8, 4));
        assertFalse(game.validMoves(new ChessPosition(5, 2)).contains(capture()));
        assertThrows(InvalidMoveException.class, () -> game.makeMove(capture()));
    }

    private ChessMove capture() {
        return move(5, 2, 6, 3);
    }

    private ChessMove move(int r1, int c1, int r2, int c2) {
        return new ChessMove(new ChessPosition(r1, c1), new ChessPosition(r2, c2), null);
    }

    private ChessGame setup() {
        ChessGame game = new ChessGame();
        ChessBoard board = new ChessBoard();
        board.addPiece(new ChessPosition(1, 5), new ChessPiece(TeamColor.WHITE, PieceType.KING));
        board.addPiece(new ChessPosition(8, 5), new ChessPiece(TeamColor.BLACK, PieceType.KING));
        board.addPiece(new ChessPosition(5, 2), new ChessPiece(TeamColor.WHITE, PieceType.PAWN));
        board.addPiece(new ChessPosition(7, 3), new ChessPiece(TeamColor.BLACK, PieceType.PAWN));
        game.setBoard(board);
        game.setTeamTurn(TeamColor.BLACK);
        return game;
    }
}
