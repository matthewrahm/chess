package dataaccess;

import model.UserData;

import java.util.HashMap;
import java.util.Map;

/** In-memory implementation of {@link UserDAO} using a HashMap. */
public class MemoryUserDAO implements UserDAO {
    private final Map<String, UserData> users = new HashMap<>();

    @Override
    public void clear() {
        users.clear();
    }

    @Override
    public void createUser(UserData user) {
        users.put(user.username(), user);
    }

    @Override
    public UserData getUser(String username) {
        return users.get(username);
    }
}
