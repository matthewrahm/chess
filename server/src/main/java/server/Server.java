package server;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.MemoryDataAccess;
import handler.HttpJson;
import handler.ClearHandler;
import handler.UserHandler;
import io.javalin.Javalin;
import service.ServiceException;
import service.ClearService;
import service.UserService;

public class Server {

    private final Javalin javalin;

    public Server() {
        this(new MemoryDataAccess());
    }

    public Server(DataAccess dataAccess) {
        javalin = Javalin.create(config -> config.staticFiles.add("web"));

        var users = new UserHandler(new UserService(dataAccess));
        var clear = new ClearHandler(new ClearService(dataAccess));
        javalin.post("/user", users::register);
        javalin.post("/session", users::login);
        javalin.delete("/session", users::logout);
        javalin.delete("/db", clear::clear);

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
