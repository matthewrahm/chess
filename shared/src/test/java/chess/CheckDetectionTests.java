package chess;

import chess.ChessGame.TeamColor;
import chess.ChessPiece.PieceType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CheckDetectionTests {
    @Test
    public void startingKingsAreNotInCheck() {
        ChessGame game = new ChessGame();
        assertFalse(game.isInCheck(TeamColor.WHITE));
        assertFalse(game.isInCheck(TeamColor.BLACK));
    }

    @Test
    public void everyPieceCanCheckEitherColor() {
        for (TeamColor color : TeamColor.values()) {
            TeamColor opponent = color == TeamColor.WHITE ? TeamColor.BLACK : TeamColor.WHITE;
            for (PieceType type : PieceType.values()) {
                ChessGame game = new ChessGame();
                ChessBoard board = new ChessBoard();
                board.addPiece(new ChessPosition(4, 4), new ChessPiece(color, PieceType.KING));
                ChessPosition attacker = switch (type) {
                    case KING -> new ChessPosition(5, 5);
                    case QUEEN -> new ChessPosition(4, 7);
                    case BISHOP -> new ChessPosition(7, 7);
                    case KNIGHT -> new ChessPosition(6, 5);
                    case ROOK -> new ChessPosition(7, 4);
                    case PAWN -> new ChessPosition(opponent == TeamColor.WHITE ? 3 : 5, 3);
                };
                board.addPiece(attacker, new ChessPiece(opponent, type));
                game.setBoard(board);
                ChessBoard original = board.copy();

                for (TeamColor turn : TeamColor.values()) {
                    game.setTeamTurn(turn);
                    assertTrue(game.isInCheck(color), type + " should check " + color);
                    assertEquals(original, game.getBoard());
                    assertEquals(turn, game.getTeamTurn());
                }
            }
        }
    }

    @Test
    public void eitherColorCanBlockSlidingAttacks() {
        for (PieceType type : new PieceType[]{PieceType.ROOK, PieceType.BISHOP, PieceType.QUEEN}) {
            for (TeamColor blockerColor : TeamColor.values()) {
                ChessGame game = new ChessGame();
                ChessBoard board = new ChessBoard();
                boolean diagonal = type == PieceType.BISHOP;
                ChessPosition blocker = new ChessPosition(6, diagonal ? 6 : 4);
                board.addPiece(new ChessPosition(4, 4), new ChessPiece(TeamColor.WHITE, PieceType.KING));
                board.addPiece(new ChessPosition(8, diagonal ? 8 : 4), new ChessPiece(TeamColor.BLACK, type));
                board.addPiece(blocker, new ChessPiece(blockerColor, PieceType.PAWN));
                game.setBoard(board);

                assertFalse(game.isInCheck(TeamColor.WHITE));
                board.addPiece(blocker, null);
                assertTrue(game.isInCheck(TeamColor.WHITE));
            }
        }
    }

    @Test
    public void pawnsDoNotAttackForwardOrBackward() {
        ChessGame game = new ChessGame();
        ChessBoard board = new ChessBoard();
        board.addPiece(new ChessPosition(4, 4), new ChessPiece(TeamColor.WHITE, PieceType.KING));
        board.addPiece(new ChessPosition(5, 4), new ChessPiece(TeamColor.BLACK, PieceType.PAWN));
        board.addPiece(new ChessPosition(3, 3), new ChessPiece(TeamColor.BLACK, PieceType.PAWN));
        game.setBoard(board);
        assertFalse(game.isInCheck(TeamColor.WHITE));
    }

    @Test
    public void friendlyPiecesDoNotCheckTheirKing() {
        ChessGame game = new ChessGame();
        ChessBoard board = new ChessBoard();
        board.addPiece(new ChessPosition(4, 4), new ChessPiece(TeamColor.WHITE, PieceType.KING));
        board.addPiece(new ChessPosition(4, 8), new ChessPiece(TeamColor.WHITE, PieceType.ROOK));
        game.setBoard(board);
        assertFalse(game.isInCheck(TeamColor.WHITE));
    }

    @Test
    public void pinnedOpponentStillAttacksKing() {
        ChessGame game = new ChessGame();
        ChessBoard board = new ChessBoard();
        board.addPiece(new ChessPosition(4, 4), new ChessPiece(TeamColor.WHITE, PieceType.KING));
        board.addPiece(new ChessPosition(6, 1), new ChessPiece(TeamColor.WHITE, PieceType.ROOK));
        board.addPiece(new ChessPosition(6, 5), new ChessPiece(TeamColor.BLACK, PieceType.KNIGHT));
        board.addPiece(new ChessPosition(6, 8), new ChessPiece(TeamColor.BLACK, PieceType.KING));
        game.setBoard(board);
        assertTrue(game.isInCheck(TeamColor.WHITE));
    }

    @Test
    public void emptyBoardHasNoKingInCheck() {
        ChessGame game = new ChessGame();
        game.setBoard(new ChessBoard());
        assertFalse(game.isInCheck(TeamColor.WHITE));
        assertFalse(game.isInCheck(TeamColor.BLACK));
    }
}
