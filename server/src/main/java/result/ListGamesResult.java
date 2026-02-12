package result;

import java.util.Collection;

public record ListGamesResult(Collection<GameInfo> games) {

    public record GameInfo(int gameID, String whiteUsername, String blackUsername, String gameName) {
    }
}
