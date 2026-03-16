package dataaccess;

import chess.ChessGame;
import model.GameData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

public class MySqlGameDAOTest {
    private MySqlGameDAO gameDAO;

    @BeforeEach
    void setUp() throws DataAccessException {
        gameDAO = new MySqlGameDAO();
        gameDAO.clear();
    }

    @Test
    void clearSuccess() throws DataAccessException {
        int id = gameDAO.generateId();
        gameDAO.createGame(new GameData(id, null, null, "TestGame", new ChessGame()));
        gameDAO.clear();
        assertTrue(gameDAO.listGames().isEmpty());
    }

    @Test
    void generateIdSuccess() throws DataAccessException {
        int id1 = gameDAO.generateId();
        int id2 = gameDAO.generateId();
        assertTrue(id1 > 0);
        assertTrue(id2 > id1);
    }

    @Test
    void createGameSuccess() throws DataAccessException {
        int id = gameDAO.generateId();
        var game = new GameData(id, null, null, "My Game", new ChessGame());
        gameDAO.createGame(game);
        GameData retrieved = gameDAO.getGame(id);
        assertNotNull(retrieved);
        assertEquals("My Game", retrieved.gameName());
        assertNotNull(retrieved.game());
    }

    @Test
    void createGameDuplicateId() throws DataAccessException {
        int id = gameDAO.generateId();
        gameDAO.createGame(new GameData(id, null, null, "Game1", new ChessGame()));
        assertThrows(DataAccessException.class, () ->
                gameDAO.createGame(new GameData(id, null, null, "Game2", new ChessGame())));
    }

    @Test
    void getGameSuccess() throws DataAccessException {
        int id = gameDAO.generateId();
        var chessGame = new ChessGame();
        gameDAO.createGame(new GameData(id, "white", "black", "Full Game", chessGame));
        GameData result = gameDAO.getGame(id);
        assertNotNull(result);
        assertEquals(id, result.gameID());
        assertEquals("white", result.whiteUsername());
        assertEquals("black", result.blackUsername());
        assertEquals("Full Game", result.gameName());
    }

    @Test
    void getGameNotFound() throws DataAccessException {
        GameData result = gameDAO.getGame(99999);
        assertNull(result);
    }

    @Test
    void listGamesSuccess() throws DataAccessException {
        int id1 = gameDAO.generateId();
        int id2 = gameDAO.generateId();
        gameDAO.createGame(new GameData(id1, null, null, "Game A", new ChessGame()));
        gameDAO.createGame(new GameData(id2, null, null, "Game B", new ChessGame()));
        Collection<GameData> games = gameDAO.listGames();
        assertEquals(2, games.size());
    }

    @Test
    void listGamesEmpty() throws DataAccessException {
        Collection<GameData> games = gameDAO.listGames();
        assertTrue(games.isEmpty());
    }

    @Test
    void updateGameSuccess() throws DataAccessException {
        int id = gameDAO.generateId();
        gameDAO.createGame(new GameData(id, null, null, "Update Me", new ChessGame()));
        gameDAO.updateGame(new GameData(id, "playerW", "playerB", "Update Me", new ChessGame()));
        GameData updated = gameDAO.getGame(id);
        assertEquals("playerW", updated.whiteUsername());
        assertEquals("playerB", updated.blackUsername());
    }

    @Test
    void updateGameNonexistent() throws DataAccessException {
        assertDoesNotThrow(() ->
                gameDAO.updateGame(new GameData(99999, "a", "b", "Ghost", new ChessGame())));
        assertNull(gameDAO.getGame(99999));
    }
}
