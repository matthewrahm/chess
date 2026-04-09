package client;

import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPiece;
import chess.ChessPosition;
import model.GameData;
import ui.ChessBoardRenderer;
import websocket.messages.ErrorMessage;
import websocket.messages.LoadGameMessage;
import websocket.messages.NotificationMessage;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static ui.EscapeSequences.*;

public class ChessClient {

    private final ServerFacade facade;
    private final int port;
    private String authToken;
    private String username;
    private State state = State.LOGGED_OUT;
    private ServerFacade.GameInfo[] cachedGames;

    private WebSocketFacade ws;
    private int currentGameID;
    private ChessGame.TeamColor playerColor;
    private ChessGame currentGame;
    private boolean awaitingResignConfirm;

    public enum State { LOGGED_OUT, LOGGED_IN, GAMEPLAY }

    public ChessClient(int port) {
        this.port = port;
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

            return switch (state) {
                case LOGGED_OUT -> evalPrelogin(command, params);
                case LOGGED_IN -> evalPostlogin(command, params);
                case GAMEPLAY -> evalGameplay(command, params);
            };
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
            case "quit" -> "quit";
            case "logout" -> logout();
            case "create" -> createGame(params);
            case "list" -> listGames();
            case "play" -> playGame(params);
            case "observe" -> observeGame(params);
            default -> "Unknown command. Type 'help' for available commands.";
        };
    }

    private String evalGameplay(String command, String[] params) {
        if (awaitingResignConfirm) {
            awaitingResignConfirm = false;
            if (command.equals("yes")) {
                return doResign();
            }
            return "Resign cancelled.";
        }

        return switch (command) {
            case "help" -> gameplayHelp();
            case "quit" -> {
                leaveGame();
                yield "quit";
            }
            case "redraw" -> redrawBoard();
            case "leave" -> leaveGame();
            case "move" -> makeMove(params);
            case "resign" -> confirmResign();
            case "highlight" -> highlightMoves(params);
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
                  play <ID> <WHITE|BLACK> - join a game
                  observe <ID> - watch a game in progress
                  logout - log out when you are done
                  quit - exit the program
                  help - display available commands""";
    }

    private String gameplayHelp() {
        return """
                  move <FROM> <TO> [PROMOTION] - make a move (e.g., move e2 e4)
                  highlight <POSITION> - show legal moves for a piece (e.g., highlight e2)
                  redraw - redraw the chess board
                  resign - forfeit the game
                  leave - leave the game
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

        playerColor = color.equals("WHITE") ? ChessGame.TeamColor.WHITE : ChessGame.TeamColor.BLACK;
        currentGameID = gameID;

        try {
            String wsUrl = "ws://localhost:" + port + "/ws";
            ws = new WebSocketFacade(wsUrl, createMessageHandler());
            ws.sendConnect(authToken, gameID);
        } catch (Exception e) {
            return "Failed to connect to game: " + e.getMessage();
        }

        state = State.GAMEPLAY;
        return "Joined game as " + color + ".";
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

        int gameID = cachedGames[index].gameID();
        playerColor = null;
        currentGameID = gameID;

        try {
            String wsUrl = "ws://localhost:" + port + "/ws";
            ws = new WebSocketFacade(wsUrl, createMessageHandler());
            ws.sendConnect(authToken, gameID);
        } catch (Exception e) {
            return "Failed to connect to game: " + e.getMessage();
        }

        state = State.GAMEPLAY;
        return "Observing game '" + cachedGames[index].gameName() + "'.";
    }

    private String redrawBoard() {
        if (currentGame == null) {
            return "No game loaded yet.";
        }
        ChessGame.TeamColor perspective = playerColor != null ? playerColor : ChessGame.TeamColor.WHITE;
        return ChessBoardRenderer.render(currentGame.getBoard(), perspective);
    }

    private String leaveGame() {
        try {
            if (ws != null) {
                ws.sendLeave(authToken, currentGameID);
                ws.close();
            }
        } catch (IOException e) {
            // Ignore close errors
        }
        ws = null;
        currentGame = null;
        currentGameID = 0;
        playerColor = null;
        awaitingResignConfirm = false;
        state = State.LOGGED_IN;
        return "Left the game.";
    }

    private String makeMove(String[] params) {
        if (playerColor == null) {
            return "Observers cannot make moves.";
        }
        if (ws == null || !ws.isOpen()) {
            return "Not connected to game. Try leaving and rejoining.";
        }
        if (params.length < 2) {
            return "Usage: move <FROM> <TO> [PROMOTION] (e.g., move e2 e4)";
        }

        ChessPosition from = parsePosition(params[0]);
        ChessPosition to = parsePosition(params[1]);
        if (from == null || to == null) {
            return "Invalid position. Use format like 'e2' (column letter + row number).";
        }

        ChessPiece.PieceType promotion = null;
        if (params.length >= 3) {
            promotion = parsePromotion(params[2]);
            if (promotion == null) {
                return "Invalid promotion piece. Use: queen, rook, bishop, knight.";
            }
        }

        ChessMove move = new ChessMove(from, to, promotion);
        try {
            ws.sendMakeMove(authToken, currentGameID, move);
        } catch (IOException e) {
            return "Error sending move: " + e.getMessage();
        }
        return "";
    }

    private String confirmResign() {
        awaitingResignConfirm = true;
        return "Are you sure you want to resign? Type 'yes' to confirm.";
    }

    private String doResign() {
        if (ws == null || !ws.isOpen()) {
            return "Not connected to game.";
        }
        try {
            ws.sendResign(authToken, currentGameID);
        } catch (IOException e) {
            return "Error: " + e.getMessage();
        }
        return "";
    }

    private String highlightMoves(String[] params) {
        if (params.length < 1) {
            return "Usage: highlight <POSITION> (e.g., highlight e2)";
        }
        if (currentGame == null) {
            return "No game loaded yet.";
        }

        ChessPosition pos = parsePosition(params[0]);
        if (pos == null) {
            return "Invalid position. Use format like 'e2'.";
        }

        Collection<ChessMove> moves = currentGame.validMoves(pos);
        if (moves == null || moves.isEmpty()) {
            return "No legal moves for that position.";
        }

        Set<ChessPosition> highlights = new HashSet<>();
        highlights.add(pos);
        for (ChessMove move : moves) {
            highlights.add(move.getEndPosition());
        }

        ChessGame.TeamColor perspective = playerColor != null ? playerColor : ChessGame.TeamColor.WHITE;
        return ChessBoardRenderer.render(currentGame.getBoard(), perspective, highlights);
    }

    private ChessPosition parsePosition(String input) {
        if (input == null || input.length() != 2) {
            return null;
        }
        char colChar = Character.toLowerCase(input.charAt(0));
        char rowChar = input.charAt(1);
        if (colChar < 'a' || colChar > 'h' || rowChar < '1' || rowChar > '8') {
            return null;
        }
        return new ChessPosition(rowChar - '0', colChar - 'a' + 1);
    }

    private ChessPiece.PieceType parsePromotion(String input) {
        return switch (input.toLowerCase()) {
            case "queen", "q" -> ChessPiece.PieceType.QUEEN;
            case "rook", "r" -> ChessPiece.PieceType.ROOK;
            case "bishop", "b" -> ChessPiece.PieceType.BISHOP;
            case "knight", "n" -> ChessPiece.PieceType.KNIGHT;
            default -> null;
        };
    }

    private WebSocketFacade.ServerMessageHandler createMessageHandler() {
        return new WebSocketFacade.ServerMessageHandler() {
            @Override
            public void onLoadGame(LoadGameMessage message) {
                GameData gameData = message.getGame();
                if (gameData != null && gameData.game() != null) {
                    currentGame = gameData.game();
                }
                ChessGame.TeamColor perspective = playerColor != null ? playerColor : ChessGame.TeamColor.WHITE;
                if (currentGame != null) {
                    System.out.println(ChessBoardRenderer.render(currentGame.getBoard(), perspective));
                }
            }

            @Override
            public void onNotification(NotificationMessage message) {
                System.out.println("\n" + SET_TEXT_COLOR_YELLOW + message.getMessage() + RESET_TEXT_COLOR);
            }

            @Override
            public void onError(ErrorMessage message) {
                System.out.println("\n" + SET_TEXT_COLOR_RED + message.getErrorMessage() + RESET_TEXT_COLOR);
            }
        };
    }
}
