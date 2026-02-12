package request;

/** Request body for user registration. */
public record RegisterRequest(String username, String password, String email) {
}
