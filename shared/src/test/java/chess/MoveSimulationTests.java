package chess;

import chess.ChessGame.TeamColor;
import chess.ChessPiece.PieceType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MoveSimulationTests {
    @Test
    public void movingPieceLeavesOriginalGameUnchanged() {
        ChessGame game = new ChessGame();
        ChessBoard original = game.getBoard();
        ChessBoard snapshot = original.copy();
        ChessPosition start = new ChessPosition(2, 5);
        ChessPosition end = new ChessPosition(4, 5);

        ChessBoard result = game.simulateMove(new ChessMove(start, end, null));

        assertNotSame(original, result);
        assertSame(original, game.getBoard());
        assertEquals(snapshot, original);
        assertEquals(TeamColor.WHITE, game.getTeamTurn());
        assertNull(result.getPiece(start));
        assertEquals(original.getPiece(start), result.getPiece(end));
        assertEquals(original.getPiece(new ChessPosition(1, 5)), result.getPiece(new ChessPosition(1, 5)));
    }

    @Test
    public void captureReplacesOnlyTheCopiedDestination() {
        ChessGame game = new ChessGame();
        ChessBoard board = new ChessBoard();
        ChessPosition start = new ChessPosition(4, 4);
        ChessPosition end = new ChessPosition(4, 7);
        ChessPiece rook = new ChessPiece(TeamColor.BLACK, PieceType.ROOK);
        ChessPiece bishop = new ChessPiece(TeamColor.WHITE, PieceType.BISHOP);
        board.addPiece(start, rook);
        board.addPiece(end, bishop);
        game.setBoard(board);
        game.setTeamTurn(TeamColor.BLACK);

        ChessBoard result = game.simulateMove(new ChessMove(start, end, null));

        assertNull(result.getPiece(start));
        assertEquals(rook, result.getPiece(end));
        assertEquals(rook, board.getPiece(start));
        assertEquals(bishop, board.getPiece(end));
        assertEquals(TeamColor.BLACK, game.getTeamTurn());
    }

    @Test
    public void promotionPreservesColorAndOriginalPawn() {
        for (TeamColor color : TeamColor.values()) {
            for (PieceType type : new PieceType[]{PieceType.QUEEN, PieceType.ROOK,
                    PieceType.BISHOP, PieceType.KNIGHT}) {
                ChessGame game = new ChessGame();
                ChessBoard board = new ChessBoard();
                ChessPosition start = new ChessPosition(color == TeamColor.WHITE ? 7 : 2, 4);
                ChessPosition end = new ChessPosition(color == TeamColor.WHITE ? 8 : 1, 5);
                ChessPiece pawn = new ChessPiece(color, PieceType.PAWN);
                TeamColor opponent = color == TeamColor.WHITE ? TeamColor.BLACK : TeamColor.WHITE;
                ChessPiece captured = new ChessPiece(opponent, PieceType.ROOK);
                board.addPiece(start, pawn);
                board.addPiece(end, captured);
                game.setBoard(board);

                ChessBoard result = game.simulateMove(new ChessMove(start, end, type));

                assertNull(result.getPiece(start));
                assertEquals(new ChessPiece(color, type), result.getPiece(end));
                assertEquals(pawn, board.getPiece(start));
                assertEquals(captured, board.getPiece(end));
            }
        }
    }
}
