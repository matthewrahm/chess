package dataaccess.sql;

import chess.ChessBoard;
import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPiece;
import chess.ChessPosition;
import model.GameData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static chess.ChessGame.TeamColor.*;
import static chess.ChessPiece.PieceType.*;
import static org.junit.jupiter.api.Assertions.*;

class SqlGameStateTests {
    private DatabaseTestSupport database;

    @BeforeEach
    void setUp() throws Exception {
        database = new DatabaseTestSupport();
        DatabaseSchema.initialize();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (database != null) {
            database.close();
        }
    }

    @Test
    void checkmateAndTurnSurviveReload() throws Exception {
        var game = new ChessGame();
        game.makeMove(move(2, 6, 3, 6));
        game.makeMove(move(7, 5, 5, 5));
        game.makeMove(move(2, 7, 4, 7));
        game.makeMove(move(8, 4, 4, 8));
        var restored = roundTrip(game);
        assertEquals(WHITE, restored.getTeamTurn());
        assertTrue(restored.isInCheck(WHITE));
        assertTrue(restored.isInCheckmate(WHITE));
        assertFalse(restored.isInStalemate(WHITE));
    }

    @Test
    void enPassantRemainsAvailableAfterReload() throws Exception {
        var game = new ChessGame();
        game.makeMove(move(2, 5, 4, 5));
        game.makeMove(move(7, 1, 6, 1));
        game.makeMove(move(4, 5, 5, 5));
        game.makeMove(move(7, 4, 5, 4));
        var restored = roundTrip(game);
        var capture = move(5, 5, 6, 4);
        assertTrue(restored.validMoves(new ChessPosition(5, 5)).contains(capture));
        restored.makeMove(capture);
        assertNull(restored.getBoard().getPiece(new ChessPosition(5, 4)));
        assertEquals(new ChessPiece(WHITE, PAWN), restored.getBoard().getPiece(new ChessPosition(6, 4)));
    }

    @Test
    void unmovedKingAndRookCanCastleAfterReload() throws Exception {
        var game = castlingPosition();
        var restored = roundTrip(game);
        var castle = move(1, 5, 1, 7);
        assertTrue(restored.validMoves(new ChessPosition(1, 5)).contains(castle));
        restored.makeMove(castle);
        assertEquals(new ChessPiece(WHITE, KING), restored.getBoard().getPiece(new ChessPosition(1, 7)));
        assertEquals(new ChessPiece(WHITE, ROOK), restored.getBoard().getPiece(new ChessPosition(1, 6)));
        assertNull(restored.getBoard().getPiece(new ChessPosition(1, 8)));
    }

    @Test
    void rookReturningToItsStartingSquareDoesNotRegainCastlingRights() throws Exception {
        var game = castlingPosition();
        game.makeMove(move(1, 8, 2, 8));
        game.makeMove(move(8, 5, 8, 4));
        game.makeMove(move(2, 8, 1, 8));
        game.makeMove(move(8, 4, 8, 5));
        var restored = roundTrip(game);
        var kingMoves = restored.validMoves(new ChessPosition(1, 5));
        assertFalse(kingMoves.contains(move(1, 5, 1, 7)));
        assertTrue(kingMoves.contains(move(1, 5, 1, 3)));
    }

    @Test
    void promotedPieceRetainsItsTypeAndColor() throws Exception {
        var board = new ChessBoard();
        board.addPiece(new ChessPosition(1, 1), new ChessPiece(WHITE, KING));
        board.addPiece(new ChessPosition(8, 8), new ChessPiece(BLACK, KING));
        board.addPiece(new ChessPosition(7, 2), new ChessPiece(WHITE, PAWN));
        var game = new ChessGame();
        game.setBoard(board);
        game.makeMove(new ChessMove(new ChessPosition(7, 2), new ChessPosition(8, 2), KNIGHT));
        var restored = roundTrip(game);
        assertEquals(new ChessPiece(WHITE, KNIGHT), restored.getBoard().getPiece(new ChessPosition(8, 2)));
        assertNull(restored.getBoard().getPiece(new ChessPosition(7, 2)));
        assertEquals(BLACK, restored.getTeamTurn());
    }

    @Test
    void customStalematePositionSurvivesReload() throws Exception {
        var board = new ChessBoard();
        board.addPiece(new ChessPosition(8, 1), new ChessPiece(BLACK, KING));
        board.addPiece(new ChessPosition(6, 3), new ChessPiece(WHITE, KING));
        board.addPiece(new ChessPosition(6, 2), new ChessPiece(WHITE, QUEEN));
        var game = new ChessGame();
        game.setBoard(board);
        game.setTeamTurn(BLACK);
        var restored = roundTrip(game);
        assertTrue(restored.isInStalemate(BLACK));
        assertFalse(restored.isInCheck(BLACK));
        assertFalse(restored.isInCheckmate(BLACK));
    }

    private ChessGame roundTrip(ChessGame game) throws Exception {
        var games = new SqlGameDao();
        int id = games.createGame("State test", new ChessGame());
        games.updateGame(new GameData(id, "white", "black", "State test", game));
        var restored = new SqlGameDao().getGame(id).game();
        assertEquals(game, restored);
        assertNotSame(game, restored);
        return restored;
    }

    private ChessGame castlingPosition() {
        var board = new ChessBoard();
        board.addPiece(new ChessPosition(1, 5), new ChessPiece(WHITE, KING));
        board.addPiece(new ChessPosition(1, 1), new ChessPiece(WHITE, ROOK));
        board.addPiece(new ChessPosition(1, 8), new ChessPiece(WHITE, ROOK));
        board.addPiece(new ChessPosition(8, 5), new ChessPiece(BLACK, KING));
        var game = new ChessGame();
        game.setBoard(board);
        return game;
    }

    private ChessMove move(int fromRow, int fromColumn, int toRow, int toColumn) {
        return new ChessMove(new ChessPosition(fromRow, fromColumn), new ChessPosition(toRow, toColumn), null);
    }
}
