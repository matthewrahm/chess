package ui;

import chess.ChessBoard;
import chess.ChessGame.TeamColor;
import client.ServerException;
import client.SessionExpiredException;
import client.ServerFacade;
import model.AuthData;
import result.GameSummary;

import java.io.PrintStream;
import java.util.Locale;
import java.util.List;

/** Owns console commands and the current session; ServerFacade owns HTTP. */
public class ConsoleClient {
    private final ServerFacade server;
    private final ConsoleInput input;
    private final PrintStream output;
    private AuthData session;
    private TeamColor boardPerspective;
    private boolean running = true;

    public ConsoleClient(ServerFacade server, ConsoleInput input, PrintStream output) {
        this.server = server;
        this.input = input;
        this.output = output;
    }

    public void run() {
        output.println("Welcome to Chess!");
        help();
        while (running) {
            String command = read(prompt(), false);
            if (command == null) {
                break;
            }
            try {
                execute(command.trim().toLowerCase(Locale.ROOT));
            } catch (ServerException exception) {
                output.println(exception.getMessage());
                if (exception instanceof SessionExpiredException) {
                    session = null;
                    boardPerspective = null;
                    help();
                }
            }
        }
        output.println("Goodbye!");
    }

    private String prompt() {
        if (boardPerspective != null) {
            return "[BOARD] >>> ";
        }
        return session == null ? "[SIGNED OUT] >>> " : "[SIGNED IN] >>> ";
    }

    private void execute(String command) throws ServerException {
        if (boardPerspective != null) {
            executeBoard(command);
            return;
        }
        switch (command) {
            case "help" -> help();
            case "quit" -> running = false;
            case "login" -> authenticate(false);
            case "register" -> authenticate(true);
            case "logout" -> logout();
            case "create" -> createGame();
            case "list" -> listGames();
            case "play" -> openGame(false);
            case "observe" -> openGame(true);
            case "" -> { }
            default -> output.println("Unknown command. Type help to see available commands.");
        }
    }

    private void help() {
        if (boardPerspective != null) {
            output.println("help     Show available commands");
            output.println("redraw   Draw the initial board again");
            output.println("back     Return to the lobby (keeps your seat)");
            output.println("logout   Sign out of your account");
        } else if (session == null) {
            output.println("help     Show available commands");
            output.println("login    Sign in to an existing account");
            output.println("register Create an account and sign in");
        } else {
            output.println("help     Show available commands");
            output.println("logout   Sign out of your account");
            output.println("create   Create a new game");
            output.println("list     List games and available seats");
            output.println("play     Choose a game and a color");
            output.println("observe  View a game without taking a seat");
        }
        output.println("quit     Exit Chess");
    }

    private void authenticate(boolean register) throws ServerException {
        if (session != null) {
            output.println("You are already signed in. Log out before switching accounts.");
            return;
        }
        String username = read("Username: ", false);
        String password = read("Password: ", true);
        String email = register ? read("Email: ", false) : null;
        if (!running) {
            return;
        }
        if (register) {
            var result = server.register(username, password, email);
            session = new AuthData(result.authToken(), result.username());
        } else {
            var result = server.login(username, password);
            session = new AuthData(result.authToken(), result.username());
        }
        output.println("Signed in as " + DisplayText.singleLine(session.username()) + ".");
        help();
    }

    private void createGame() throws ServerException {
        if (!requireSession()) {
            return;
        }
        String name = read("Game name: ", false);
        if (name == null) {
            return;
        }
        if (name.isBlank()) {
            output.println("Please enter a game name.");
            return;
        }
        server.createGame(session.authToken(), name.trim());
        output.println("Game created.");
    }

    private List<GameSummary> listGames() throws ServerException {
        if (!requireSession()) {
            return List.of();
        }
        var games = server.listGames(session.authToken());
        return new GameListView().print(games, output);
    }

    private void openGame(boolean observe) throws ServerException {
        var game = selectGame();
        if (game == null) {
            return;
        }
        TeamColor color = observe ? TeamColor.WHITE : readColor();
        if (color == null) {
            return;
        }
        if (!observe) {
            server.joinGame(session.authToken(), color.name(), game.gameID());
        }
        boardPerspective = color;
        output.println(observe ? "Observing the initial position." : "Joined as " + color.name().toLowerCase(Locale.ROOT) + ".");
        drawBoard();
        help();
    }

    private GameSummary selectGame() throws ServerException {
        var games = listGames();
        if (games.isEmpty()) {
            return null;
        }
        String number = read("Game number (or cancel): ", false);
        if (number == null || number.trim().equalsIgnoreCase("cancel")) {
            return null;
        }
        try {
            int index = Integer.parseInt(number.trim()) - 1;
            if (index >= 0 && index < games.size()) {
                return games.get(index);
            }
        } catch (NumberFormatException ignored) {
            // Invalid text and oversized integers use the same helpful message.
        }
        output.println("Please enter a listed game number.");
        return null;
    }

    private TeamColor readColor() {
        String color = read("Color (white/black, or cancel): ", false);
        if (color == null || color.trim().equalsIgnoreCase("cancel")) {
            return null;
        }
        try {
            return TeamColor.valueOf(color.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            output.println("Please choose white or black.");
            return null;
        }
    }

    private void executeBoard(String command) throws ServerException {
        switch (command) {
            case "help" -> help();
            case "redraw" -> drawBoard();
            case "back" -> {
                boardPerspective = null;
                output.println("Returned to the lobby.");
                help();
            }
            case "logout" -> logout();
            case "quit" -> running = false;
            case "" -> { }
            default -> output.println("Unknown command. Type help to see available commands.");
        }
    }

    private void drawBoard() {
        var board = new ChessBoard();
        board.resetBoard();
        output.println("Initial position. White pieces: uppercase. Black pieces: lowercase.");
        output.print(new BoardRenderer().render(board, boardPerspective));
    }

    private boolean requireSession() {
        if (session == null) {
            output.println("Please log in first.");
            return false;
        }
        return true;
    }

    private void logout() throws ServerException {
        if (session == null) {
            output.println("You are already signed out.");
            return;
        }
        server.logout(session.authToken());
        session = null;
        boardPerspective = null;
        output.println("Signed out.");
        help();
    }

    private String read(String prompt, boolean secret) {
        if (!running) {
            return null;
        }
        String value = input.read(prompt, output, secret);
        if (value == null) {
            running = false;
        }
        return value;
    }
}
