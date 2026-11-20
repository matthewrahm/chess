package server.websocket;

import chess.ChessBoard;
import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPiece;
import chess.ChessPosition;
import dataaccess.MemoryDataAccess;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import websocket.commands.MakeMoveCommand;

import static chess.ChessGame.TeamColor.*;
import static chess.ChessPiece.PieceType.*;
import static org.junit.jupiter.api.Assertions.*;

class GameplayStatusTests extends GameSocketFixture {
    private final FailingStore store = new FailingStore();

    @Override
    protected MemoryDataAccess createDataAccess() {
        return store;
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void terminalMoveNotifiesEveryoneOnceAndRemainsEndedAfterReconnect(boolean mate) throws Exception {
        var move = prepareTerminalPosition(mate);
        String token = mate ? "black-token" : "white-token";
        String opponentToken = mate ? "white-token" : "black-token";
        String status = mate ? "Alice (white) is in checkmate. Game over."
                : "Bob (black) is in stalemate. Game over: draw.";
        int otherID = data.createGame("Other", new ChessGame());
        try (var mover = new SocketTestClient(port); var opponent = new SocketTestClient(port);
             var observer = new SocketTestClient(port); var other = new SocketTestClient(port)) {
            connect(mover, token, gameID);
            connect(opponent, opponentToken, gameID);
            notice(mover);
            connect(observer, "observer-token", gameID);
            notice(mover);
            notice(opponent);
            connect(other, "observer-token", otherID);
            mover.send(new MakeMoveCommand(token, gameID, move));
            assertTrue(load(mover).isGameOver());
            assertEquals(status, notice(mover));
            assertTrue(load(opponent).isGameOver());
            assertTrue(notice(opponent).contains("moved queen"));
            assertEquals(status, notice(opponent));
            assertTrue(load(observer).isGameOver());
            notice(observer);
            assertEquals(status, notice(observer));
            var ended = data.getGame(gameID);
            assertTrue(ended.game().isGameOver());
            assertEquals("Alice", ended.whiteUsername());
            assertEquals("Bob", ended.blackUsername());
            mover.send(new MakeMoveCommand(token, gameID, move));
            assertTrue(mover.error().contains("game is over"));
            assertEquals(ended, data.getGame(gameID));
            mover.expectSilence();
            opponent.expectSilence();
            observer.expectSilence();
            other.expectSilence();
            try (var reconnected = new SocketTestClient(port)) {
                assertEquals(ended.game(), connect(reconnected, token, gameID));
            }
        }
    }

    @Test
    void checkNotifiesMoverAndObserversButStillAllowsAnEscape() throws Exception {
        var game = emptyGame();
        place(game, WHITE, KING, 1, 1);
        place(game, WHITE, ROOK, 2, 5);
        place(game, BLACK, KING, 8, 5);
        savePosition(game);
        try (var white = new SocketTestClient(port); var black = new SocketTestClient(port)) {
            connect(white, "white-token", gameID);
            connect(black, "black-token", gameID);
            notice(white);
            white.send(new MakeMoveCommand("white-token", gameID, move(2, 5, 7, 5)));
            assertFalse(load(white).isGameOver());
            assertEquals("Bob (black) is in check.", notice(white));
            assertTrue(load(black).isInCheck(BLACK));
            notice(black);
            assertEquals("Bob (black) is in check.", notice(black));
            black.send(new MakeMoveCommand("black-token", gameID, move(8, 5, 8, 4)));
            assertFalse(load(black).isInCheck(BLACK));
            assertFalse(load(white).isGameOver());
            notice(white);
            white.expectSilence();
            black.expectSilence();
        }
    }

    @ParameterizedTest
    @EnumSource(value = ChessPiece.PieceType.class, names = {"QUEEN", "ROOK", "BISHOP", "KNIGHT"})
    void promotionPersistsSelectedPieceAndEvaluatesItsCheck(ChessPiece.PieceType promotion) throws Exception {
        var game = emptyGame();
        place(game, WHITE, KING, 1, 1);
        place(game, WHITE, PAWN, 7, 6);
        place(game, BLACK, KING, 8, 8);
        savePosition(game);
        try (var white = new SocketTestClient(port); var observer = new SocketTestClient(port)) {
            connect(white, "white-token", gameID);
            connect(observer, "observer-token", gameID);
            notice(white);
            var move = new ChessMove(new ChessPosition(7, 6), new ChessPosition(8, 6), promotion);
            white.send(new MakeMoveCommand("white-token", gameID, move));
            var loaded = load(white);
            assertEquals(promotion, loaded.getBoard().getPiece(new ChessPosition(8, 6)).getPieceType());
            assertEquals(loaded, load(observer));
            assertTrue(notice(observer).contains("promoted to " + promotion.name().toLowerCase(java.util.Locale.ROOT)));
            if (promotion == QUEEN || promotion == ROOK) {
                assertEquals("Bob (black) is in check.", notice(white));
                assertEquals("Bob (black) is in check.", notice(observer));
            }
            assertFalse(loaded.isGameOver());
            white.expectSilence();
            observer.expectSilence();
        }
    }

    @Test
    void failedTerminalSaveDoesNotEndGameOrSendCompletionNotice() throws Exception {
        var move = prepareTerminalPosition(true);
        try (var black = new SocketTestClient(port); var observer = new SocketTestClient(port)) {
            connect(black, "black-token", gameID);
            connect(observer, "observer-token", gameID);
            notice(black);
            var original = data.getGame(gameID);
            store.fail = true;
            black.send(new MakeMoveCommand("black-token", gameID, move));
            black.error();
            assertEquals(original, data.getGame(gameID));
            assertFalse(data.getGame(gameID).game().isGameOver());
            observer.expectSilence();
            black.expectSilence();
            store.fail = false;
            black.send(new MakeMoveCommand("black-token", gameID, move));
            assertTrue(load(black).isGameOver());
            assertTrue(notice(black).contains("checkmate"));
            assertTrue(load(observer).isGameOver());
            notice(observer);
            assertTrue(notice(observer).contains("checkmate"));
        }
    }

    private ChessMove prepareTerminalPosition(boolean mate) throws Exception {
        var game = new ChessGame();
        if (mate) {
            game.makeMove(move(2, 6, 3, 6));
            game.makeMove(move(7, 5, 5, 5));
            game.makeMove(move(2, 7, 4, 7));
        } else {
            game = emptyGame();
            place(game, WHITE, KING, 6, 3);
            place(game, WHITE, QUEEN, 6, 2);
            place(game, BLACK, KING, 8, 1);
        }
        savePosition(game);
        return mate ? move(8, 4, 4, 8) : move(6, 2, 7, 3);
    }

    private void savePosition(ChessGame game) throws Exception {
        join("white-token", gameID, "WHITE");
        join("black-token", gameID, "BLACK");
        var saved = data.getGame(gameID);
        data.updateGame(new model.GameData(gameID, saved.whiteUsername(), saved.blackUsername(), saved.gameName(), game));
    }

    private ChessGame emptyGame() {
        var game = new ChessGame();
        game.setBoard(new ChessBoard());
        return game;
    }

    private void place(ChessGame game, ChessGame.TeamColor color, ChessPiece.PieceType piece, int row, int col) {
        game.getBoard().addPiece(new ChessPosition(row, col), new ChessPiece(color, piece));
    }

    private ChessMove move(int row, int col, int endRow, int endCol) {
        return new ChessMove(new ChessPosition(row, col), new ChessPosition(endRow, endCol), null);
    }

    private ChessGame load(SocketTestClient client) throws Exception {
        var game = gson.fromJson(client.next("LOAD_GAME").get("game"), ChessGame.class);
        assertEquals(data.getGame(gameID).game(), game);
        return game;
    }
}
