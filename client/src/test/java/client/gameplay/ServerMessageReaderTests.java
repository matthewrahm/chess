package client.gameplay;

import chess.ChessGame;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import websocket.messages.ErrorMessage;
import websocket.messages.LoadGameMessage;
import websocket.messages.NotificationMessage;

import static org.junit.jupiter.api.Assertions.*;

class ServerMessageReaderTests {
    private final ServerMessageReader reader = new ServerMessageReader();

    @Test
    void decodesTypedPayloadsWithoutAddingAnotherErrorPrefix() {
        var gson = new Gson();
        var game = new ChessGame();
        game.endGame();
        var loaded = assertInstanceOf(LoadGameMessage.class, reader.read(gson.toJson(new LoadGameMessage(game))));
        assertTrue(loaded.getGame().isGameOver());
        var error = assertInstanceOf(ErrorMessage.class, reader.read(gson.toJson(new ErrorMessage("rejected"))));
        assertEquals("Error: rejected", error.getErrorMessage());
        var notice = assertInstanceOf(NotificationMessage.class,
                reader.read(gson.toJson(new NotificationMessage("Alice moved"))));
        assertEquals("Alice moved", notice.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "[]", "{", "{}", "{\"serverMessageType\":\"OTHER\"}",
            "{\"serverMessageType\":\"LOAD_GAME\"}", "{\"serverMessageType\":\"ERROR\"}",
            "{\"serverMessageType\":\"LOAD_GAME\",\"game\":{}}",
            "{\"serverMessageType\":\"NOTIFICATION\",\"message\":4}",
            "{\"serverMessageType\":\"LOAD_GAME\",\"game\":null}"})
    void rejectsInvalidDiscriminatorsAndMissingOrWrongPayloads(String json) {
        assertThrows(IllegalArgumentException.class, () -> reader.read(json));
    }

    @Test
    void rejectsIncompleteBoardAndInvalidTurnRatherThanInventingInitialState() {
        var gson = new Gson();
        var message = gson.toJsonTree(new LoadGameMessage(new ChessGame())).getAsJsonObject();
        message.getAsJsonObject("game").getAsJsonObject("board").getAsJsonArray("squares").remove(0);
        assertThrows(IllegalArgumentException.class, () -> reader.read(message.toString()));
        var invalidTurn = gson.toJsonTree(new LoadGameMessage(new ChessGame())).getAsJsonObject();
        invalidTurn.getAsJsonObject("game").addProperty("teamTurn", "OTHER");
        assertThrows(IllegalArgumentException.class, () -> reader.read(invalidTurn.toString()));
    }
}
