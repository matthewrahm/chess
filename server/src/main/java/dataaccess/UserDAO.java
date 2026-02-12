package dataaccess;

import model.UserData;

/** Data access interface for user storage operations. */
public interface UserDAO {
    void clear() throws DataAccessException;
    void createUser(UserData user) throws DataAccessException;
    UserData getUser(String username) throws DataAccessException;
}
