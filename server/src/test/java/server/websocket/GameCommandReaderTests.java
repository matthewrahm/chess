package server.websocket;

import chess.ChessMove;
import chess.ChessPiece;
import chess.ChessPosition;
import com.google.gson.Gson;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import service.GameplayException;
import websocket.commands.MakeMoveCommand;

import static org.junit.jupiter.api.Assertions.*;

class GameCommandReaderTests {
    @ParameterizedTest
    @ValueSource(strings = {
            "null", "[]", "{}",
            "{\"startPosition\":null,\"endPosition\":{\"row\":4,\"col\":5}}",
            "{\"startPosition\":{\"row\":2,\"col\":5}}",
            "{\"startPosition\":{\"row\":\"2\",\"col\":5},\"endPosition\":{\"row\":4,\"col\":5}}",
            "{\"startPosition\":{\"row\":2.5,\"col\":5},\"endPosition\":{\"row\":4,\"col\":5}}",
            "{\"startPosition\":{\"row\":0,\"col\":5},\"endPosition\":{\"row\":4,\"col\":5}}",
            "{\"startPosition\":{\"row\":2,\"col\":5},\"endPosition\":{\"row\":9,\"col\":5}}",
            "{\"startPosition\":{\"row\":2,\"col\":5},\"endPosition\":{\"row\":4,\"col\":false}}"
    })
    void rejectsMalformedMoves(String move) {
        String json = "{\"commandType\":\"MAKE_MOVE\",\"authToken\":\"token\",\"gameID\":1,\"move\":" + move + "}";
        assertThrows(GameplayException.class, () -> new GameCommandReader().read(json));
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"KING\"", "\"PAWN\"", "\"queen\"", "17", "{}", "true"})
    void rejectsInvalidPromotionTypes(String promotion) {
        var json = new Gson().toJsonTree(command(null)).getAsJsonObject();
        json.getAsJsonObject("move").add("promotionPiece", new Gson().fromJson(promotion, com.google.gson.JsonElement.class));
        assertThrows(GameplayException.class, () -> new GameCommandReader().read(json.toString()));
    }

    @ParameterizedTest
    @EnumSource(value = ChessPiece.PieceType.class, names = {"QUEEN", "ROOK", "BISHOP", "KNIGHT"})
    void preservesPromotionAndExactPositionFields(ChessPiece.PieceType promotion) throws Exception {
        var expected = command(promotion);
        var actual = assertInstanceOf(MakeMoveCommand.class, new GameCommandReader().read(new Gson().toJson(expected)));
        assertEquals(expected.getMove(), actual.getMove());
        assertEquals(expected.getAuthToken(), actual.getAuthToken());
        assertEquals(expected.getGameID(), actual.getGameID());
    }

    private MakeMoveCommand command(ChessPiece.PieceType promotion) {
        return new MakeMoveCommand("token", 1, new ChessMove(new ChessPosition(7, 1), new ChessPosition(8, 1), promotion));
    }
}
