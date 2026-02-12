package handler;

import io.javalin.http.Context;
import model.GameData;
import request.CreateGameRequest;
import request.JoinGameRequest;
import result.CreateGameResult;
import result.ListGamesResult;
import result.ListGamesResult.GameInfo;
import service.GameService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** HTTP handler for game operations (GET /game, POST /game, PUT /game). */
public class GameHandler {
    private final GameService gameService;

    public GameHandler(GameService gameService) {
        this.gameService = gameService;
    }

    public void listGames(Context ctx) throws Exception {
        String authToken = ctx.header("Authorization");
        Collection<GameData> games = gameService.listGames(authToken);

        List<GameInfo> gameInfos = new ArrayList<>();
        for (GameData game : games) {
            gameInfos.add(new GameInfo(game.gameID(), game.whiteUsername(), game.blackUsername(), game.gameName()));
        }

        ctx.contentType("application/json");
        ctx.result(JsonUtil.toJson(new ListGamesResult(gameInfos)));
    }

    public void createGame(Context ctx) throws Exception {
        String authToken = ctx.header("Authorization");
        CreateGameRequest req = JsonUtil.fromJson(ctx.body(), CreateGameRequest.class);

        int gameID = gameService.createGame(authToken, req.gameName());

        ctx.contentType("application/json");
        ctx.result(JsonUtil.toJson(new CreateGameResult(gameID)));
    }

    public void joinGame(Context ctx) throws Exception {
        String authToken = ctx.header("Authorization");
        JoinGameRequest req = JsonUtil.fromJson(ctx.body(), JoinGameRequest.class);

        gameService.joinGame(authToken, req.playerColor(), req.gameID());

        ctx.contentType("application/json");
        ctx.result("{}");
    }
}
