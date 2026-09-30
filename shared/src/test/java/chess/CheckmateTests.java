package chess;

import chess.ChessGame.TeamColor;
import chess.ChessPiece.PieceType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CheckmateTests {
    @Test
    public void detectsMateForEitherTeamWithoutChangingGame() {
        for (TeamColor color : TeamColor.values()) {
            TeamColor opponent = color == TeamColor.WHITE ? TeamColor.BLACK : TeamColor.WHITE;
            ChessGame game = cornerGame(color);
            ChessBoard board = game.getBoard();
            ChessBoard snapshot = board.copy();
            for (TeamColor turn : TeamColor.values()) {
                game.setTeamTurn(turn);
                assertTrue(game.isInCheckmate(color));
                assertFalse(game.isInCheckmate(opponent));
                assertSame(board, game.getBoard());
                assertEquals(snapshot, board);
                assertEquals(turn, game.getTeamTurn());
            }
        }
    }

    @Test
    public void requiresCheckEvenWhenKingHasNoMoves() {
        ChessGame game = cornerGame(TeamColor.WHITE);
        game.getBoard().addPiece(new ChessPosition(2, 2), null);
        game.getBoard().addPiece(new ChessPosition(2, 3), new ChessPiece(TeamColor.BLACK, PieceType.QUEEN));
        assertTrue(game.validMoves(new ChessPosition(1, 1)).isEmpty());
        assertFalse(game.isInCheck(TeamColor.WHITE));
        assertFalse(game.isInCheckmate(TeamColor.WHITE));
        assertFalse(new ChessGame().isInCheckmate(TeamColor.WHITE));
    }

    @Test
    public void anotherPieceCanCaptureTheCheckingPiece() {
        ChessGame game = cornerGame(TeamColor.WHITE);
        game.getBoard().addPiece(new ChessPosition(1, 2), new ChessPiece(TeamColor.WHITE, PieceType.ROOK));
        assertTrue(game.isInCheck(TeamColor.WHITE));
        assertTrue(game.validMoves(new ChessPosition(1, 1)).isEmpty());
        assertTrue(game.validMoves(new ChessPosition(1, 2)).contains(
                new ChessMove(new ChessPosition(1, 2), new ChessPosition(2, 2), null)));
        assertFalse(game.isInCheckmate(TeamColor.WHITE));
    }

    @Test
    public void anotherPieceCanBlockCheckWhenKingCannotMove() {
        ChessGame game = new ChessGame();
        ChessBoard board = new ChessBoard();
        board.addPiece(new ChessPosition(1, 1), new ChessPiece(TeamColor.WHITE, PieceType.KING));
        board.addPiece(new ChessPosition(2, 3), new ChessPiece(TeamColor.WHITE, PieceType.BISHOP));
        board.addPiece(new ChessPosition(3, 3), new ChessPiece(TeamColor.BLACK, PieceType.KING));
        board.addPiece(new ChessPosition(1, 8), new ChessPiece(TeamColor.BLACK, PieceType.ROOK));
        board.addPiece(new ChessPosition(4, 3), new ChessPiece(TeamColor.BLACK, PieceType.BISHOP));
        game.setBoard(board);
        assertTrue(game.isInCheck(TeamColor.WHITE));
        assertTrue(game.validMoves(new ChessPosition(1, 1)).isEmpty());
        assertTrue(game.validMoves(new ChessPosition(2, 3)).contains(
                new ChessMove(new ChessPosition(2, 3), new ChessPosition(1, 2), null)));
        assertFalse(game.isInCheckmate(TeamColor.WHITE));
    }

    private ChessGame cornerGame(TeamColor color) {
        TeamColor opponent = color == TeamColor.WHITE ? TeamColor.BLACK : TeamColor.WHITE;
        ChessGame game = new ChessGame();
        ChessBoard board = new ChessBoard();
        board.addPiece(new ChessPosition(1, 1), new ChessPiece(color, PieceType.KING));
        board.addPiece(new ChessPosition(2, 2), new ChessPiece(opponent, PieceType.QUEEN));
        board.addPiece(new ChessPosition(3, 3), new ChessPiece(opponent, PieceType.KING));
        game.setBoard(board);
        return game;
    }
}
