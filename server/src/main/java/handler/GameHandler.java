package handler;

import com.google.gson.JsonElement;
import dataaccess.DataAccessException;
import io.javalin.http.Context;
import request.CreateGameRequest;
import request.JoinGameRequest;
import request.ListGamesRequest;
import service.GameService;
import service.ServiceException;

import java.util.Map;

import static service.ServiceException.Code.BAD_REQUEST;

public class GameHandler {
    private final GameService service;

    public GameHandler(GameService service) {
        this.service = service;
    }

    public void list(Context context) throws ServiceException, DataAccessException {
        HttpJson.write(context, service.listGames(new ListGamesRequest(context.header("authorization"))));
    }

    public void create(Context context) throws ServiceException, DataAccessException {
        var body = HttpJson.readBody(context, CreateBody.class, "gameName");
        var request = new CreateGameRequest(context.header("authorization"), body.gameName());
        HttpJson.write(context, service.createGame(request));
    }

    public void join(Context context) throws ServiceException, DataAccessException {
        var body = HttpJson.readBody(context, JoinBody.class, "playerColor");
        var request = new JoinGameRequest(context.header("authorization"), body.playerColor(), gameID(body.gameID()));
        service.joinGame(request);
        HttpJson.write(context, Map.of());
    }

    private Integer gameID(JsonElement value) throws ServiceException {
        if (value == null || value.isJsonNull()) {
            return null;
        }
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new ServiceException(BAD_REQUEST);
        }
        try {
            // Reject fractional and overflowing IDs instead of Gson's integer truncation.
            return value.getAsBigDecimal().intValueExact();
        } catch (ArithmeticException | NumberFormatException exception) {
            throw new ServiceException(BAD_REQUEST);
        }
    }

    private record CreateBody(String gameName) {
    }

    private record JoinBody(String playerColor, JsonElement gameID) {
    }
}
