package handler;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.javalin.http.Context;
import model.AuthData;
import service.UserService;

public class SessionHandler {
    private final UserService userService;
    private final Gson gson = new Gson();

    public SessionHandler(UserService userService) {
        this.userService = userService;
    }

    public void login(Context ctx) throws Exception {
        JsonObject body = gson.fromJson(ctx.body(), JsonObject.class);
        String username = body.has("username") && !body.get("username").isJsonNull() ? body.get("username").getAsString() : null;
        String password = body.has("password") && !body.get("password").isJsonNull() ? body.get("password").getAsString() : null;

        AuthData auth = userService.login(username, password);

        JsonObject result = new JsonObject();
        result.addProperty("username", auth.username());
        result.addProperty("authToken", auth.authToken());
        ctx.contentType("application/json");
        ctx.result(gson.toJson(result));
    }

    public void logout(Context ctx) throws Exception {
        String authToken = ctx.header("Authorization");
        userService.logout(authToken);
        ctx.contentType("application/json");
        ctx.result("{}");
    }
}
