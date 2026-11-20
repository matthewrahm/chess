package dataaccess.sql;

import chess.ChessMove;
import chess.ChessPosition;
import org.junit.jupiter.api.Test;
import service.GameplayService;
import service.GameplayException;
import websocket.commands.MakeMoveCommand;
import websocket.commands.UserGameCommand;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static websocket.commands.UserGameCommand.CommandType.*;

class SqlGameplayDepartureTests extends TestSqlServerFixture {
    @Test
    void resignationSurvivesServerRestartAndRejectsLaterMovesAndResignation() throws Exception {
        String white = http.register("white");
        String black = http.register("black");
        int id = http.createGame(white, "Resigned game");
        http.call("PUT", "/game", white, Map.of("gameID", id, "playerColor", "WHITE"), 200);
        http.call("PUT", "/game", black, Map.of("gameID", id, "playerColor", "BLACK"), 200);
        var result = new GameplayService(new SqlDataAccess()).resign(new UserGameCommand(RESIGN, black, id), "black");

        http.restart();

        var data = new SqlDataAccess();
        var restored = data.getGame(id);
        assertEquals(result.game(), restored);
        assertTrue(restored.game().isGameOver());
        assertEquals("white", restored.whiteUsername());
        assertEquals("black", restored.blackUsername());
        var service = new GameplayService(data);
        var move = new ChessMove(new ChessPosition(2, 5), new ChessPosition(4, 5), null);
        assertThrows(GameplayException.class, () -> service.makeMove(new MakeMoveCommand(white, id, move), "white"));
        assertThrows(GameplayException.class, () -> service.resign(new UserGameCommand(RESIGN, white, id), "white"));
        assertEquals(restored, new SqlDataAccess().getGame(id));
    }

    @Test
    void releasedSeatAndBoardSurviveServerRestartAndCanBeRejoined() throws Exception {
        String white = http.register("white");
        String black = http.register("black");
        int id = http.createGame(white, "Saved game");
        http.call("PUT", "/game", white, Map.of("gameID", id, "playerColor", "WHITE"), 200);
        http.call("PUT", "/game", black, Map.of("gameID", id, "playerColor", "BLACK"), 200);
        var data = new SqlDataAccess();
        var original = data.getGame(id);
        original.game().makeMove(new ChessMove(new ChessPosition(2, 5), new ChessPosition(4, 5), null));
        data.updateGame(original);
        new GameplayService(data).leave(new UserGameCommand(LEAVE, white, id), "white");

        http.restart();

        var restored = new SqlDataAccess().getGame(id);
        assertNull(restored.whiteUsername());
        assertEquals("black", restored.blackUsername());
        assertEquals(original.game(), restored.game());
        String replacement = http.register("replacement");
        http.call("PUT", "/game", replacement, Map.of("gameID", id, "playerColor", "WHITE"), 200);
        assertEquals("replacement", new SqlDataAccess().getGame(id).whiteUsername());
    }
}
