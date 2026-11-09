package client;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ConsolePlayTests extends ConsoleTestSupport {
    private static final String REGISTER = "register\nplayer\nsecret\np@example.com\n";
    private static final String LOGIN = "login\nplayer\nsecret\n";

    @ParameterizedTest
    @ValueSource(strings = {" white ", "bLaCk"})
    void playJoinsSelectedColorAndSupportsRedrawAndReturnToLobby(String color) throws Exception {
        int id = prepareGame();
        var before = storage.getGame(id).game();
        String output = plain(run(facade, REGISTER + "play\n1\n" + color + "\nredraw\nback\nlist\nquit\n"));
        var game = storage.getGame(id);
        boolean white = color.trim().equalsIgnoreCase("white");
        assertEquals("player", white ? game.whiteUsername() : game.blackUsername());
        assertNull(white ? game.blackUsername() : game.whiteUsername());
        assertEquals(before, game.game());
        assertTrue(output.contains(white ? "    a  b  c  d  e  f  g  h" : "    h  g  f  e  d  c  b  a"));
        assertEquals(2, output.split("Initial position[.]", -1).length - 1);
        assertTrue(output.contains("[BOARD] >>>"));
        assertTrue(output.contains("Returned to the lobby."));
        assertTrue(output.contains("[SIGNED IN] >>>"));
    }

    @Test
    void observingAFullGameUsesWhiteViewWithoutChangingItsPlayers() throws Exception {
        var host = facade.register("host", "secret", "h@example.com");
        int id = facade.createGame(host.authToken(), "Full game").gameID();
        facade.joinGame(host.authToken(), "WHITE", id);
        facade.joinGame(host.authToken(), "BLACK", id);
        var before = storage.getGame(id);
        String output = plain(run(facade, REGISTER + "observe\n1\nhelp\nback\nquit\n"));
        assertEquals(before, storage.getGame(id));
        assertTrue(output.contains("Observing the initial position."));
        assertTrue(output.contains("    a  b  c  d  e  f  g  h"));
        assertTrue(output.contains("redraw   Draw the initial board again"));
        assertFalse(output.contains("Joined as"));
    }

    @Test
    void displayedNumberMapsToSparseIdFromTheExactDisplayedSnapshot() throws Exception {
        var lists = new AtomicInteger();
        try (var http = new TestHttpServer(request -> {
            if (request.path().equals("/session")) {
                return new TestHttpServer.Response(200, "{\"username\":\"player\",\"authToken\":\"private-token\"}");
            }
            if (request.method().equals("GET")) {
                lists.incrementAndGet();
                return new TestHttpServer.Response(200, "{\"games\":["
                        + "{\"gameID\":9876,\"gameName\":\"Second\"},{\"gameID\":4321,\"gameName\":\"First\"}]}");
            }
            return new TestHttpServer.Response(200, "{}");
        })) {
            String output = run(new ServerFacade(http.port()), LOGIN + "play\n2\nblack\nquit\n");
            assertEquals(1, lists.get());
            assertTrue(output.contains("1. First"));
            assertTrue(output.contains("2. Second"));
            assertFalse(output.contains("9876"));
            var request = http.request();
            assertEquals("PUT", request.method());
            assertEquals("private-token", request.authorization());
            var body = JsonParser.parseString(request.body()).getAsJsonObject();
            assertEquals(9876, body.get("gameID").getAsInt());
            assertEquals("BLACK", body.get("playerColor").getAsString());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"play", "observe"})
    void gameViewingRequiresLoginAndDoesNotPromptForSelection(String command) throws Exception {
        try (var http = new TestHttpServer(500, "private details")) {
            String output = run(new ServerFacade(http.port()), command + "\nquit\n");
            assertTrue(output.contains("Please log in first."));
            assertFalse(output.contains("Game number"));
            assertNull(http.request());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "2", "abc", "", "2147483648", "-2147483648"})
    void invalidGameNumbersLeaveSeatsEmptyAndAllowAnotherCommand(String number) throws Exception {
        int id = prepareGame();
        String output = run(facade, REGISTER + "play\n" + number + "\nhelp\nquit\n");
        assertTrue(output.contains("Please enter a listed game number."));
        assertFalse(output.contains("Color ("));
        assertFalse(output.contains("[BOARD]"));
        assertNull(storage.getGame(id).whiteUsername());
        assertNull(storage.getGame(id).blackUsername());
    }

    @ParameterizedTest
    @ValueSource(strings = {"green", "", "observer"})
    void invalidColorsCannotJoin(String color) throws Exception {
        int id = prepareGame();
        String output = run(facade, REGISTER + "play\n1\n" + color + "\nquit\n");
        assertTrue(output.contains("Please choose white or black."));
        assertFalse(output.contains("[BOARD]"));
        assertNull(storage.getGame(id).whiteUsername());
        assertNull(storage.getGame(id).blackUsername());
    }

    @ParameterizedTest
    @ValueSource(strings = {"play\ncancel\n", "play\n1\ncancel\n", "observe\ncancel\n", "play\n", "play\n1\n"})
    void cancellationOrEofDoesNotJoinOrEnterBoard(String commands) throws Exception {
        int id = prepareGame();
        String output = run(facade, REGISTER + commands);
        assertTrue(output.endsWith("Goodbye!\n"));
        assertFalse(output.contains("[BOARD]"));
        assertNull(storage.getGame(id).whiteUsername());
        assertNull(storage.getGame(id).blackUsername());
    }

    @Test
    void emptyListAvoidsSelectionAndOccupiedSeatAllowsAnotherColor() throws Exception {
        String output = run(facade, REGISTER + "play\ncreate\nPractice\nplay\n1\nwhite\nback\n"
                + "play\n1\nwhite\nplay\n1\nblack\nquit\n");
        assertTrue(output.contains("No games yet."));
        assertTrue(output.contains("That seat is already taken."));
        assertTrue(output.contains("Joined as black."));
        assertEquals(2, output.split("Joined as", -1).length - 1);
        var game = storage.listGames().iterator().next();
        assertEquals("player", game.whiteUsername());
        assertEquals("player", game.blackUsername());
    }

    @Test
    void boardCommandsAreRestrictedAndLogoutClearsBoardState() throws Exception {
        prepareGame();
        String output = run(facade, REGISTER + "observe\n1\ncreate\nlogout\nredraw\nquit\n");
        assertTrue(output.contains("Unknown command."));
        assertFalse(output.contains("Game name:"));
        assertTrue(output.substring(output.indexOf("Signed out.")).contains("[SIGNED OUT]"));
        assertEquals(1, output.split("Initial position[.]", -1).length - 1);
        assertEquals(1, storage.listGames().size());
    }

    private int prepareGame() throws Exception {
        var host = facade.register("host", "secret", "h@example.com");
        return facade.createGame(host.authToken(), "Practice").gameID();
    }

    private String plain(String text) {
        return text.replaceAll("\\x1B\\[[0-9;]*m", "");
    }
}
