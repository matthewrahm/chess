package ui;

import chess.ChessBoard;
import chess.ChessGame.TeamColor;
import chess.ChessPiece;
import chess.ChessPosition;

import java.util.Objects;

import static ui.EscapeSequences.*;

/** Renders a board without changing it; uppercase pieces are White, lowercase are Black. */
public class BoardRenderer {
    private static final String RESET = RESET_BG_COLOR + RESET_TEXT_COLOR;

    public String render(ChessBoard board, TeamColor perspective) {
        Objects.requireNonNull(board);
        Objects.requireNonNull(perspective);
        var text = new StringBuilder();
        String border = border(perspective);
        text.append(border);
        for (int index = 0; index < 8; index++) {
            int row = perspective == TeamColor.WHITE ? 8 - index : index + 1;
            text.append(" ").append(row).append(" ");
            for (int offset = 0; offset < 8; offset++) {
                int column = perspective == TeamColor.WHITE ? offset + 1 : 8 - offset;
                text.append((row + column) % 2 == 1 ? SET_BG_COLOR_WHITE : SET_BG_COLOR_LIGHT_GREY);
                text.append(SET_TEXT_COLOR_BLACK).append(" ");
                text.append(symbol(board.getPiece(new ChessPosition(row, column)))).append(" ");
            }
            text.append(RESET).append(" ").append(row).append(" \n");
        }
        return text.append(border).toString();
    }

    private String border(TeamColor perspective) {
        var text = new StringBuilder(RESET + "   ");
        String files = perspective == TeamColor.WHITE ? "abcdefgh" : "hgfedcba";
        for (char file : files.toCharArray()) {
            text.append(" ").append(file).append(" ");
        }
        return text.append("   \n").toString();
    }

    private char symbol(ChessPiece piece) {
        if (piece == null) {
            return ' ';
        }
        char symbol = switch (piece.getPieceType()) {
            case KING -> 'K';
            case QUEEN -> 'Q';
            case ROOK -> 'R';
            case BISHOP -> 'B';
            case KNIGHT -> 'N';
            case PAWN -> 'P';
        };
        return piece.getTeamColor() == TeamColor.WHITE ? symbol : Character.toLowerCase(symbol);
    }
}
