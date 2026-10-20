package handler;

import dataaccess.DataAccessException;
import io.javalin.http.Context;
import service.ClearService;

import java.util.Map;

public class ClearHandler {
    private final ClearService service;

    public ClearHandler(ClearService service) {
        this.service = service;
    }

    public void clear(Context context) throws DataAccessException {
        service.clear();
        HttpJson.write(context, Map.of());
    }
}
