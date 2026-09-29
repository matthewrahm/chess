package chess;

import chess.ChessGame.TeamColor;
import chess.ChessPiece.PieceType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MoveValidationTests {
    @Test
    public void rejectsMissingAndOutOfBoundsPositions() {
        ChessGame game = new ChessGame();
        ChessPosition start = new ChessPosition(2, 5);
        ChessPosition end = new ChessPosition(4, 5);
        assertRejected(game, null);
        assertRejected(game, new ChessMove(null, end, null));
        assertRejected(game, new ChessMove(start, null, null));
        for (ChessPosition position : new ChessPosition[]{new ChessPosition(0, 5),
                new ChessPosition(9, 5), new ChessPosition(2, 0), new ChessPosition(2, 9)}) {
            assertRejected(game, new ChessMove(position, end, null));
            assertRejected(game, new ChessMove(start, position, null));
        }
    }

    @Test
    public void rejectsEmptySquareAndWrongTurn() {
        ChessGame game = new ChessGame();
        assertRejected(game, new ChessMove(new ChessPosition(4, 4), new ChessPosition(5, 4), null));
        assertRejected(game, new ChessMove(new ChessPosition(7, 5), new ChessPosition(5, 5), null));
        game.setTeamTurn(TeamColor.BLACK);
        assertRejected(game, new ChessMove(new ChessPosition(2, 5), new ChessPosition(4, 5), null));
    }

    @Test
    public void rejectsBlockedMovesFriendlyCapturesAndInvalidMovement() {
        ChessGame game = new ChessGame();
        assertRejected(game, new ChessMove(new ChessPosition(1, 1), new ChessPosition(4, 1), null));
        assertRejected(game, new ChessMove(new ChessPosition(1, 3), new ChessPosition(2, 4), null));
        assertRejected(game, new ChessMove(new ChessPosition(2, 5), new ChessPosition(5, 5), null));
        assertRejected(game, new ChessMove(new ChessPosition(2, 5), new ChessPosition(3, 6), null));
        assertRejected(game, new ChessMove(new ChessPosition(2, 5), new ChessPosition(2, 5), null));
    }

    @Test
    public void rejectsMovesThatExposeKingOrIgnoreCheck() {
        ChessGame game = new ChessGame();
        ChessBoard board = new ChessBoard();
        board.addPiece(new ChessPosition(1, 5), new ChessPiece(TeamColor.WHITE, PieceType.KING));
        board.addPiece(new ChessPosition(2, 5), new ChessPiece(TeamColor.WHITE, PieceType.ROOK));
        board.addPiece(new ChessPosition(8, 5), new ChessPiece(TeamColor.BLACK, PieceType.ROOK));
        board.addPiece(new ChessPosition(8, 8), new ChessPiece(TeamColor.BLACK, PieceType.KING));
        game.setBoard(board);
        assertRejected(game, new ChessMove(new ChessPosition(2, 5), new ChessPosition(2, 4), null));

        board.addPiece(new ChessPosition(2, 5), null);
        board.addPiece(new ChessPosition(1, 1), new ChessPiece(TeamColor.WHITE, PieceType.ROOK));
        assertTrue(game.isInCheck(TeamColor.WHITE));
        assertRejected(game, new ChessMove(new ChessPosition(1, 1), new ChessPosition(2, 1), null));
    }

    @Test
    public void rejectsInvalidAndMissingPromotionChoices() {
        for (TeamColor color : TeamColor.values()) {
            ChessGame game = new ChessGame();
            ChessBoard board = new ChessBoard();
            ChessPosition start = new ChessPosition(color == TeamColor.WHITE ? 7 : 2, 4);
            ChessPosition end = new ChessPosition(color == TeamColor.WHITE ? 8 : 1, 4);
            board.addPiece(start, new ChessPiece(color, PieceType.PAWN));
            game.setBoard(board);
            game.setTeamTurn(color);
            for (PieceType promotion : new PieceType[]{null, PieceType.KING, PieceType.PAWN}) {
                assertRejected(game, new ChessMove(start, end, promotion));
            }
        }
    }

    @Test
    public void rejectsPromotionOnOrdinaryMoves() {
        ChessGame game = new ChessGame();
        assertRejected(game, new ChessMove(new ChessPosition(2, 5), new ChessPosition(3, 5), PieceType.QUEEN));
        assertRejected(game, new ChessMove(new ChessPosition(1, 2), new ChessPosition(3, 3), PieceType.QUEEN));
    }

    private void assertRejected(ChessGame game, ChessMove move) {
        ChessBoard board = game.getBoard();
        ChessBoard snapshot = board.copy();
        TeamColor turn = game.getTeamTurn();
        assertThrows(InvalidMoveException.class, () -> game.makeMove(move));
        assertSame(board, game.getBoard());
        assertEquals(snapshot, game.getBoard());
        assertEquals(turn, game.getTeamTurn());
    }
}
