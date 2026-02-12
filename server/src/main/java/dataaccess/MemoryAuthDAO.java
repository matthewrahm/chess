package dataaccess;

import model.AuthData;

import java.util.HashMap;
import java.util.Map;

/** In-memory implementation of {@link AuthDAO} using a HashMap. */
public class MemoryAuthDAO implements AuthDAO {
    private final Map<String, AuthData> auths = new HashMap<>();

    @Override
    public void clear() {
        auths.clear();
    }

    @Override
    public void createAuth(AuthData auth) {
        auths.put(auth.authToken(), auth);
    }

    @Override
    public AuthData getAuth(String authToken) {
        return auths.get(authToken);
    }

    @Override
    public void deleteAuth(String authToken) {
        auths.remove(authToken);
    }
}
