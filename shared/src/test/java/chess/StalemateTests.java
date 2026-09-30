package chess;

import chess.ChessGame.TeamColor;
import chess.ChessPiece.PieceType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class StalemateTests {
    @Test
    public void detectsStalemateForEitherTeamWithoutChangingGame() {
        for (TeamColor color : TeamColor.values()) {
            TeamColor opponent = color == TeamColor.WHITE ? TeamColor.BLACK : TeamColor.WHITE;
            ChessGame game = cornerGame(color);
            ChessBoard board = game.getBoard();
            ChessBoard snapshot = board.copy();
            for (TeamColor turn : TeamColor.values()) {
                game.setTeamTurn(turn);
                assertTrue(game.isInStalemate(color));
                assertFalse(game.isInCheckmate(color));
                assertFalse(game.isInStalemate(opponent));
                assertSame(board, game.getBoard());
                assertEquals(snapshot, board);
                assertEquals(turn, game.getTeamTurn());
            }
        }
    }

    @Test
    public void anotherPieceWithALegalMovePreventsStalemate() {
        for (TeamColor color : TeamColor.values()) {
            ChessGame game = cornerGame(color);
            ChessPosition pawn = new ChessPosition(color == TeamColor.WHITE ? 2 : 7, 8);
            game.getBoard().addPiece(pawn, new ChessPiece(color, PieceType.PAWN));
            assertTrue(game.validMoves(new ChessPosition(1, 1)).isEmpty());
            assertFalse(game.validMoves(pawn).isEmpty());
            assertFalse(game.isInStalemate(color));
        }
    }

    @Test
    public void checkmateIsNotStalemate() {
        for (TeamColor color : TeamColor.values()) {
            TeamColor opponent = color == TeamColor.WHITE ? TeamColor.BLACK : TeamColor.WHITE;
            ChessGame game = cornerGame(color);
            game.getBoard().addPiece(new ChessPosition(2, 3), null);
            game.getBoard().addPiece(new ChessPosition(2, 2), new ChessPiece(opponent, PieceType.QUEEN));
            assertTrue(game.isInCheckmate(color));
            assertFalse(game.isInStalemate(color));
        }
    }

    @Test
    public void startingPositionIsNotStalemateForEitherTeam() {
        ChessGame game = new ChessGame();
        assertFalse(game.isInStalemate(TeamColor.WHITE));
        assertFalse(game.isInStalemate(TeamColor.BLACK));
    }

    private ChessGame cornerGame(TeamColor color) {
        TeamColor opponent = color == TeamColor.WHITE ? TeamColor.BLACK : TeamColor.WHITE;
        ChessGame game = new ChessGame();
        ChessBoard board = new ChessBoard();
        board.addPiece(new ChessPosition(1, 1), new ChessPiece(color, PieceType.KING));
        board.addPiece(new ChessPosition(2, 3), new ChessPiece(opponent, PieceType.QUEEN));
        board.addPiece(new ChessPosition(3, 3), new ChessPiece(opponent, PieceType.KING));
        game.setBoard(board);
        return game;
    }
}
