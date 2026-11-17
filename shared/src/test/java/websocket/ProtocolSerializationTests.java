package websocket;

import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPiece;
import chess.ChessPosition;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import websocket.commands.MakeMoveCommand;
import websocket.commands.UserGameCommand;
import websocket.messages.ErrorMessage;
import websocket.messages.LoadGameMessage;
import websocket.messages.NotificationMessage;
import websocket.messages.ServerMessage;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ProtocolSerializationTests {
    private final Gson gson = new Gson();

    @ParameterizedTest
    @EnumSource(value = UserGameCommand.CommandType.class, names = {"CONNECT", "LEAVE", "RESIGN"})
    void simpleCommandsUseExactCourseFields(UserGameCommand.CommandType type) {
        var command = new UserGameCommand(type, "session-token", 42);
        var json = json(command);
        assertEquals(Set.of("commandType", "authToken", "gameID"), json.keySet());
        assertEquals(type.name(), json.get("commandType").getAsString());
        assertEquals("session-token", json.get("authToken").getAsString());
        assertEquals(42, json.get("gameID").getAsInt());
        assertEquals(command, gson.fromJson(json, UserGameCommand.class));
    }

    @Test
    void ordinaryMoveIncludesPositionsAndRoundTripsWithoutPromotion() {
        var move = new ChessMove(new ChessPosition(2, 5), new ChessPosition(4, 5), null);
        var command = new MakeMoveCommand("token", 7, move);
        var json = json(command);
        assertEquals(Set.of("commandType", "authToken", "gameID", "move"), json.keySet());
        assertEquals("MAKE_MOVE", json.get("commandType").getAsString());
        assertEquals("token", json.get("authToken").getAsString());
        assertEquals(7, json.get("gameID").getAsInt());
        var restored = gson.fromJson(json, MakeMoveCommand.class);
        assertEquals(move, restored.getMove());
        assertEquals(UserGameCommand.CommandType.MAKE_MOVE, restored.getCommandType());
    }

    @ParameterizedTest
    @EnumSource(value = ChessPiece.PieceType.class, names = {"QUEEN", "ROOK", "BISHOP", "KNIGHT"})
    void promotionChoiceSurvivesTheWire(ChessPiece.PieceType promotion) {
        var move = new ChessMove(new ChessPosition(7, 1), new ChessPosition(8, 1), promotion);
        var command = new MakeMoveCommand("token", 7, move);
        var restored = gson.fromJson(gson.toJson(command), MakeMoveCommand.class);
        assertEquals(move, restored.getMove());
        assertEquals(promotion, restored.getMove().getPromotionPiece());
    }

    @Test
    void loadGameContainsActualPositionAndNoUnrelatedPayloads() throws Exception {
        var game = new ChessGame();
        game.makeMove(new ChessMove(new ChessPosition(2, 5), new ChessPosition(4, 5), null));
        var json = json(new LoadGameMessage(game));
        assertEquals(Set.of("serverMessageType", "game"), json.keySet());
        assertEquals("LOAD_GAME", json.get("serverMessageType").getAsString());
        var restored = gson.fromJson(json, LoadGameMessage.class);
        assertEquals(game, restored.getGame());
        assertEquals(ChessGame.TeamColor.BLACK, restored.getGame().getTeamTurn());
        assertNotSame(game, restored.getGame());
    }

    @Test
    void notificationPreservesNamesAndHasOnlyItsOwnPayload() {
        String text = "Player O'Brien \"棋手\" moved e2 to e4.";
        var json = json(new NotificationMessage(text));
        assertEquals(Set.of("serverMessageType", "message"), json.keySet());
        assertEquals("NOTIFICATION", json.get("serverMessageType").getAsString());
        assertEquals(text, gson.fromJson(json, NotificationMessage.class).getMessage());
    }

    @Test
    void errorsIncludeRequiredErrorTextAndNoOtherPayload() {
        var json = json(new ErrorMessage("It is not your turn"));
        assertEquals(Set.of("serverMessageType", "errorMessage"), json.keySet());
        assertEquals("ERROR", json.get("serverMessageType").getAsString());
        assertEquals("Error: It is not your turn", gson.fromJson(json, ErrorMessage.class).getErrorMessage());
    }

    @Test
    void baseTypeCanIdentifyWhichPayloadToDecode() {
        ServerMessage message = new NotificationMessage("Alice connected as white.");
        String wire = gson.toJson(message);
        var header = gson.fromJson(wire, ServerMessage.class);
        assertEquals(ServerMessage.ServerMessageType.NOTIFICATION, header.getServerMessageType());
        assertEquals("Alice connected as white.", gson.fromJson(wire, NotificationMessage.class).getMessage());
    }

    private JsonObject json(Object value) {
        return gson.toJsonTree(value).getAsJsonObject();
    }
}
