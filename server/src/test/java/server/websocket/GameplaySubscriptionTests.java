package server.websocket;

import chess.ChessGame;
import org.junit.jupiter.api.Test;
import websocket.commands.UserGameCommand;

import static org.junit.jupiter.api.Assertions.*;
import static websocket.commands.UserGameCommand.CommandType.*;

class GameplaySubscriptionTests extends GameSocketFixture {
    @Test
    void connectionsInOtherGamesAndUnsubscribedSocketsReceiveNothing() throws Exception {
        int otherID = data.createGame("Other", new ChessGame());
        try (var first = new SocketTestClient(port); var second = new SocketTestClient(port);
             var other = new SocketTestClient(port); var idle = new SocketTestClient(port)) {
            connect(first, "white-token", gameID);
            connect(other, "white-token", otherID);
            connect(second, "black-token", gameID);
            assertEquals("Bob connected as an observer.", notice(first));
            other.expectSilence();
            idle.expectSilence();
            first.expectSilence();
            second.expectSilence();
        }
    }

    @Test
    void subscribedSocketCannotChangeIdentityOrGame() throws Exception {
        int otherID = data.createGame("Other", new ChessGame());
        try (var client = new SocketTestClient(port); var witness = new SocketTestClient(port)) {
            connect(client, "white-token", gameID);
            connect(witness, "observer-token", gameID);
            notice(client);
            client.send(new UserGameCommand(CONNECT, "black-token", gameID));
            assertTrue(client.error().contains("Leave the current game"));
            client.send(new UserGameCommand(CONNECT, "white-token", otherID));
            client.error();
            witness.expectSilence();
            connect(client, "white-token", gameID);
            witness.expectSilence();
        }
    }

    @Test
    void actionsRequireMatchingSubscriptionEvenWithValidToken() throws Exception {
        int otherID = data.createGame("Other", new ChessGame());
        try (var client = new SocketTestClient(port)) {
            client.send(new UserGameCommand(RESIGN, "white-token", gameID));
            assertTrue(client.error().contains("Connect to this game"));
            connect(client, "white-token", gameID);
            client.send(new UserGameCommand(RESIGN, "black-token", gameID));
            assertTrue(client.error().contains("Connect to this game"));
            client.send(new UserGameCommand(RESIGN, "white-token", otherID));
            assertTrue(client.error().contains("Connect to this game"));
            assertFalse(data.getGame(gameID).game().isGameOver());
            assertFalse(data.getGame(otherID).game().isGameOver());
        }
    }

    @Test
    void revokedTokenCannotReloadGame() throws Exception {
        try (var client = new SocketTestClient(port)) {
            connect(client, "white-token", gameID);
            data.deleteAuth("white-token");
            client.send(new UserGameCommand(CONNECT, "white-token", gameID));
            assertTrue(client.error().contains("Unauthorized"));
            client.expectSilence();
        }
    }

    @Test
    void failedConnectDoesNotSubscribeOrPreventLaterValidConnect() throws Exception {
        try (var client = new SocketTestClient(port); var other = new SocketTestClient(port)) {
            client.send(new UserGameCommand(CONNECT, "white-token", gameID + 100));
            client.error();
            connect(other, "black-token", gameID);
            client.expectSilence();
            connect(client, "white-token", gameID);
            assertEquals("Alice connected as an observer.", notice(other));
        }
    }

    @Test
    void separateSessionsForSamePlayerEachReceiveOneNotification() throws Exception {
        join("white-token", gameID, "WHITE");
        try (var first = new SocketTestClient(port); var second = new SocketTestClient(port);
             var observer = new SocketTestClient(port)) {
            connect(first, "white-token", gameID);
            connect(second, "white-token", gameID);
            assertEquals("Alice connected as white.", notice(first));
            connect(observer, "observer-token", gameID);
            assertEquals("Charlie connected as an observer.", notice(first));
            assertEquals("Charlie connected as an observer.", notice(second));
            first.expectSilence();
            second.expectSilence();
        }
    }
}
