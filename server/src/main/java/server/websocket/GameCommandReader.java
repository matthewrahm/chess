package server.websocket;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import service.GameplayException;
import websocket.commands.UserGameCommand;

import java.io.IOException;
import java.io.StringReader;

/** Rejects malformed JSON and coerced field types before dispatching commands. */
final class GameCommandReader {
    private static final Gson GSON = new Gson();

    UserGameCommand read(String text) throws GameplayException {
        try (var reader = new JsonReader(new StringReader(text))) {
            reader.setLenient(false);
            if (reader.peek() != JsonToken.BEGIN_OBJECT) {
                throw new GameplayException("Expected a JSON command object");
            }
            reader.skipValue();
            if (reader.peek() != JsonToken.END_DOCUMENT) {
                throw new GameplayException("Expected one JSON command");
            }
            JsonObject object = GSON.fromJson(text, JsonObject.class);
            var type = UserGameCommand.CommandType.valueOf(string(object, "commandType"));
            String token = string(object, "authToken");
            var id = object.get("gameID");
            if (id == null || !id.isJsonPrimitive() || !id.getAsJsonPrimitive().isNumber()) {
                throw new GameplayException("Expected a numeric game ID");
            }
            int gameID = id.getAsBigDecimal().intValueExact();
            if (gameID <= 0) {
                throw new GameplayException("Invalid game ID");
            }
            return new UserGameCommand(type, token, gameID);
        } catch (IOException | JsonParseException | IllegalArgumentException | ArithmeticException | IllegalStateException e) {
            throw new GameplayException("Invalid game command");
        }
    }

    private String string(JsonObject object, String field) throws GameplayException {
        var value = object.get(field);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new GameplayException("Expected string field " + field);
        }
        return value.getAsString();
    }
}
