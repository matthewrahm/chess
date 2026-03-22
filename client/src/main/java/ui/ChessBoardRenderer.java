package ui;

import chess.ChessBoard;
import chess.ChessGame;
import chess.ChessPiece;
import chess.ChessPosition;

import static ui.EscapeSequences.*;

public class ChessBoardRenderer {

    private static final String LIGHT_BG = SET_BG_COLOR_WHITE;
    private static final String DARK_BG = SET_BG_COLOR_DARK_GREEN;
    private static final String BORDER_BG = SET_BG_COLOR_DARK_GREY;
    private static final String BORDER_TEXT = SET_TEXT_COLOR_WHITE;

    public static String render(ChessBoard board, ChessGame.TeamColor perspective) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");

        boolean whiteBottom = (perspective == ChessGame.TeamColor.WHITE);
        String[] colLabels = whiteBottom
                ? new String[]{"a", "b", "c", "d", "e", "f", "g", "h"}
                : new String[]{"h", "g", "f", "e", "d", "c", "b", "a"};

        appendColumnHeader(sb, colLabels);

        int rowStart = whiteBottom ? 8 : 1;
        int rowEnd = whiteBottom ? 1 : 8;
        int rowStep = whiteBottom ? -1 : 1;

        for (int row = rowStart; whiteBottom ? row >= rowEnd : row <= rowEnd; row += rowStep) {
            sb.append(BORDER_BG).append(BORDER_TEXT).append(" ").append(row).append(" ");

            int colStart = whiteBottom ? 1 : 8;
            int colEnd = whiteBottom ? 8 : 1;
            int colStep = whiteBottom ? 1 : -1;

            for (int col = colStart; whiteBottom ? col <= colEnd : col >= colEnd; col += colStep) {
                boolean isLight = (row + col) % 2 == 1;
                sb.append(isLight ? LIGHT_BG : DARK_BG);

                ChessPiece piece = board.getPiece(new ChessPosition(row, col));
                if (piece == null) {
                    sb.append(EMPTY);
                } else {
                    sb.append(getPieceColor(piece));
                    sb.append(getPieceSymbol(piece));
                }
            }

            sb.append(BORDER_BG).append(BORDER_TEXT).append(" ").append(row).append(" ");
            sb.append(RESET_BG_COLOR).append(RESET_TEXT_COLOR).append("\n");
        }

        appendColumnHeader(sb, colLabels);
        sb.append(RESET_BG_COLOR).append(RESET_TEXT_COLOR);

        return sb.toString();
    }

    private static void appendColumnHeader(StringBuilder sb, String[] colLabels) {
        sb.append(BORDER_BG).append(BORDER_TEXT).append("   ");
        for (String label : colLabels) {
            sb.append(" ").append(label).append("\u2003");
        }
        sb.append("   ");
        sb.append(RESET_BG_COLOR).append(RESET_TEXT_COLOR).append("\n");
    }

    private static String getPieceColor(ChessPiece piece) {
        return piece.getTeamColor() == ChessGame.TeamColor.WHITE
                ? SET_TEXT_COLOR_RED
                : SET_TEXT_COLOR_BLUE;
    }

    private static String getPieceSymbol(ChessPiece piece) {
        return switch (piece.getPieceType()) {
            case KING -> piece.getTeamColor() == ChessGame.TeamColor.WHITE ? WHITE_KING : BLACK_KING;
            case QUEEN -> piece.getTeamColor() == ChessGame.TeamColor.WHITE ? WHITE_QUEEN : BLACK_QUEEN;
            case BISHOP -> piece.getTeamColor() == ChessGame.TeamColor.WHITE ? WHITE_BISHOP : BLACK_BISHOP;
            case KNIGHT -> piece.getTeamColor() == ChessGame.TeamColor.WHITE ? WHITE_KNIGHT : BLACK_KNIGHT;
            case ROOK -> piece.getTeamColor() == ChessGame.TeamColor.WHITE ? WHITE_ROOK : BLACK_ROOK;
            case PAWN -> piece.getTeamColor() == ChessGame.TeamColor.WHITE ? WHITE_PAWN : BLACK_PAWN;
        };
    }
}
