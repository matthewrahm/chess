package dataaccess;

import chess.ChessGame;
import model.AuthData;
import model.GameData;
import model.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MemoryDataAccessTests {
    private DataAccess data;
    private final UserData user = new UserData("matthew", "stored-password-hash", "matthew@example.com");

    @BeforeEach
    void setUp() {
        data = new MemoryDataAccess();
    }

    @Test
    void emptyStoreReturnsMissingLookupsAndEmptyGames() throws DataAccessException {
        assertNull(data.getUser("missing"));
        assertNull(data.getAuth("missing"));
        assertNull(data.getAuth(null));
        assertNull(data.getGame(1));
        assertTrue(data.listGames().isEmpty());
    }

    @Test
    void storesUserWithoutChangingFields() throws DataAccessException {
        data.createUser(user);
        assertEquals(user, data.getUser(user.username()));
    }

    @Test
    void duplicateUserCannotOverwriteOriginal() throws DataAccessException {
        data.createUser(user);
        var replacement = new UserData(user.username(), "different-hash", "other@example.com");
        assertThrows(DataAccessException.class, () -> data.createUser(replacement));
        assertEquals(user, data.getUser(user.username()));
    }

    @Test
    void deletingOneSessionPreservesOtherSessionsUserAndGame() throws DataAccessException {
        data.createUser(user);
        var first = new AuthData("first-token", user.username());
        var second = new AuthData("second-token", user.username());
        data.createAuth(first);
        data.createAuth(second);
        int gameID = data.createGame("Practice", new ChessGame());

        assertEquals(first, data.getAuth(first.authToken()));
        data.deleteAuth(first.authToken());

        assertNull(data.getAuth(first.authToken()));
        assertEquals(second, data.getAuth(second.authToken()));
        assertEquals(user, data.getUser(user.username()));
        assertNotNull(data.getGame(gameID));
    }

    @Test
    void duplicateTokenCannotChangeItsOwner() throws DataAccessException {
        var original = new AuthData("shared-token", user.username());
        data.createAuth(original);
        assertThrows(DataAccessException.class,
                () -> data.createAuth(new AuthData(original.authToken(), "someone-else")));
        assertEquals(original, data.getAuth(original.authToken()));
    }

    @Test
    void deletingUnknownTokenDoesNotDeleteValidSession() throws DataAccessException {
        var auth = new AuthData("valid-token", user.username());
        data.createAuth(auth);
        data.deleteAuth("unknown-token");
        assertEquals(auth, data.getAuth(auth.authToken()));
    }

    @Test
    void sameGameNameStillProducesDistinctIdsAndEmptySeats() throws DataAccessException {
        var board = new ChessGame();
        int first = data.createGame("Practice", board);
        int second = data.createGame("Practice", new ChessGame());
        assertNotEquals(first, second);
        assertEquals(new GameData(first, null, null, "Practice", board), data.getGame(first));
        assertEquals(2, data.listGames().size());
    }

    @Test
    void changingReturnedCollectionDoesNotRemoveStoredGames() throws DataAccessException {
        int gameID = data.createGame("Practice", new ChessGame());
        var result = data.listGames();
        assertTrue(result.contains(data.getGame(gameID)));
        result.clear();
        assertEquals(1, data.listGames().size());
        assertNotNull(data.getGame(gameID));
    }

    @Test
    void updateReplacesOnlySpecifiedGame() throws DataAccessException {
        var board = new ChessGame();
        int first = data.createGame("Practice", board);
        int second = data.createGame("Another game", new ChessGame());
        var otherGame = data.getGame(second);
        var joined = new GameData(first, user.username(), "opponent", "Practice", board);
        data.updateGame(joined);
        assertEquals(joined, data.getGame(first));
        assertEquals(otherGame, data.getGame(second));
        assertEquals(2, data.listGames().size());
    }

    @Test
    void updatingMissingGameDoesNotInsertIt() throws DataAccessException {
        var missing = new GameData(100, user.username(), null, "Missing", new ChessGame());
        assertThrows(DataAccessException.class, () -> data.updateGame(missing));
        assertNull(data.getGame(100));
        assertTrue(data.listGames().isEmpty());
    }

    @Test
    void clearRemovesAllDataAndAllowsNewRecords() throws DataAccessException {
        data.createUser(user);
        data.createAuth(new AuthData("token", user.username()));
        int oldGameID = data.createGame("Old game", new ChessGame());
        data.clear();
        assertNull(data.getUser(user.username()));
        assertNull(data.getAuth("token"));
        assertNull(data.getGame(oldGameID));
        assertTrue(data.listGames().isEmpty());
        data.createUser(user);
        assertEquals(user, data.getUser(user.username()));
        int newGameID = data.createGame("New game", new ChessGame());
        assertNotNull(data.getGame(newGameID));
    }

    @Test
    void separateStoresDoNotShareRecords() throws DataAccessException {
        data.createUser(user);
        data.createAuth(new AuthData("token", user.username()));
        int gameID = data.createGame("Practice", new ChessGame());
        DataAccess other = new MemoryDataAccess();
        assertNull(other.getUser(user.username()));
        assertNull(other.getAuth("token"));
        assertNull(other.getGame(gameID));
        other.clear();
        assertEquals(user, data.getUser(user.username()));
    }
}
