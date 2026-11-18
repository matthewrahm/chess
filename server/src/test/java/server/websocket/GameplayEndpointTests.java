package server.websocket;

import chess.ChessGame;
import dataaccess.MemoryDataAccess;
import model.AuthData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import server.Server;
import websocket.commands.UserGameCommand;

import static org.junit.jupiter.api.Assertions.*;
import static websocket.commands.UserGameCommand.CommandType.*;

class GameplayEndpointTests {
    private MemoryDataAccess data;
    private Server server;
    private int port;
    private int gameID;

    @BeforeEach
    void start() throws Exception {
        data = new MemoryDataAccess();
        data.createAuth(new AuthData("valid-token", "Alice"));
        gameID = data.createGame("Practice", new ChessGame());
        server = new Server(data);
        port = server.run(0);
    }

    @AfterEach
    void stop() {
        server.stop();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "null", "[]", "42", "{}", "{bad}", "{} {}", "{\"commandType\":",
            "{commandType:'CONNECT',authToken:'valid-token',gameID:1}"})
    void malformedInputProducesOneErrorAndSocketRemainsUsable(String input) throws Exception {
        try (var client = new SocketTestClient(port)) {
            client.sendText(input);
            client.error();
            client.send(new UserGameCommand(CONNECT, "bad-token", gameID));
            assertTrue(client.error().contains("Unauthorized"));
            client.expectSilence();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "\"1\"", "1.5", "2147483648", "0", "-1", "true", "[]", "{}"})
    void invalidGameIdTypesAreRejectedWithoutGsonCoercion(String id) throws Exception {
        try (var client = new SocketTestClient(port)) {
            client.sendText("{\"commandType\":\"CONNECT\",\"authToken\":\"valid-token\",\"gameID\":" + id + "}");
            client.error();
            client.expectSilence();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"commandType\":\"UNKNOWN\",\"authToken\":\"valid-token\",\"gameID\":1}",
            "{\"commandType\":true,\"authToken\":\"valid-token\",\"gameID\":1}",
            "{\"commandType\":\"CONNECT\",\"authToken\":123,\"gameID\":1}",
            "{\"commandType\":\"CONNECT\",\"authToken\":null,\"gameID\":1}"})
    void commandTypeAndTokenMustBeStrings(String text) throws Exception {
        try (var client = new SocketTestClient(port)) {
            client.sendText(text);
            client.error();
        }
    }

    @Test
    void invalidAuthorizationAndMissingGameDoNotMutateOrNotifyOthers() throws Exception {
        var before = data.getGame(gameID);
        try (var client = new SocketTestClient(port); var other = new SocketTestClient(port)) {
            for (String token : new String[]{"", " ", "missing"}) {
                client.send(new UserGameCommand(CONNECT, token, gameID));
                assertTrue(client.error().contains("Unauthorized"));
            }
            client.send(new UserGameCommand(CONNECT, "valid-token", gameID + 1));
            assertTrue(client.error().contains("Game does not exist"));
            other.expectSilence();
            assertEquals(before, data.getGame(gameID));
        }
    }

    @Test
    void binaryMessageReturnsErrorWithoutDisconnecting() throws Exception {
        try (var client = new SocketTestClient(port)) {
            client.sendBinary();
            assertTrue(client.error().contains("JSON text"));
            client.send(new UserGameCommand(CONNECT, "missing", gameID));
            client.error();
        }
    }

    @Test
    void authorizesEveryCommandRatherThanTrustingEarlierToken() throws Exception {
        try (var client = new SocketTestClient(port)) {
            client.send(new UserGameCommand(RESIGN, "valid-token", gameID));
            client.error();
            data.deleteAuth("valid-token");
            client.send(new UserGameCommand(RESIGN, "valid-token", gameID));
            assertTrue(client.error().contains("Unauthorized"));
        }
    }

    @Test
    void multipleQueuedErrorsKeepCommandOrder() throws Exception {
        try (var client = new SocketTestClient(port)) {
            for (int i = 0; i < 8; i++) {
                client.send(new UserGameCommand(CONNECT, "missing", gameID));
                client.send(new UserGameCommand(CONNECT, "valid-token", gameID + 1));
            }
            for (int i = 0; i < 8; i++) {
                assertTrue(client.error().contains("Unauthorized"));
                assertTrue(client.error().contains("Game does not exist"));
            }
            client.expectSilence();
        }
    }

    @Test
    void closingOneSocketDoesNotStopAnother() throws Exception {
        try (var other = new SocketTestClient(port)) {
            try (var client = new SocketTestClient(port)) {
                client.send(new UserGameCommand(CONNECT, "missing", gameID));
                client.error();
            }
            other.send(new UserGameCommand(CONNECT, "missing", gameID));
            other.error();
        }
    }
}
