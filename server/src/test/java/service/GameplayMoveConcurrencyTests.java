package service;

import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPosition;
import dataaccess.MemoryDataAccess;
import model.AuthData;
import model.GameData;
import org.junit.jupiter.api.Test;
import websocket.commands.MakeMoveCommand;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class GameplayMoveConcurrencyTests {
    @Test
    void competingMovesFromSeparateServicesCannotBothCommit() throws Exception {
        var data = new MemoryDataAccess();
        data.createAuth(new AuthData("token", "Alice"));
        int id = data.createGame("Race", new ChessGame());
        data.updateGame(new GameData(id, "Alice", "Bob", "Race", new ChessGame()));
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (var threads = Executors.newFixedThreadPool(2)) {
            var first = threads.submit(attempt(data, id, 4, ready, start));
            var second = threads.submit(attempt(data, id, 5, ready, start));
            assertTrue(ready.await(3, TimeUnit.SECONDS));
            start.countDown();
            boolean firstWon = first.get(3, TimeUnit.SECONDS);
            boolean secondWon = second.get(3, TimeUnit.SECONDS);
            assertNotEquals(firstWon, secondWon);
            var expected = new ChessGame();
            int column = firstWon ? 4 : 5;
            expected.makeMove(move(column));
            assertEquals(expected, data.getGame(id).game());
            assertEquals("Bob", data.getGame(id).blackUsername());
        } finally {
            start.countDown();
        }
    }

    private Callable<Boolean> attempt(MemoryDataAccess data, int id, int column,
                                      CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            assertTrue(start.await(3, TimeUnit.SECONDS));
            try {
                new GameplayService(data).makeMove(new MakeMoveCommand("token", id, move(column)), "Alice");
                return true;
            } catch (GameplayException exception) {
                return false;
            }
        };
    }

    private ChessMove move(int column) {
        return new ChessMove(new ChessPosition(2, column), new ChessPosition(4, column), null);
    }
}
