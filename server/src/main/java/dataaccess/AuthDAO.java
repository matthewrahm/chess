package dataaccess;

import model.AuthData;

/** Data access interface for authentication token storage operations. */
public interface AuthDAO {
    void clear() throws DataAccessException;
    void createAuth(AuthData auth) throws DataAccessException;
    AuthData getAuth(String authToken) throws DataAccessException;
    void deleteAuth(String authToken) throws DataAccessException;
}
