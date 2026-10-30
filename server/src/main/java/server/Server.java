package server;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.sql.SqlDataAccess;
import handler.HttpJson;
import handler.ClearHandler;
import handler.GameHandler;
import handler.UserHandler;
import io.javalin.Javalin;
import service.ServiceException;
import service.ClearService;
import service.GameService;
import service.UserService;

public class Server {

    private final Javalin javalin;

    public Server() {
        this(createPersistentStorage());
    }

    private static DataAccess createPersistentStorage() {
        try {
            return new SqlDataAccess();
        } catch (DataAccessException exception) {
            throw new IllegalStateException("Unable to initialize server storage", exception);
        }
    }

    public Server(DataAccess dataAccess) {
        javalin = Javalin.create(config -> config.staticFiles.add("web"));

        var users = new UserHandler(new UserService(dataAccess));
        var clear = new ClearHandler(new ClearService(dataAccess));
        var games = new GameHandler(new GameService(dataAccess));
        javalin.post("/user", users::register);
        javalin.post("/session", users::login);
        javalin.delete("/session", users::logout);
        javalin.delete("/db", clear::clear);
        javalin.get("/game", games::list);
        javalin.post("/game", games::create);
        javalin.put("/game", games::join);

        javalin.exception(ServiceException.class, (error, context) -> {
            int status = switch (error.getCode()) {
                case BAD_REQUEST -> 400;
                case UNAUTHORIZED -> 401;
                case ALREADY_TAKEN -> 403;
            };
            HttpJson.error(context, status, error.getMessage());
        });
        javalin.exception(DataAccessException.class,
                (error, context) -> HttpJson.error(context, 500, "Error: internal server error"));
        javalin.exception(Exception.class,
                (error, context) -> HttpJson.error(context, 500, "Error: internal server error"));
    }

    public int run(int desiredPort) {
        javalin.start(desiredPort);
        return javalin.port();
    }

    public void stop() {
        javalin.stop();
    }
}
