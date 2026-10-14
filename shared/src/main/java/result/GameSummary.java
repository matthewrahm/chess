package result;

/** Public game-list fields; deliberately excludes the board and turn. */
public record GameSummary(int gameID, String whiteUsername, String blackUsername, String gameName) {
}
