package client;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

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

    @Test
    void listRequiresLoginWithoutContactingServer() throws Exception {
        try (var http = new TestHttpServer(500, "private details")) {
            String output = run(new ServerFacade(http.port()), "list\nquit\n");
            assertTrue(output.contains("Please log in first."));
            assertFalse(output.contains("No games yet."));
            assertNull(http.request());
        }
    }

    @Test
    void listingAnEmptyServerExplainsNextStep() throws Exception {
        String output = run(facade, REGISTER + "list\nquit\n");
        assertTrue(output.contains("No games yet. Use create to start one."));
        assertTrue(output.contains("list     List games and available seats"));
    }

    @Test
    void eachListReflectsGamesCreatedSinceThePreviousRequest() throws Exception {
        String output = run(facade, REGISTER + "list\ncreate\nPractice\nlist\ncreate\nTournament\nlist\nquit\n");
        assertTrue(output.contains("No games yet."));
        assertEquals(2, output.split("1[.] Practice", -1).length - 1);
        assertTrue(output.contains("2. Tournament | White: (open) | Black: (open)"));
        assertEquals(2, storage.listGames().size());
    }

    @Test
    void listShowsEveryGameAndBothPlayersRegardlessOfWhoCreatedIt() throws Exception {
        var white = facade.register("white", "secret", "w@example.com");
        var black = facade.register("black", "secret", "b@example.com");
        int game = facade.createGame(white.authToken(), "Shared board").gameID();
        facade.joinGame(white.authToken(), "WHITE", game);
        facade.joinGame(black.authToken(), "BLACK", game);
        String output = run(facade, REGISTER + "list\nquit\n");
        assertTrue(output.contains("1. Shared board | White: white | Black: black"));
        assertFalse(output.contains(white.authToken()));
        assertFalse(output.contains(black.authToken()));
        assertFalse(output.contains("gameID"));
    }

    @Test
    void failedListDoesNotPrintOldResultsAndCanBeRetried() throws Exception {
        var calls = new AtomicInteger();
        try (var http = new TestHttpServer(request -> {
            if (request.path().equals("/session")) {
                return new TestHttpServer.Response(200, "{\"username\":\"player\",\"authToken\":\"private-token\"}");
            }
            return switch (calls.incrementAndGet()) {
                case 1 -> new TestHttpServer.Response(200,
                        "{\"games\":[{\"gameID\":7654,\"gameName\":\"Earlier game\"}]}");
                case 2 -> new TestHttpServer.Response(500, "private database details");
                default -> new TestHttpServer.Response(200, "{\"games\":[]}");
            };
        })) {
            String output = run(new ServerFacade(http.port()), "login\nplayer\nsecret\nlist\nlist\nlist\nquit\n");
            assertEquals(3, calls.get());
            assertEquals(1, output.split("Earlier game", -1).length - 1);
            assertTrue(output.contains("No games yet."));
            assertFalse(output.contains("private database details"));
            assertFalse(output.contains("7654"));
            assertFalse(output.contains("private-token"));
            assertEquals("private-token", http.request().authorization());
            assertEquals("GET", http.request().method());
        }
    }

    @Test
    void logoutPreventsListing() throws Exception {
        String output = run(facade, REGISTER + "logout\nlist\nquit\n");
        assertTrue(output.contains("Please log in first."));
        assertFalse(output.contains("No games yet."));
    }
}
