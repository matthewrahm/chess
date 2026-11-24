package client.gameplay;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import websocket.messages.ErrorMessage;
import websocket.messages.LoadGameMessage;
import websocket.messages.NotificationMessage;
import websocket.messages.ServerMessage;

/** Decodes the discriminator before reading the corresponding payload. */
final class ServerMessageReader {
    private final Gson gson = new Gson();

    ServerMessage read(String text) {
        try {
            JsonObject json = JsonParser.parseString(text).getAsJsonObject();
            String type = string(json, "serverMessageType");
            return switch (ServerMessage.ServerMessageType.valueOf(type)) {
                case LOAD_GAME -> readGame(json);
                case ERROR -> {
                    string(json, "errorMessage");
                    yield gson.fromJson(json, ErrorMessage.class);
                }
                case NOTIFICATION -> new NotificationMessage(string(json, "message"));
            };
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("The server sent an invalid game message.", exception);
        }
    }

    private LoadGameMessage readGame(JsonObject json) {
        var state = json.getAsJsonObject("game");
        string(state, "teamTurn");
        var squares = state.getAsJsonObject("board").getAsJsonArray("squares");
        if (squares == null || squares.size() != 8) {
            throw new IllegalArgumentException("Missing board squares");
        }
        for (var row : squares) {
            if (!row.isJsonArray() || row.getAsJsonArray().size() != 8) {
                throw new IllegalArgumentException("Invalid board row");
            }
        }
        var message = gson.fromJson(json, LoadGameMessage.class);
        var game = message.getGame();
        if (game.getBoard() == null || game.getTeamTurn() == null) {
            throw new IllegalArgumentException("Missing game state");
        }
        return message;
    }

    private String string(JsonObject json, String field) {
        var value = json.get(field);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("Missing message field");
        }
        return value.getAsString();
    }
}
