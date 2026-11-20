package dataaccess.sql;

import chess.ChessGame;
import chess.ChessBoard;
import chess.ChessPiece;
import chess.ChessMove;
import chess.ChessPosition;
import chess.InvalidMoveException;
import com.google.gson.Gson;
import dataaccess.DataAccessException;
import dataaccess.DatabaseManager;
import model.GameData;
import model.AuthData;
import service.GameplayException;
import service.GameplayService;
import websocket.commands.MakeMoveCommand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class SqlGameLifecycleTests {
    private TestDatabaseSupport database;
    private SqlGameDao games;
    private int id;

    @BeforeEach
    void setUp() throws Exception {
        database = new TestDatabaseSupport();
        DatabaseSchema.initialize();
        games = new SqlGameDao();
        id = games.createGame("Practice", new ChessGame());
    }

    @AfterEach
    void tearDown() throws Exception {
        if (database != null) {
            database.close();
        }
    }

    @Test
    void finishedGameAndSeatsSurviveNewStorageInstance() throws Exception {
        var game = games.getGame(id).game();
        game.makeMove(move());
        game.endGame();
        var ended = new GameData(id, "white", "black", "Practice", game);
        games.updateGame(ended);
        var restored = new SqlDataAccess().getGame(id);
        assertEquals(ended, restored);
        assertTrue(restored.game().isGameOver());
        assertEquals("white", restored.whiteUsername());
        assertEquals("black", restored.blackUsername());
        assertThrows(InvalidMoveException.class, () -> restored.game().makeMove(
                new ChessMove(new ChessPosition(7, 5), new ChessPosition(5, 5), null)));
        assertTrue(new SqlGameDao().listGames().iterator().next().game().isGameOver());
    }

    @Test
    void legacyDatabaseRowLoadsAsPlayableWithoutSchemaMigration() throws Exception {
        var json = new Gson().toJsonTree(new ChessGame()).getAsJsonObject();
        json.remove("gameOver");
        try (var connection = DatabaseManager.getConnection();
             var statement = connection.prepareStatement("UPDATE games SET game_json = ? WHERE game_id = ?")) {
            statement.setString(1, json.toString());
            statement.setInt(2, id);
            assertEquals(1, statement.executeUpdate());
        }
        var restored = new SqlGameDao().getGame(id);
        assertFalse(restored.game().isGameOver());
        restored.game().makeMove(move());
        games.updateGame(restored);
        assertEquals(ChessGame.TeamColor.BLACK, games.getGame(id).game().getTeamTurn());
    }

    @Test
    void failedWriteDoesNotPersistMovesOrEndState() throws Exception {
        var original = games.getGame(id);
        var candidate = original.copy();
        candidate.game().makeMove(move());
        candidate.game().endGame();
        database.configureUnavailableServer();
        assertThrows(DataAccessException.class, () -> games.updateGame(candidate));
        database.configureDatabase(database.databaseName());
        assertEquals(original, new SqlGameDao().getGame(id));
        assertFalse(games.getGame(id).game().isGameOver());
    }

    private ChessMove move() {
        return new ChessMove(new ChessPosition(2, 5), new ChessPosition(4, 5), null);
    }

    @Test
    void acceptedMoveAndTurnSurviveNewStorageAndServiceInstances() throws Exception {
        var data = new SqlDataAccess();
        data.createAuth(new AuthData("white-token", "Alice"));
        data.createAuth(new AuthData("black-token", "Bob"));
        int id = data.createGame("Saved match", new ChessGame());
        data.updateGame(new GameData(id, "Alice", "Bob", "Saved match", new ChessGame()));
        var result = new GameplayService(data).makeMove(command(id, "white-token", 2, 4), "Alice");
        var restarted = new SqlDataAccess();
        assertEquals(result.game(), restarted.getGame(id));
        assertEquals(ChessGame.TeamColor.BLACK, restarted.getGame(id).game().getTeamTurn());
        var service = new GameplayService(restarted);
        assertThrows(GameplayException.class, () -> service.makeMove(command(id, "white-token", 2, 3), "Alice"));
        assertEquals(result.game(), restarted.getGame(id));
        service.makeMove(command(id, "black-token", 7, 5), "Bob");
        var saved = new SqlDataAccess().getGame(id);
        assertEquals(ChessGame.TeamColor.WHITE, saved.game().getTeamTurn());
        assertNotNull(saved.game().getBoard().getPiece(new ChessPosition(4, 5)));
        assertNotNull(saved.game().getBoard().getPiece(new ChessPosition(5, 5)));
        assertEquals("Alice", saved.whiteUsername());
        assertEquals("Bob", saved.blackUsername());
    }

    private MakeMoveCommand command(int id, String token, int fromRow, int toRow) {
        return new MakeMoveCommand(token, id, new ChessMove(new ChessPosition(fromRow, 5), new ChessPosition(toRow, 5), null));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void terminalMovePersistsCompletionAndRejectsMovesAfterReload(boolean mate) throws Exception {
        var data = new SqlDataAccess();
        data.createAuth(new AuthData("white-token", "Alice"));
        data.createAuth(new AuthData("black-token", "Bob"));
        var game = new ChessGame();
        ChessMove finalMove;
        String token;
        String username;
        if (mate) {
            game.makeMove(new ChessMove(new ChessPosition(2, 6), new ChessPosition(3, 6), null));
            game.makeMove(new ChessMove(new ChessPosition(7, 5), new ChessPosition(5, 5), null));
            game.makeMove(new ChessMove(new ChessPosition(2, 7), new ChessPosition(4, 7), null));
            finalMove = new ChessMove(new ChessPosition(8, 4), new ChessPosition(4, 8), null);
            token = "black-token";
            username = "Bob";
        } else {
            var board = new ChessBoard();
            board.addPiece(new ChessPosition(6, 3), new ChessPiece(ChessGame.TeamColor.WHITE, ChessPiece.PieceType.KING));
            board.addPiece(new ChessPosition(6, 2), new ChessPiece(ChessGame.TeamColor.WHITE, ChessPiece.PieceType.QUEEN));
            board.addPiece(new ChessPosition(8, 1), new ChessPiece(ChessGame.TeamColor.BLACK, ChessPiece.PieceType.KING));
            game.setBoard(board);
            finalMove = new ChessMove(new ChessPosition(6, 2), new ChessPosition(7, 3), null);
            token = "white-token";
            username = "Alice";
        }
        data.updateGame(new GameData(id, "Alice", "Bob", "Practice", game));
        var command = new MakeMoveCommand(token, id, finalMove);
        var result = new GameplayService(data).makeMove(command, username);
        var restarted = new SqlDataAccess();
        assertEquals(result.game(), restarted.getGame(id));
        assertTrue(restarted.getGame(id).game().isGameOver());
        assertTrue(result.statusNotice().contains(mate ? "checkmate" : "stalemate"));
        assertThrows(GameplayException.class, () -> new GameplayService(restarted).makeMove(command, username));
        assertEquals(result.game(), restarted.getGame(id));
    }
}
