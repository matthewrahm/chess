package request;

/** Request body for joining an existing game. */
public record JoinGameRequest(String playerColor, Integer gameID) {
}
