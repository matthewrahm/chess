package service;

import dataaccess.AuthDAO;
import dataaccess.DataAccessException;
import model.AuthData;

public class AuthHelper {
    private final AuthDAO authDAO;

    public AuthHelper(AuthDAO authDAO) {
        this.authDAO = authDAO;
    }

    public AuthData validateAuth(String authToken) throws ServiceException, DataAccessException {
        if (authToken == null) {
            throw new ServiceException(401, "Error: unauthorized");
        }
        AuthData auth = authDAO.getAuth(authToken);
        if (auth == null) {
            throw new ServiceException(401, "Error: unauthorized");
        }
        return auth;
    }
}
