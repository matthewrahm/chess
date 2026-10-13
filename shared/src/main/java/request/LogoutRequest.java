package request;

/** The HTTP handler supplies authToken from the authorization header. */
public record LogoutRequest(String authToken) {
}
