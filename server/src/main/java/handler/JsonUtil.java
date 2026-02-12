package handler;

import com.google.gson.Gson;

public class JsonUtil {
    private static final Gson GSON = new Gson();

    public static <T> T fromJson(String json, Class<T> clazz) {
        return GSON.fromJson(json, clazz);
    }

    public static String toJson(Object obj) {
        return GSON.toJson(obj);
    }
}
