package dataaccess.sql;

import chess.ChessGame;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.AuthData;
import model.GameData;
import model.UserData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SqlDataAccessTests {
    @Test
    void initializesStorageAndSharesRecordsAcrossInstances() throws Exception {
        try (var database = new DatabaseTestSupport()) {
            DataAccess first = new SqlDataAccess();
            var user = new UserData("player", "stored-hash", "player@example.com");
            var auth = new AuthData("token", "player");
            first.createUser(user);
            first.createAuth(auth);
            int id = first.createGame("Practice", new ChessGame());
            var game = new GameData(id, "player", null, "Practice", new ChessGame());
            first.updateGame(game);

            DataAccess second = new SqlDataAccess();
            assertEquals(user, second.getUser("player"));
            assertEquals(auth, second.getAuth("token"));
            assertEquals(game, second.getGame(id));
            assertEquals(List.of(game), List.copyOf(second.listGames()));
            assertNull(second.getUser("missing"));
            assertNull(second.getAuth("missing"));
            assertNull(second.getGame(id + 1));
        }
    }

    @Test
    void logoutAndClearAreVisibleToExistingInstances() throws Exception {
        try (var database = new DatabaseTestSupport()) {
            DataAccess first = new SqlDataAccess();
            DataAccess second = new SqlDataAccess();
            first.createUser(new UserData("player", "stored-hash", "player@example.com"));
            first.createAuth(new AuthData("first-token", "player"));
            first.createAuth(new AuthData("second-token", "player"));
            int id = first.createGame("Practice", new ChessGame());

            second.deleteAuth("first-token");
            assertNull(first.getAuth("first-token"));
            assertNotNull(first.getAuth("second-token"));
            assertNotNull(first.getUser("player"));
            assertNotNull(first.getGame(id));

            second.clear();
            assertNull(first.getAuth("second-token"));
            assertNull(first.getUser("player"));
            assertNull(first.getGame(id));
            assertTrue(first.listGames().isEmpty());
            assertDoesNotThrow(first::clear);
        }
    }

    @Test
    void failedInitializationDoesNotCreateUsableStorage() throws Exception {
        try (var database = new DatabaseTestSupport()) {
            database.configureUnavailableServer();
            assertThrows(DataAccessException.class, SqlDataAccess::new);
            database.configureDatabase(database.databaseName());
            assertTrue(new SqlDataAccess().listGames().isEmpty());
        }
    }
}
