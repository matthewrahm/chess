package handler;

import io.javalin.http.Context;
import service.ClearService;

/** HTTP handler for clearing all application data (DELETE /db). */
public class ClearHandler {
    private final ClearService clearService;

    public ClearHandler(ClearService clearService) {
        this.clearService = clearService;
    }

    public void clear(Context ctx) throws Exception {
        clearService.clear();
        ctx.contentType("application/json");
        ctx.result("{}");
    }
}
