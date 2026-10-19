package handler;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import io.javalin.http.Context;
import service.ServiceException;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;

import static service.ServiceException.Code.BAD_REQUEST;

/** JSON conversion belongs in the HTTP layer, not in the services. */
public final class HttpJson {
    private static final Gson GSON = new Gson();

    private HttpJson() {
    }

    public static <T> T readBody(Context context, Class<T> type, String... stringFields) throws ServiceException {
        String body = context.body();
        try (var reader = new JsonReader(new StringReader(body))) {
            // Gson otherwise accepts some non-JSON syntax and coerces numbers into strings.
            reader.setLenient(false);
            if (reader.peek() != JsonToken.BEGIN_OBJECT) {
                throw new ServiceException(BAD_REQUEST);
            }
            reader.skipValue();
            if (reader.peek() != JsonToken.END_DOCUMENT) {
                throw new ServiceException(BAD_REQUEST);
            }
            JsonObject object = GSON.fromJson(body, JsonObject.class);
            for (String field : stringFields) {
                var value = object.get(field);
                if (value != null && !value.isJsonNull()
                        && !(value.isJsonPrimitive() && value.getAsJsonPrimitive().isString())) {
                    throw new ServiceException(BAD_REQUEST);
                }
            }
            return GSON.fromJson(object, type);
        } catch (IOException | JsonParseException | IllegalStateException e) {
            throw new ServiceException(BAD_REQUEST);
        }
    }

    public static void write(Context context, Object response) {
        context.contentType("application/json").result(GSON.toJson(response));
    }

    public static void error(Context context, int status, String message) {
        context.status(status);
        write(context, Map.of("message", message));
    }
}
