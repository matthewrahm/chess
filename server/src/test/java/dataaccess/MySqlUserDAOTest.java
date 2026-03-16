package dataaccess;

import model.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MySqlUserDAOTest {
    private MySqlUserDAO userDAO;

    @BeforeEach
    void setUp() throws DataAccessException {
        userDAO = new MySqlUserDAO();
        userDAO.clear();
    }

    @Test
    void clearSuccess() throws DataAccessException {
        userDAO.createUser(new UserData("alice", "pass123", "alice@mail.com"));
        userDAO.clear();
        assertNull(userDAO.getUser("alice"));
    }

    @Test
    void createUserSuccess() throws DataAccessException {
        var user = new UserData("bob", "secret", "bob@mail.com");
        userDAO.createUser(user);
        UserData retrieved = userDAO.getUser("bob");
        assertNotNull(retrieved);
        assertEquals("bob", retrieved.username());
        assertEquals("bob@mail.com", retrieved.email());
    }

    @Test
    void createUserDuplicate() throws DataAccessException {
        var user = new UserData("charlie", "pass", "c@mail.com");
        userDAO.createUser(user);
        assertThrows(DataAccessException.class, () ->
                userDAO.createUser(new UserData("charlie", "other", "c2@mail.com")));
    }

    @Test
    void getUserSuccess() throws DataAccessException {
        userDAO.createUser(new UserData("dave", "pw", "dave@mail.com"));
        UserData result = userDAO.getUser("dave");
        assertNotNull(result);
        assertEquals("dave", result.username());
        assertEquals("pw", result.password());
        assertEquals("dave@mail.com", result.email());
    }

    @Test
    void getUserNotFound() throws DataAccessException {
        UserData result = userDAO.getUser("nonexistent");
        assertNull(result);
    }
}
