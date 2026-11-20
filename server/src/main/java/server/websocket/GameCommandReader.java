package server.websocket;

import com.google.gson.Gson;
import chess.ChessMove;
import chess.ChessPiece;
import chess.ChessPosition;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import service.GameplayException;
import websocket.commands.UserGameCommand;
import websocket.commands.MakeMoveCommand;

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
            int gameID = integer(object, "gameID");
            if (gameID <= 0) {
                throw new GameplayException("Invalid game ID");
            }
            return type == UserGameCommand.CommandType.MAKE_MOVE
                    ? new MakeMoveCommand(token, gameID, move(object))
                    : new UserGameCommand(type, token, gameID);
        } catch (IOException | JsonParseException | IllegalArgumentException | ArithmeticException | IllegalStateException e) {
            throw new GameplayException("Invalid game command");
        }
    }

    private ChessMove move(JsonObject command) throws GameplayException {
        var move = object(command, "move");
        ChessPiece.PieceType promotion = null;
        if (move.has("promotionPiece") && !move.get("promotionPiece").isJsonNull()) {
            promotion = ChessPiece.PieceType.valueOf(string(move, "promotionPiece"));
            if (promotion == ChessPiece.PieceType.KING || promotion == ChessPiece.PieceType.PAWN) {
                throw new GameplayException("Invalid promotion piece");
            }
        }
        return new ChessMove(position(move, "startPosition"), position(move, "endPosition"), promotion);
    }

    private ChessPosition position(JsonObject move, String field) throws GameplayException {
        var position = object(move, field);
        int row = integer(position, "row");
        int col = integer(position, "col");
        if (row < 1 || row > 8 || col < 1 || col > 8) {
            throw new GameplayException("Move must use positions on the board");
        }
        return new ChessPosition(row, col);
    }

    private JsonObject object(JsonObject parent, String field) throws GameplayException {
        var value = parent.get(field);
        if (value == null || !value.isJsonObject()) {
            throw new GameplayException("Expected object field " + field);
        }
        return value.getAsJsonObject();
    }

    private int integer(JsonObject object, String field) throws GameplayException {
        var value = object.get(field);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new GameplayException("Expected integer field " + field);
        }
        return value.getAsBigDecimal().intValueExact();
    }

    private String string(JsonObject object, String field) throws GameplayException {
        var value = object.get(field);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new GameplayException("Expected string field " + field);
        }
        return value.getAsString();
    }
}
