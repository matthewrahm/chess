package request;

/** Nullable gameID distinguishes a missing JSON field from a supplied number. */
public record JoinGameRequest(String authToken, String playerColor, Integer gameID) {
}
