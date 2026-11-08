package ui;

import client.ServerException;
import client.ServerFacade;
import model.AuthData;

import java.io.PrintStream;
import java.util.Locale;

/** Owns console commands and the current session; ServerFacade owns HTTP. */
public class ConsoleClient {
    private final ServerFacade server;
    private final ConsoleInput input;
    private final PrintStream output;
    private AuthData session;
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
            String command = read(session == null ? "[SIGNED OUT] >>> " : "[SIGNED IN] >>> ", false);
            if (command == null) {
                break;
            }
            try {
                execute(command.trim().toLowerCase(Locale.ROOT));
            } catch (ServerException exception) {
                output.println(exception.getMessage());
            }
        }
        output.println("Goodbye!");
    }

    private void execute(String command) throws ServerException {
        switch (command) {
            case "help" -> help();
            case "quit" -> running = false;
            case "login" -> authenticate(false);
            case "register" -> authenticate(true);
            case "logout" -> logout();
            case "create" -> createGame();
            case "" -> { }
            default -> output.println("Unknown command. Type help to see available commands.");
        }
    }

    private void help() {
        if (session == null) {
            output.println("help     Show available commands");
            output.println("login    Sign in to an existing account");
            output.println("register Create an account and sign in");
        } else {
            output.println("help     Show available commands");
            output.println("logout   Sign out of your account");
            output.println("create   Create a new game");
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
        output.println("Signed in as " + session.username() + ".");
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
