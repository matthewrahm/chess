package ui;

import result.GameSummary;
import result.ListGamesResult;

import java.io.PrintStream;
import java.util.Comparator;

/** Displays public game details with consecutive numbers instead of internal IDs. */
public class GameListView {
    public void print(ListGamesResult result, PrintStream output) {
        var games = result.games().stream().sorted(Comparator.comparingInt(GameSummary::gameID)).toList();
        if (games.isEmpty()) {
            output.println("No games yet. Use create to start one.");
            return;
        }
        for (int index = 0; index < games.size(); index++) {
            var game = games.get(index);
            output.printf("%d. %s | White: %s | Black: %s%n", index + 1,
                    displayText(game.gameName()), displayText(game.whiteUsername()), displayText(game.blackUsername()));
        }
    }

    private String displayText(String value) {
        if (value == null) {
            return "(open)";
        }
        var text = new StringBuilder();
        for (char character : value.toCharArray()) {
            text.append(Character.isISOControl(character) ? ' ' : character);
        }
        return text.toString();
    }
}
