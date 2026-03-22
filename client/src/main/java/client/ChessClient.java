package client;

public class ChessClient {

    private final ServerFacade facade;
    private String authToken;
    private String username;
    private State state = State.LOGGED_OUT;
    private ServerFacade.GameInfo[] cachedGames;

    public enum State { LOGGED_OUT, LOGGED_IN }

    public ChessClient(int port) {
        this.facade = new ServerFacade(port);
    }

    public State getState() {
        return state;
    }

    public String getUsername() {
        return username;
    }

    public String eval(String input) {
        try {
            var tokens = input.strip().split("\\s+");
            if (tokens.length == 0 || tokens[0].isEmpty()) {
                return "";
            }
            String command = tokens[0].toLowerCase();
            String[] params = new String[tokens.length - 1];
            System.arraycopy(tokens, 1, params, 0, params.length);

            if (state == State.LOGGED_OUT) {
                return evalPrelogin(command, params);
            } else {
                return evalPostlogin(command, params);
            }
        } catch (ServerFacadeException e) {
            String msg = e.getMessage();
            if (msg.startsWith("Error: ")) {
                msg = msg.substring(7);
            }
            return msg;
        } catch (Exception e) {
            return "An error occurred: " + e.getMessage();
        }
    }

    private String evalPrelogin(String command, String[] params) throws ServerFacadeException {
        return switch (command) {
            case "help" -> preloginHelp();
            case "quit" -> "quit";
            case "login" -> login(params);
            case "register" -> register(params);
            default -> "Unknown command. Type 'help' for available commands.";
        };
    }

    private String evalPostlogin(String command, String[] params) throws ServerFacadeException {
        return switch (command) {
            case "help" -> postloginHelp();
            case "logout" -> logout();
            case "create" -> createGame(params);
            case "list" -> listGames();
            case "play" -> playGame(params);
            case "observe" -> observeGame(params);
            default -> "Unknown command. Type 'help' for available commands.";
        };
    }

    private String preloginHelp() {
        return """
                  register <USERNAME> <PASSWORD> <EMAIL> - create an account
                  login <USERNAME> <PASSWORD> - log in to play chess
                  quit - exit the program
                  help - display available commands""";
    }

    private String postloginHelp() {
        return """
                  create <NAME> - create a new game
                  list - list all games
                  play <ID> [WHITE|BLACK] - join a game
                  observe <ID> - watch a game in progress
                  logout - log out when you are done
                  help - display available commands""";
    }

    private String login(String[] params) throws ServerFacadeException {
        if (params.length != 2) {
            return "Usage: login <USERNAME> <PASSWORD>";
        }
        var result = facade.login(params[0], params[1]);
        authToken = result.authToken();
        username = result.username();
        state = State.LOGGED_IN;
        return "Logged in as " + username + ".";
    }

    private String register(String[] params) throws ServerFacadeException {
        if (params.length != 3) {
            return "Usage: register <USERNAME> <PASSWORD> <EMAIL>";
        }
        var result = facade.register(params[0], params[1], params[2]);
        authToken = result.authToken();
        username = result.username();
        state = State.LOGGED_IN;
        return "Registered and logged in as " + username + ".";
    }

    private String logout() throws ServerFacadeException {
        facade.logout(authToken);
        authToken = null;
        username = null;
        state = State.LOGGED_OUT;
        cachedGames = null;
        return "Logged out.";
    }

    private String createGame(String[] params) throws ServerFacadeException {
        if (params.length < 1) {
            return "Usage: create <NAME>";
        }
        String gameName = String.join(" ", params);
        facade.createGame(authToken, gameName);
        return "Created game '" + gameName + "'.";
    }

    private String listGames() throws ServerFacadeException {
        cachedGames = facade.listGames(authToken);
        if (cachedGames.length == 0) {
            return "No games available.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("  %-4s %-20s %-15s %-15s%n", "#", "Game Name", "White", "Black"));
        for (int i = 0; i < cachedGames.length; i++) {
            var g = cachedGames[i];
            String white = g.whiteUsername() != null ? g.whiteUsername() : "--";
            String black = g.blackUsername() != null ? g.blackUsername() : "--";
            sb.append(String.format("  %-4d %-20s %-15s %-15s%n", i + 1, g.gameName(), white, black));
        }
        return sb.toString().stripTrailing();
    }

    private String playGame(String[] params) throws ServerFacadeException {
        if (params.length < 2) {
            return "Usage: play <ID> [WHITE|BLACK]";
        }
        if (cachedGames == null) {
            return "Please run 'list' first to see available games.";
        }
        int index;
        try {
            index = Integer.parseInt(params[0]) - 1;
        } catch (NumberFormatException e) {
            return "Invalid game number.";
        }
        if (index < 0 || index >= cachedGames.length) {
            return "Game number out of range. Use 'list' to see available games.";
        }
        String color = params[1].toUpperCase();
        if (!color.equals("WHITE") && !color.equals("BLACK")) {
            return "Color must be WHITE or BLACK.";
        }
        int gameID = cachedGames[index].gameID();
        facade.joinGame(authToken, color, gameID);

        chess.ChessGame game = new chess.ChessGame();
        chess.ChessGame.TeamColor perspective = color.equals("WHITE")
                ? chess.ChessGame.TeamColor.WHITE
                : chess.ChessGame.TeamColor.BLACK;
        return "Joined game as " + color + ".\n" +
                ui.ChessBoardRenderer.render(game.getBoard(), perspective);
    }

    private String observeGame(String[] params) {
        if (params.length < 1) {
            return "Usage: observe <ID>";
        }
        if (cachedGames == null) {
            return "Please run 'list' first to see available games.";
        }
        int index;
        try {
            index = Integer.parseInt(params[0]) - 1;
        } catch (NumberFormatException e) {
            return "Invalid game number.";
        }
        if (index < 0 || index >= cachedGames.length) {
            return "Game number out of range. Use 'list' to see available games.";
        }
        chess.ChessGame game = new chess.ChessGame();
        return "Observing game '" + cachedGames[index].gameName() + "'.\n" +
                ui.ChessBoardRenderer.render(game.getBoard(), chess.ChessGame.TeamColor.WHITE);
    }
}
