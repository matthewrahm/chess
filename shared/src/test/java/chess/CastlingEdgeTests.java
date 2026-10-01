package chess;

import chess.ChessGame.TeamColor;
import chess.ChessPiece.PieceType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CastlingEdgeTests {
    @Test
    public void pawnAttacksPreventCrossingAndLandingForBothColors() {
        for (TeamColor color : TeamColor.values()) {
            int row = color == TeamColor.WHITE ? 1 : 8;
            int pawnRow = color == TeamColor.WHITE ? 2 : 7;
            TeamColor opponent = color == TeamColor.WHITE ? TeamColor.BLACK : TeamColor.WHITE;
            for (int col : new int[]{7, 8}) {
                ChessGame game = setup(color);
                game.getBoard().addPiece(new ChessPosition(pawnRow, col), new ChessPiece(opponent, PieceType.PAWN));
                assertFalse(game.validMoves(new ChessPosition(row, 5)).contains(castle(row, 7)));
            }
        }
    }

    @Test
    public void queensideRequiresEmptyBFileButNotSafeBFile() {
        ChessGame game = setup(TeamColor.WHITE);
        game.getBoard().addPiece(new ChessPosition(8, 2), new ChessPiece(TeamColor.BLACK, PieceType.ROOK));
        assertTrue(game.validMoves(new ChessPosition(1, 5)).contains(castle(1, 3)));
        game.getBoard().addPiece(new ChessPosition(1, 2), new ChessPiece(TeamColor.WHITE, PieceType.KNIGHT));
        assertFalse(game.validMoves(new ChessPosition(1, 5)).contains(castle(1, 3)));
    }

    @Test
    public void requiresFriendlyRookAndOriginalKingSquare() {
        for (ChessPiece replacement : new ChessPiece[]{null,
                new ChessPiece(TeamColor.WHITE, PieceType.BISHOP),
                new ChessPiece(TeamColor.BLACK, PieceType.ROOK)}) {
            ChessGame game = setup(TeamColor.WHITE);
            game.getBoard().addPiece(new ChessPosition(1, 8), replacement);
            assertFalse(game.validMoves(new ChessPosition(1, 5)).contains(castle(1, 7)));
        }
        ChessGame game = setup(TeamColor.WHITE);
        game.getBoard().addPiece(new ChessPosition(1, 5), null);
        game.getBoard().addPiece(new ChessPosition(2, 5), new ChessPiece(TeamColor.WHITE, PieceType.KING));
        assertFalse(game.validMoves(new ChessPosition(2, 5)).contains(castle(2, 7)));
    }

    @Test
    public void queriesAndRejectedMovesDoNotRemoveRights() {
        ChessGame game = setup(TeamColor.WHITE);
        ChessGame original = setup(TeamColor.WHITE);
        for (int i = 0; i < 3; i++) {
            assertTrue(game.validMoves(new ChessPosition(1, 5)).contains(castle(1, 7)));
            game.isInCheckmate(TeamColor.WHITE);
            game.isInStalemate(TeamColor.WHITE);
        }
        assertThrows(InvalidMoveException.class, () -> game.makeMove(
                new ChessMove(new ChessPosition(1, 8), new ChessPosition(2, 7), null)));
        assertEquals(original, game);
        assertEquals(original.hashCode(), game.hashCode());
    }

    @Test
    public void capturedRookCannotBeReplacedToRegainRights() throws InvalidMoveException {
        ChessGame game = setup(TeamColor.WHITE);
        game.getBoard().addPiece(new ChessPosition(2, 7), new ChessPiece(TeamColor.BLACK, PieceType.BISHOP));
        game.setTeamTurn(TeamColor.BLACK);
        game.makeMove(new ChessMove(new ChessPosition(2, 7), new ChessPosition(1, 8), null));
        game.getBoard().addPiece(new ChessPosition(1, 8), new ChessPiece(TeamColor.WHITE, PieceType.ROOK));
        assertFalse(game.validMoves(new ChessPosition(1, 5)).contains(castle(1, 7)));
        assertTrue(game.validMoves(new ChessPosition(1, 5)).contains(castle(1, 3)));
        ChessGame fresh = setup(TeamColor.WHITE);
        assertEquals(fresh.getBoard(), game.getBoard());
        assertNotEquals(fresh, game);
        game.setBoard(fresh.getBoard());
        assertEquals(fresh, game);
    }

    private ChessMove castle(int row, int col) {
        return new ChessMove(new ChessPosition(row, 5), new ChessPosition(row, col), null);
    }

    private ChessGame setup(TeamColor color) {
        ChessGame game = new ChessGame();
        ChessBoard board = new ChessBoard();
        int row = color == TeamColor.WHITE ? 1 : 8;
        TeamColor opponent = color == TeamColor.WHITE ? TeamColor.BLACK : TeamColor.WHITE;
        board.addPiece(new ChessPosition(row, 5), new ChessPiece(color, PieceType.KING));
        board.addPiece(new ChessPosition(row, 1), new ChessPiece(color, PieceType.ROOK));
        board.addPiece(new ChessPosition(row, 8), new ChessPiece(color, PieceType.ROOK));
        board.addPiece(new ChessPosition(9 - row, 5), new ChessPiece(opponent, PieceType.KING));
        game.setBoard(board);
        game.setTeamTurn(color);
        return game;
    }
}
