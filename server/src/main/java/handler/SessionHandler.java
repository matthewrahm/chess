package handler;

import io.javalin.http.Context;
import model.AuthData;
import request.LoginRequest;
import result.AuthResult;
import service.UserService;

/** HTTP handler for session management (POST /session, DELETE /session). */
public class SessionHandler {
    private final UserService userService;

    public SessionHandler(UserService userService) {
        this.userService = userService;
    }

    public void login(Context ctx) throws Exception {
        LoginRequest req = JsonUtil.fromJson(ctx.body(), LoginRequest.class);
        AuthData auth = userService.login(req.username(), req.password());

        ctx.contentType("application/json");
        ctx.result(JsonUtil.toJson(new AuthResult(auth.username(), auth.authToken())));
    }

    public void logout(Context ctx) throws Exception {
        String authToken = ctx.header("Authorization");
        userService.logout(authToken);
        ctx.contentType("application/json");
        ctx.result("{}");
    }
}
