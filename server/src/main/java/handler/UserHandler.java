package handler;

import dataaccess.DataAccessException;
import io.javalin.http.Context;
import request.LoginRequest;
import request.LogoutRequest;
import request.RegisterRequest;
import service.ServiceException;
import service.UserService;

import java.util.Map;

public class UserHandler {
    private final UserService service;

    public UserHandler(UserService service) {
        this.service = service;
    }

    public void register(Context context) throws ServiceException, DataAccessException {
        var request = HttpJson.readBody(context, RegisterRequest.class, "username", "password", "email");
        HttpJson.write(context, service.register(request));
    }

    public void login(Context context) throws ServiceException, DataAccessException {
        var request = HttpJson.readBody(context, LoginRequest.class, "username", "password");
        HttpJson.write(context, service.login(request));
    }

    public void logout(Context context) throws ServiceException, DataAccessException {
        service.logout(new LogoutRequest(context.header("authorization")));
        HttpJson.write(context, Map.of());
    }
}
