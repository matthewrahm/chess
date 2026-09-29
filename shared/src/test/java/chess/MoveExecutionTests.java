package chess;

import chess.ChessGame.TeamColor;
import chess.ChessPiece.PieceType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MoveExecutionTests {
    @Test
    public void captureUpdatesTheBoardAndAlternatesTurns() throws InvalidMoveException {
        ChessGame game = new ChessGame();
        ChessBoard board = game.getBoard();
        game.makeMove(move(2, 5, 4, 5));
        assertEquals(TeamColor.BLACK, game.getTeamTurn());
        game.makeMove(move(7, 4, 5, 4));
        assertEquals(TeamColor.WHITE, game.getTeamTurn());
        game.makeMove(move(4, 5, 5, 4));

        assertSame(board, game.getBoard());
        assertNull(board.getPiece(new ChessPosition(4, 5)));
        assertEquals(new ChessPiece(TeamColor.WHITE, PieceType.PAWN), board.getPiece(new ChessPosition(5, 4)));
        assertEquals(TeamColor.BLACK, game.getTeamTurn());
        assertEquals(new ChessPiece(TeamColor.BLACK, PieceType.KING), board.getPiece(new ChessPosition(8, 5)));
    }

    @Test
    public void rejectedMoveDoesNotConsumeTurnBetweenValidMoves() throws InvalidMoveException {
        ChessGame game = new ChessGame();
        game.makeMove(move(2, 5, 4, 5));
        ChessBoard snapshot = game.getBoard().copy();

        assertThrows(InvalidMoveException.class, () -> game.makeMove(move(2, 4, 4, 4)));
        assertEquals(TeamColor.BLACK, game.getTeamTurn());
        assertEquals(snapshot, game.getBoard());

        game.makeMove(move(7, 5, 5, 5));
        assertEquals(TeamColor.WHITE, game.getTeamTurn());
        assertNull(game.getBoard().getPiece(new ChessPosition(7, 5)));
    }

    @Test
    public void captureRemovesCheckAndPreservesMovingColor() throws InvalidMoveException {
        for (TeamColor color : TeamColor.values()) {
            TeamColor opponent = color == TeamColor.WHITE ? TeamColor.BLACK : TeamColor.WHITE;
            ChessGame game = new ChessGame();
            ChessBoard board = new ChessBoard();
            board.addPiece(new ChessPosition(1, 5), new ChessPiece(color, PieceType.KING));
            board.addPiece(new ChessPosition(2, 4), new ChessPiece(color, PieceType.ROOK));
            board.addPiece(new ChessPosition(2, 5), new ChessPiece(opponent, PieceType.ROOK));
            board.addPiece(new ChessPosition(8, 8), new ChessPiece(opponent, PieceType.KING));
            game.setBoard(board);
            game.setTeamTurn(color);
            assertTrue(game.isInCheck(color));

            game.makeMove(move(2, 4, 2, 5));

            assertFalse(game.isInCheck(color));
            assertNull(board.getPiece(new ChessPosition(2, 4)));
            assertEquals(new ChessPiece(color, PieceType.ROOK), board.getPiece(new ChessPosition(2, 5)));
            assertEquals(opponent, game.getTeamTurn());
        }
    }

    @Test
    public void promotionsApplyEveryChoiceForBothTeams() throws InvalidMoveException {
        for (TeamColor color : TeamColor.values()) {
            TeamColor opponent = color == TeamColor.WHITE ? TeamColor.BLACK : TeamColor.WHITE;
            for (PieceType type : new PieceType[]{PieceType.QUEEN, PieceType.ROOK,
                    PieceType.BISHOP, PieceType.KNIGHT}) {
                for (boolean capture : new boolean[]{false, true}) {
                    ChessGame game = new ChessGame();
                    ChessBoard board = new ChessBoard();
                    ChessPosition start = new ChessPosition(color == TeamColor.WHITE ? 7 : 2, 4);
                    ChessPosition end = new ChessPosition(color == TeamColor.WHITE ? 8 : 1, capture ? 5 : 4);
                    board.addPiece(new ChessPosition(1, 1), new ChessPiece(TeamColor.WHITE, PieceType.KING));
                    board.addPiece(new ChessPosition(8, 8), new ChessPiece(TeamColor.BLACK, PieceType.KING));
                    board.addPiece(start, new ChessPiece(color, PieceType.PAWN));
                    if (capture) board.addPiece(end, new ChessPiece(opponent, PieceType.KNIGHT));
                    game.setBoard(board);
                    game.setTeamTurn(color);

                    game.makeMove(new ChessMove(start, end, type));

                    assertSame(board, game.getBoard());
                    assertNull(board.getPiece(start));
                    assertEquals(new ChessPiece(color, type), board.getPiece(end));
                    assertEquals(opponent, game.getTeamTurn());
                }
            }
        }
    }

    private ChessMove move(int startRow, int startCol, int endRow, int endCol) {
        return new ChessMove(new ChessPosition(startRow, startCol), new ChessPosition(endRow, endCol), null);
    }
}
