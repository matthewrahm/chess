package service;

import chess.ChessGame;
import dataaccess.MemoryDataAccess;
import model.AuthData;
import model.GameData;
import org.junit.jupiter.api.Test;
import websocket.commands.UserGameCommand;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static websocket.commands.UserGameCommand.CommandType.RESIGN;

class GameplayResignConcurrencyTests {
    @Test
    void competingResignationsOnlyCommitOnce() throws Exception {
        var data = new MemoryDataAccess();
        data.createAuth(new AuthData("Alice", "Alice"));
        data.createAuth(new AuthData("Bob", "Bob"));
        int id = data.createGame("Race", new ChessGame());
        data.updateGame(new GameData(id, "Alice", "Bob", "Race", new ChessGame()));
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (var threads = Executors.newFixedThreadPool(2)) {
            var first = threads.submit(attempt(data, id, "Alice", ready, start));
            var second = threads.submit(attempt(data, id, "Bob", ready, start));
            assertTrue(ready.await(3, TimeUnit.SECONDS));
            start.countDown();
            assertNotEquals(first.get(3, TimeUnit.SECONDS), second.get(3, TimeUnit.SECONDS));
            var saved = data.getGame(id);
            assertTrue(saved.game().isGameOver());
            assertEquals("Alice", saved.whiteUsername());
            assertEquals("Bob", saved.blackUsername());
        } finally {
            start.countDown();
        }
    }

    private Callable<Boolean> attempt(MemoryDataAccess data, int id, String username,
                                      CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            assertTrue(start.await(3, TimeUnit.SECONDS));
            try {
                new GameplayService(data).resign(new UserGameCommand(RESIGN, username, id), username);
                return true;
            } catch (GameplayException exception) {
                return false;
            }
        };
    }
}
