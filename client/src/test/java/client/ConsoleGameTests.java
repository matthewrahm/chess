package client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConsoleGameTests extends ConsoleTestSupport {
    private static final String REGISTER = "register\nplayer\nsecret\np@example.com\n";

    @Test
    void createRequiresLoginWithoutPromptingOrContactingServer() throws Exception {
        try (var http = new TestHttpServer(500, "private details")) {
            String output = run(new ServerFacade(http.port()), "create\nquit\n");
            assertTrue(output.contains("Please log in first."));
            assertFalse(output.contains("Game name:"));
            assertNull(http.request());
        }
    }

    @Test
    void createPreservesNameAndLeavesBothSeatsEmpty() throws Exception {
        String output = run(facade, REGISTER + "create\n  Friday 王's chess club  \nhelp\nquit\n");
        var games = storage.listGames();
        assertEquals(1, games.size());
        var game = games.iterator().next();
        assertEquals("Friday 王's chess club", game.gameName());
        assertNull(game.whiteUsername());
        assertNull(game.blackUsername());
        assertTrue(output.contains("Game created."));
        assertTrue(output.contains("create   Create a new game"));
        assertFalse(output.contains("gameID"));
        assertFalse(output.contains("authToken"));
    }

    @Test
    void blankNameCanBeRetriedAndDuplicateNamesRemainSeparateGames() throws Exception {
        String output = run(facade, REGISTER + "create\n   \ncreate\nPractice\ncreate\nPractice\nquit\n");
        var games = storage.listGames();
        assertEquals(2, games.size());
        assertEquals(2, games.stream().map(game -> game.gameID()).distinct().count());
        assertTrue(games.stream().allMatch(game -> game.gameName().equals("Practice")));
        assertTrue(output.contains("Please enter a game name."));
        assertEquals(2, output.split("Game created.", -1).length - 1);
    }

    @Test
    void eofAtGameNameDoesNotCreateAGame() throws Exception {
        String output = run(facade, REGISTER + "create\n");
        assertTrue(storage.listGames().isEmpty());
        assertTrue(output.endsWith("Goodbye!\n"));
        assertFalse(output.contains("Game created."));
    }

    @Test
    void rejectedCreateLeavesConsoleUsableForRetry() throws Exception {
        String longName = "x".repeat(300);
        String output = run(facade, REGISTER + "create\n" + longName + "\ncreate\nPractice\nquit\n");
        assertEquals(1, storage.listGames().size());
        assertTrue(output.contains("Game created."));
        assertFalse(output.contains("Exception"));
        assertFalse(output.contains("INSERT"));
    }

    @Test
    void logoutPreventsFurtherGameCreation() throws Exception {
        String output = run(facade, REGISTER + "logout\ncreate\nquit\n");
        assertTrue(output.contains("Please log in first."));
        assertFalse(output.contains("Game name:"));
        assertTrue(storage.listGames().isEmpty());
    }
}
