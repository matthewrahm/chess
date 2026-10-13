package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.DuplicateUserException;
import model.AuthData;
import model.UserData;
import org.mindrot.jbcrypt.BCrypt;
import request.LoginRequest;
import request.LogoutRequest;
import request.RegisterRequest;
import result.LoginResult;
import result.RegisterResult;

import java.util.UUID;

import static service.ServiceException.Code.*;

public class UserService {
    private final DataAccess dataAccess;

    public UserService(DataAccess dataAccess) {
        this.dataAccess = dataAccess;
    }

    public RegisterResult register(RegisterRequest request) throws ServiceException, DataAccessException {
        if (request == null || isMissing(request.username()) || isMissing(request.password())
                || isMissing(request.email())) {
            throw new ServiceException(BAD_REQUEST);
        }
        if (dataAccess.getUser(request.username()) != null) {
            throw new ServiceException(ALREADY_TAKEN);
        }

        String hash = BCrypt.hashpw(request.password(), BCrypt.gensalt());
        try {
            dataAccess.createUser(new UserData(request.username(), hash, request.email()));
        } catch (DuplicateUserException e) {
            // Another request may have registered this username after the lookup.
            throw new ServiceException(ALREADY_TAKEN);
        }
        AuthData auth = createAuthorization(request.username());
        return new RegisterResult(auth.username(), auth.authToken());
    }

    public LoginResult login(LoginRequest request) throws ServiceException, DataAccessException {
        if (request == null || isMissing(request.username()) || isMissing(request.password())) {
            throw new ServiceException(BAD_REQUEST);
        }
        UserData user = dataAccess.getUser(request.username());
        if (user == null || !BCrypt.checkpw(request.password(), user.password())) {
            throw new ServiceException(UNAUTHORIZED);
        }
        AuthData auth = createAuthorization(user.username());
        return new LoginResult(auth.username(), auth.authToken());
    }

    public void logout(LogoutRequest request) throws ServiceException, DataAccessException {
        if (request == null || isMissing(request.authToken()) || dataAccess.getAuth(request.authToken()) == null) {
            throw new ServiceException(UNAUTHORIZED);
        }
        dataAccess.deleteAuth(request.authToken());
    }

    private AuthData createAuthorization(String username) throws DataAccessException {
        var auth = new AuthData(UUID.randomUUID().toString(), username);
        dataAccess.createAuth(auth);
        return auth;
    }

    private boolean isMissing(String value) {
        return value == null || value.isBlank();
    }
}
