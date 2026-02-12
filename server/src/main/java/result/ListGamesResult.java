package result;

import java.util.Collection;

/** Response body for listing all games. */
public record ListGamesResult(Collection<GameInfo> games) {

    public record GameInfo(int gameID, String whiteUsername, String blackUsername, String gameName) {
    }
}
