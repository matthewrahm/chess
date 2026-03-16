package dataaccess;

import model.AuthData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MySqlAuthDAOTest {
    private MySqlAuthDAO authDAO;

    @BeforeEach
    void setUp() throws DataAccessException {
        authDAO = new MySqlAuthDAO();
        authDAO.clear();
    }

    @Test
    void clearSuccess() throws DataAccessException {
        authDAO.createAuth(new AuthData("token1", "alice"));
        authDAO.createAuth(new AuthData("token2", "bob"));
        authDAO.clear();
        assertNull(authDAO.getAuth("token1"));
        assertNull(authDAO.getAuth("token2"));
    }

    @Test
    void createAuthSuccess() throws DataAccessException {
        var auth = new AuthData("mytoken", "alice");
        authDAO.createAuth(auth);
        AuthData retrieved = authDAO.getAuth("mytoken");
        assertNotNull(retrieved);
        assertEquals("mytoken", retrieved.authToken());
        assertEquals("alice", retrieved.username());
    }

    @Test
    void createAuthDuplicateToken() throws DataAccessException {
        authDAO.createAuth(new AuthData("sametoken", "alice"));
        assertThrows(DataAccessException.class, () ->
                authDAO.createAuth(new AuthData("sametoken", "bob")));
    }

    @Test
    void getAuthSuccess() throws DataAccessException {
        authDAO.createAuth(new AuthData("findme", "charlie"));
        AuthData result = authDAO.getAuth("findme");
        assertNotNull(result);
        assertEquals("findme", result.authToken());
        assertEquals("charlie", result.username());
    }

    @Test
    void getAuthNotFound() throws DataAccessException {
        AuthData result = authDAO.getAuth("doesnotexist");
        assertNull(result);
    }

    @Test
    void deleteAuthSuccess() throws DataAccessException {
        authDAO.createAuth(new AuthData("removeme", "dave"));
        assertNotNull(authDAO.getAuth("removeme"));
        authDAO.deleteAuth("removeme");
        assertNull(authDAO.getAuth("removeme"));
    }

    @Test
    void deleteAuthNonexistent() throws DataAccessException {
        assertDoesNotThrow(() -> authDAO.deleteAuth("ghost"));
    }
}
