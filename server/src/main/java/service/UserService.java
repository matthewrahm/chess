package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.DuplicateUserException;
import model.AuthData;
import model.UserData;
import org.mindrot.jbcrypt.BCrypt;
import request.RegisterRequest;
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

    private AuthData createAuthorization(String username) throws DataAccessException {
        var auth = new AuthData(UUID.randomUUID().toString(), username);
        dataAccess.createAuth(auth);
        return auth;
    }

    private boolean isMissing(String value) {
        return value == null || value.isBlank();
    }
}
