package chess;

import chess.ChessGame.TeamColor;
import chess.ChessPiece.PieceType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class KingCaptureTests {
    @Test
    public void checkingPiecesCannotCaptureTheOpposingKing() {
        for (TeamColor color : TeamColor.values()) {
            for (PieceType type : PieceType.values()) {
                ChessGame game = setup(color, type);
                ChessPosition start = attackerPosition(color, type);
                ChessPosition king = new ChessPosition(4, 4);
                ChessMove capture = new ChessMove(start, king, null);
                TeamColor opponent = color == TeamColor.WHITE ? TeamColor.BLACK : TeamColor.WHITE;

                assertTrue(game.getBoard().getPiece(start).pieceMoves(game.getBoard(), start).contains(capture));
                assertTrue(game.isInCheck(opponent));
                assertFalse(game.validMoves(start).contains(capture), type + " must not capture a king");
            }
        }
    }

    @Test
    public void rejectedKingCapturePreservesTheGame() {
        for (TeamColor color : TeamColor.values()) {
            for (PieceType type : PieceType.values()) {
                ChessGame game = setup(color, type);
                ChessBoard board = game.getBoard();
                ChessBoard snapshot = board.copy();
                int hash = game.hashCode();
                ChessMove capture = new ChessMove(attackerPosition(color, type), new ChessPosition(4, 4), null);

                assertThrows(InvalidMoveException.class, () -> game.makeMove(capture));
                assertSame(board, game.getBoard());
                assertEquals(snapshot, board);
                assertEquals(color, game.getTeamTurn());
                assertEquals(hash, game.hashCode());
            }
        }
    }

    private ChessPosition attackerPosition(TeamColor color, PieceType type) {
        return switch (type) {
            case KING -> new ChessPosition(3, 3);
            case QUEEN, ROOK -> new ChessPosition(4, 1);
            case BISHOP -> new ChessPosition(1, 1);
            case KNIGHT -> new ChessPosition(2, 3);
            case PAWN -> new ChessPosition(color == TeamColor.WHITE ? 3 : 5, 3);
        };
    }

    private ChessGame setup(TeamColor color, PieceType type) {
        ChessGame game = new ChessGame();
        ChessBoard board = new ChessBoard();
        TeamColor opponent = color == TeamColor.WHITE ? TeamColor.BLACK : TeamColor.WHITE;
        board.addPiece(new ChessPosition(4, 4), new ChessPiece(opponent, PieceType.KING));
        board.addPiece(attackerPosition(color, type), new ChessPiece(color, type));
        if (type != PieceType.KING) {
            board.addPiece(new ChessPosition(8, 8), new ChessPiece(color, PieceType.KING));
        }
        game.setBoard(board);
        game.setTeamTurn(color);
        return game;
    }
}
