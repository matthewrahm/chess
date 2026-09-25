package chess;

import org.junit.jupiter.api.Test;
import passoff.chess.TestUtilities;

import static org.junit.jupiter.api.Assertions.*;

public class GameSetupTests {
    @Test
    public void newGameStartsWithWhiteAndStandardBoard() {
        ChessGame game = new ChessGame();
        assertEquals(ChessGame.TeamColor.WHITE, game.getTeamTurn());
        assertEquals(TestUtilities.defaultBoard(), game.getBoard());
    }

    @Test
    public void gamesHaveIndependentState() {
        ChessGame first = new ChessGame();
        ChessGame second = new ChessGame();
        first.setTeamTurn(ChessGame.TeamColor.BLACK);
        first.getBoard().addPiece(new ChessPosition(1, 1), null);

        assertEquals(ChessGame.TeamColor.BLACK, first.getTeamTurn());
        assertEquals(ChessGame.TeamColor.WHITE, second.getTeamTurn());
        assertEquals(TestUtilities.defaultBoard(), second.getBoard());
        assertNull(first.getBoard().getPiece(new ChessPosition(1, 1)));
    }

    @Test
    public void replacingBoardPreservesTurn() {
        ChessGame game = new ChessGame();
        ChessBoard board = new ChessBoard();
        ChessPosition position = new ChessPosition(4, 4);
        ChessPiece king = new ChessPiece(ChessGame.TeamColor.BLACK, ChessPiece.PieceType.KING);
        board.addPiece(position, king);
        game.setTeamTurn(ChessGame.TeamColor.BLACK);
        game.setBoard(board);

        assertEquals(board, game.getBoard());
        assertEquals(king, game.getBoard().getPiece(position));
        assertEquals(ChessGame.TeamColor.BLACK, game.getTeamTurn());
    }

    @Test
    public void equalityUsesBoardAndTurn() {
        ChessGame first = new ChessGame();
        ChessGame second = new ChessGame();
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, null);
        assertNotEquals(first, new ChessBoard());

        second.setTeamTurn(ChessGame.TeamColor.BLACK);
        assertNotEquals(first, second);
        second.setTeamTurn(ChessGame.TeamColor.WHITE);
        second.getBoard().addPiece(new ChessPosition(2, 1), null);
        assertNotEquals(first, second);
    }
}
