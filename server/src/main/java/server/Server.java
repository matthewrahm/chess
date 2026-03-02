package server;

import dataaccess.MemoryAuthDAO;
import dataaccess.MemoryGameDAO;
import dataaccess.MemoryUserDAO;
import handler.ClearHandler;
import handler.GameHandler;
import handler.JsonUtil;
import handler.SessionHandler;
import handler.UserHandler;
import io.javalin.Javalin;
import io.javalin.http.Context;
import service.AuthHelper;
import service.ClearService;
import service.GameService;
import service.ServiceException;
import service.UserService;

/** Chess server application that configures and runs the Javalin HTTP server. */
public class Server {

    private final Javalin javalin;

    public Server() {
        MemoryUserDAO userDAO = new MemoryUserDAO();
        MemoryAuthDAO authDAO = new MemoryAuthDAO();
        MemoryGameDAO gameDAO = new MemoryGameDAO();

        AuthHelper authHelper = new AuthHelper(authDAO);
        UserService userService = new UserService(userDAO, authDAO, authHelper);
        GameService gameService = new GameService(gameDAO, authHelper);
        ClearService clearService = new ClearService(userDAO, authDAO, gameDAO);

        UserHandler userHandler = new UserHandler(userService);
        SessionHandler sessionHandler = new SessionHandler(userService);
        GameHandler gameHandler = new GameHandler(gameService);
        ClearHandler clearHandler = new ClearHandler(clearService);

        javalin = Javalin.create(config -> config.staticFiles.add("web"));

        javalin.delete("/db", clearHandler::clear);
        javalin.post("/user", userHandler::register);
        javalin.post("/session", sessionHandler::login);
        javalin.delete("/session", sessionHandler::logout);
        javalin.get("/game", gameHandler::listGames);
        javalin.post("/game", gameHandler::createGame);
        javalin.put("/game", gameHandler::joinGame);

        javalin.exception(ServiceException.class, (e, ctx) ->
                sendError(ctx, e.getStatusCode(), e.getMessage()));

        javalin.exception(Exception.class, (e, ctx) ->
                sendError(ctx, 500, "Error: " + e.getMessage()));
    }

    private void sendError(Context ctx, int status, String message) {
        ctx.status(status);
        ctx.contentType("application/json");
        record ErrorResponse(String message) { }
        ctx.result(JsonUtil.toJson(new ErrorResponse(message)));
    }

    public int run(int desiredPort) {
        javalin.start(desiredPort);
        return javalin.port();
    }

    public void stop() {
        javalin.stop();
    }
}
