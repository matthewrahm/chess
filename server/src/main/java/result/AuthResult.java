package result;

/** Response body for successful registration or login. */
public record AuthResult(String username, String authToken) {
}
