package server.websocket;

import chess.ChessGame;
import dataaccess.MemoryDataAccess;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import websocket.commands.UserGameCommand;

import static org.junit.jupiter.api.Assertions.*;
import static websocket.commands.UserGameCommand.CommandType.*;

class GameplayLeaveTests extends GameSocketFixture {
    private final FailingStore store = new FailingStore();

    @Override
    protected MemoryDataAccess createDataAccess() {
        return store;
    }

    @ParameterizedTest
    @ValueSource(strings = {"WHITE", "BLACK"})
    void playerLeaveReleasesOnlyTheirSeatAndAllowsReplacement(String color) throws Exception {
        join("white-token", gameID, "WHITE");
        join("black-token", gameID, "BLACK");
        boolean white = color.equals("WHITE");
        String token = white ? "white-token" : "black-token";
        String name = white ? "Alice" : "Bob";
        int otherID = data.createGame("Other", new ChessGame());
        try (var player = new SocketTestClient(port); var observer = new SocketTestClient(port);
             var other = new SocketTestClient(port)) {
            connect(player, token, gameID);
            connect(observer, "observer-token", gameID);
            notice(player);
            connect(other, "observer-token", otherID);
            var original = data.getGame(gameID);
            player.send(new UserGameCommand(LEAVE, token, gameID));
            assertEquals(name + " left the game.", notice(observer));
            var saved = data.getGame(gameID);
            assertEquals(white ? null : "Alice", saved.whiteUsername());
            assertEquals(white ? "Bob" : null, saved.blackUsername());
            assertEquals(original.game(), saved.game());
            assertEquals(original.gameName(), saved.gameName());
            join("observer-token", gameID, color);
            try (var replacement = new SocketTestClient(port)) {
                assertEquals(saved.game(), connect(replacement, "observer-token", gameID));
                assertTrue(notice(observer).contains("Charlie connected as"));
                player.expectSilence();
            }
            player.send(new UserGameCommand(LEAVE, token, gameID));
            assertTrue(player.error().contains("Connect to this game"));
            observer.expectSilence();
            other.expectSilence();
            // Leaving removes the subscription, not the usable transport.
            connect(player, token, otherID);
            assertTrue(notice(other).contains(name));
        }
    }

    @Test
    void observerLeaveDoesNotWriteStorageOrChangeSeats() throws Exception {
        join("white-token", gameID, "WHITE");
        try (var player = new SocketTestClient(port); var observer = new SocketTestClient(port)) {
            connect(player, "white-token", gameID);
            connect(observer, "observer-token", gameID);
            notice(player);
            var original = data.getGame(gameID);
            store.fail = true;
            observer.send(new UserGameCommand(LEAVE, "observer-token", gameID));
            assertEquals("Charlie left the game.", notice(player));
            assertEquals(original, data.getGame(gameID));
            observer.expectSilence();
            connect(observer, "observer-token", gameID);
            assertEquals("Charlie connected as an observer.", notice(player));
        }
    }

    @Test
    void failedLeaveKeepsSeatAndSubscriptionAndCanBeRetried() throws Exception {
        join("white-token", gameID, "WHITE");
        try (var player = new SocketTestClient(port); var observer = new SocketTestClient(port)) {
            connect(player, "white-token", gameID);
            connect(observer, "observer-token", gameID);
            notice(player);
            var original = data.getGame(gameID);
            store.fail = true;
            player.send(new UserGameCommand(LEAVE, "white-token", gameID));
            assertEquals("Error: Unable to process game command", player.error());
            assertEquals(original, data.getGame(gameID));
            observer.expectSilence();
            // The failed departure must still receive new game notifications.
            try (var newcomer = new SocketTestClient(port)) {
                connect(newcomer, "black-token", gameID);
                assertTrue(notice(player).contains("Bob"));
                notice(observer);
            }
            store.fail = false;
            player.send(new UserGameCommand(LEAVE, "white-token", gameID));
            assertEquals("Alice left the game.", notice(observer));
            assertNull(data.getGame(gameID).whiteUsername());
            player.expectSilence();
        }
    }

    @Test
    void invalidLeaveCannotReleaseSeatsOrNotifyOthers() throws Exception {
        join("white-token", gameID, "WHITE");
        try (var player = new SocketTestClient(port); var observer = new SocketTestClient(port)) {
            player.send(new UserGameCommand(LEAVE, "white-token", gameID));
            player.error();
            connect(player, "white-token", gameID);
            connect(observer, "observer-token", gameID);
            notice(player);
            var original = data.getGame(gameID);
            player.send(new UserGameCommand(LEAVE, "black-token", gameID));
            player.error();
            player.send(new UserGameCommand(LEAVE, "white-token", gameID + 100));
            player.error();
            data.deleteAuth("white-token");
            player.send(new UserGameCommand(LEAVE, "white-token", gameID));
            assertTrue(player.error().contains("Unauthorized"));
            assertEquals(original, data.getGame(gameID));
            observer.expectSilence();
        }
    }

    @Test
    void leavingFinishedGamePreservesItsEndState() throws Exception {
        join("white-token", gameID, "WHITE");
        var ended = data.getGame(gameID);
        ended.game().endGame();
        data.updateGame(ended);
        try (var player = new SocketTestClient(port); var observer = new SocketTestClient(port)) {
            connect(player, "white-token", gameID);
            connect(observer, "observer-token", gameID);
            notice(player);
            player.send(new UserGameCommand(LEAVE, "white-token", gameID));
            notice(observer);
            assertNull(data.getGame(gameID).whiteUsername());
            assertTrue(data.getGame(gameID).game().isGameOver());
        }
    }
}
