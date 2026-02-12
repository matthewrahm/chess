package handler;

import io.javalin.http.Context;
import model.AuthData;
import request.RegisterRequest;
import result.AuthResult;
import service.UserService;

public class UserHandler {
    private final UserService userService;

    public UserHandler(UserService userService) {
        this.userService = userService;
    }

    public void register(Context ctx) throws Exception {
        RegisterRequest req = JsonUtil.fromJson(ctx.body(), RegisterRequest.class);
        AuthData auth = userService.register(req.username(), req.password(), req.email());

        ctx.contentType("application/json");
        ctx.result(JsonUtil.toJson(new AuthResult(auth.username(), auth.authToken())));
    }
}
