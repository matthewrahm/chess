package handler;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.javalin.http.Context;
import model.GameData;
import service.GameService;

import java.util.Collection;

public class GameHandler {
    private final GameService gameService;
    private final Gson gson = new Gson();

    public GameHandler(GameService gameService) {
        this.gameService = gameService;
    }

    public void listGames(Context ctx) throws Exception {
        String authToken = ctx.header("Authorization");
        Collection<GameData> games = gameService.listGames(authToken);

        JsonArray gamesArray = new JsonArray();
        for (GameData game : games) {
            JsonObject gameObj = new JsonObject();
            gameObj.addProperty("gameID", game.gameID());
            gameObj.addProperty("whiteUsername", game.whiteUsername());
            gameObj.addProperty("blackUsername", game.blackUsername());
            gameObj.addProperty("gameName", game.gameName());
            gamesArray.add(gameObj);
        }

        JsonObject result = new JsonObject();
        result.add("games", gamesArray);
        ctx.contentType("application/json");
        ctx.result(gson.toJson(result));
    }

    public void createGame(Context ctx) throws Exception {
        String authToken = ctx.header("Authorization");
        JsonObject body = gson.fromJson(ctx.body(), JsonObject.class);
        String gameName = body.has("gameName") && !body.get("gameName").isJsonNull() ? body.get("gameName").getAsString() : null;

        int gameID = gameService.createGame(authToken, gameName);

        JsonObject result = new JsonObject();
        result.addProperty("gameID", gameID);
        ctx.contentType("application/json");
        ctx.result(gson.toJson(result));
    }

    public void joinGame(Context ctx) throws Exception {
        String authToken = ctx.header("Authorization");
        JsonObject body = gson.fromJson(ctx.body(), JsonObject.class);

        String playerColor = null;
        if (body.has("playerColor") && !body.get("playerColor").isJsonNull()) {
            playerColor = body.get("playerColor").getAsString();
        }

        Integer gameID = null;
        if (body.has("gameID") && !body.get("gameID").isJsonNull()) {
            gameID = body.get("gameID").getAsInt();
        }

        gameService.joinGame(authToken, playerColor, gameID);

        ctx.contentType("application/json");
        ctx.result("{}");
    }
}
