package request;

/** Request body for user login. */
public record LoginRequest(String username, String password) {
}
