package server;

import dataaccess.AuthDAO;
import dataaccess.DataAccessException;
import dataaccess.GameDAO;
import dataaccess.MySqlAuthDAO;
import dataaccess.MySqlGameDAO;
import dataaccess.MySqlUserDAO;
import dataaccess.UserDAO;
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
import websocket.WebSocketHandler;

/** Chess server application that configures and runs the Javalin HTTP server. */
public class Server {

    private final Javalin javalin;

    public Server() {
        UserDAO userDAO;
        AuthDAO authDAO;
        GameDAO gameDAO;
        try {
            userDAO = new MySqlUserDAO();
            authDAO = new MySqlAuthDAO();
            gameDAO = new MySqlGameDAO();
        } catch (DataAccessException ex) {
            throw new RuntimeException("failed to initialize database", ex);
        }

        AuthHelper authHelper = new AuthHelper(authDAO);
        UserService userService = new UserService(userDAO, authDAO, authHelper);
        GameService gameService = new GameService(gameDAO, authHelper);
        ClearService clearService = new ClearService(userDAO, authDAO, gameDAO);

        UserHandler userHandler = new UserHandler(userService);
        SessionHandler sessionHandler = new SessionHandler(userService);
        GameHandler gameHandler = new GameHandler(gameService);
        ClearHandler clearHandler = new ClearHandler(clearService);
        WebSocketHandler wsHandler = new WebSocketHandler(authHelper, gameDAO);

        javalin = Javalin.create(config -> config.staticFiles.add("web"));

        javalin.ws("/ws", ws -> {
            ws.onMessage(ctx -> wsHandler.handleMessage(ctx.session, ctx.message));
            ws.onClose(ctx -> wsHandler.handleClose(ctx.session, ctx.statusCode, ctx.reason));
        });

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
