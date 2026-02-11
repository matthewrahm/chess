package handler;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.javalin.http.Context;
import model.AuthData;
import service.UserService;

public class UserHandler {
    private final UserService userService;
    private final Gson gson = new Gson();

    public UserHandler(UserService userService) {
        this.userService = userService;
    }

    public void register(Context ctx) throws Exception {
        JsonObject body = gson.fromJson(ctx.body(), JsonObject.class);
        String username = body.has("username") && !body.get("username").isJsonNull() ? body.get("username").getAsString() : null;
        String password = body.has("password") && !body.get("password").isJsonNull() ? body.get("password").getAsString() : null;
        String email = body.has("email") && !body.get("email").isJsonNull() ? body.get("email").getAsString() : null;

        AuthData auth = userService.register(username, password, email);

        JsonObject result = new JsonObject();
        result.addProperty("username", auth.username());
        result.addProperty("authToken", auth.authToken());
        ctx.contentType("application/json");
        ctx.result(gson.toJson(result));
    }
}
