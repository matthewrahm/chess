package ui;

import org.junit.jupiter.api.Test;
import result.GameSummary;
import result.ListGamesResult;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameListViewTests {
    @Test
    void sparseIdsBecomeConsecutiveNumbersWithCorrectNamesAndSeats() {
        String output = render(List.of(
                new GameSummary(9876, null, "Zoë", "Practice"),
                new GameSummary(321, "王", null, "Practice")));
        assertEquals("1. Practice | White: 王 | Black: (open)\n"
                + "2. Practice | White: (open) | Black: Zoë\n", output);
        assertFalse(output.contains("9876"));
        assertFalse(output.contains("321"));
    }

    @Test
    void serverOrderingDoesNotChangeDisplayedNumbers() {
        var first = new GameSummary(55, null, null, "Alpha");
        var second = new GameSummary(70, "white player", "black player", "Beta");
        assertEquals(render(List.of(first, second)), render(List.of(second, first)));
    }

    @Test
    void emptyListExplainsHowToCreateAGame() {
        assertEquals("No games yet. Use create to start one.\n", render(List.of()));
    }

    @Test
    void controlCharactersCannotAddRowsOrEscapeSequences() {
        String output = render(List.of(new GameSummary(77, "name\twith\u009bcontrol", "black\rplayer",
                "Game\n\u001b[31m王")));
        assertEquals("1. Game  [31m王 | White: name with control | Black: black player\n", output);
        assertEquals(1, output.lines().count());
        assertFalse(output.contains("\u001b"));
    }

    private String render(List<GameSummary> games) {
        var bytes = new ByteArrayOutputStream();
        try (var output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            new GameListView().print(new ListGamesResult(games), output);
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }
}
