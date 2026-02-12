package service;

import dataaccess.AuthDAO;
import dataaccess.DataAccessException;
import dataaccess.UserDAO;
import model.AuthData;
import model.UserData;
import org.mindrot.jbcrypt.BCrypt;

import java.util.UUID;

/** Service handling user registration, login, and logout operations. */
public class UserService {
    private final UserDAO userDAO;
    private final AuthDAO authDAO;
    private final AuthHelper authHelper;

    public UserService(UserDAO userDAO, AuthDAO authDAO, AuthHelper authHelper) {
        this.userDAO = userDAO;
        this.authDAO = authDAO;
        this.authHelper = authHelper;
    }

    public AuthData register(String username, String password, String email) throws ServiceException, DataAccessException {
        if (username == null || password == null || email == null ||
                username.isEmpty() || password.isEmpty() || email.isEmpty()) {
            throw new ServiceException(400, "Error: bad request");
        }

        if (userDAO.getUser(username) != null) {
            throw new ServiceException(403, "Error: already taken");
        }

        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        userDAO.createUser(new UserData(username, hashedPassword, email));

        String authToken = UUID.randomUUID().toString();
        AuthData auth = new AuthData(authToken, username);
        authDAO.createAuth(auth);
        return auth;
    }

    public AuthData login(String username, String password) throws ServiceException, DataAccessException {
        if (username == null || password == null) {
            throw new ServiceException(400, "Error: bad request");
        }

        UserData user = userDAO.getUser(username);
        if (user == null) {
            throw new ServiceException(401, "Error: unauthorized");
        }

        if (!BCrypt.checkpw(password, user.password())) {
            throw new ServiceException(401, "Error: unauthorized");
        }

        String authToken = UUID.randomUUID().toString();
        AuthData auth = new AuthData(authToken, username);
        authDAO.createAuth(auth);
        return auth;
    }

    public void logout(String authToken) throws ServiceException, DataAccessException {
        authHelper.validateAuth(authToken);
        authDAO.deleteAuth(authToken);
    }
}
