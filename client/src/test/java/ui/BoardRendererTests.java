package ui;

import chess.ChessBoard;
import chess.ChessGame.TeamColor;
import chess.ChessPiece;
import chess.ChessPosition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;
import static ui.EscapeSequences.*;

class BoardRendererTests {
    private final BoardRenderer renderer = new BoardRenderer();

    @Test
    void whitePerspectiveHasA1AtBottomLeftAndQueensOnDFile() {
        var board = new ChessBoard();
        board.resetBoard();
        var lines = plain(renderer.render(board, TeamColor.WHITE)).lines().toList();
        assertEquals("    a  b  c  d  e  f  g  h    ", lines.getFirst());
        assertEquals(" 8  r  n  b  q  k  b  n  r  8 ", lines.get(1));
        assertEquals(" 7  p  p  p  p  p  p  p  p  7 ", lines.get(2));
        assertEquals(" 2  P  P  P  P  P  P  P  P  2 ", lines.get(7));
        assertEquals(" 1  R  N  B  Q  K  B  N  R  1 ", lines.get(8));
        assertEquals(lines.getFirst(), lines.getLast());
    }

    @Test
    void blackPerspectiveRotatesBothRanksAndFiles() {
        var board = new ChessBoard();
        board.resetBoard();
        var lines = plain(renderer.render(board, TeamColor.BLACK)).lines().toList();
        assertEquals("    h  g  f  e  d  c  b  a    ", lines.getFirst());
        assertEquals(" 1  R  N  B  K  Q  B  N  R  1 ", lines.get(1));
        assertEquals(" 8  r  n  b  k  q  b  n  r  8 ", lines.get(8));
        assertEquals(lines.getFirst(), lines.getLast());
    }

    @ParameterizedTest
    @EnumSource(TeamColor.class)
    void squaresHaveEqualWidthsAndBalancedColorsWithNoColorBleeding(TeamColor color) {
        String output = renderer.render(new ChessBoard(), color);
        var lines = plain(output).lines().toList();
        assertEquals(10, lines.size());
        assertTrue(lines.stream().allMatch(line -> line.length() == 30));
        assertEquals(32, occurrences(output, SET_BG_COLOR_WHITE));
        assertEquals(32, occurrences(output, SET_BG_COLOR_LIGHT_GREY));
        assertEquals(10, occurrences(output, RESET_BG_COLOR + RESET_TEXT_COLOR));
        assertTrue(lines.subList(1, 9).stream().allMatch(line -> line.substring(3, 27).isBlank()));
    }

    @Test
    void whiteTopLeftAndBottomRightAreLightSquares() {
        var lines = renderer.render(new ChessBoard(), TeamColor.WHITE).lines().toList();
        assertTrue(lines.get(1).startsWith(" 8 " + SET_BG_COLOR_WHITE));
        assertTrue(lines.get(8).startsWith(" 1 " + SET_BG_COLOR_LIGHT_GREY));
        assertTrue(lines.get(8).endsWith(SET_BG_COLOR_WHITE + SET_TEXT_COLOR_BLACK + "   "
                + RESET_BG_COLOR + RESET_TEXT_COLOR + " 1 "));
    }

    @Test
    void arbitraryPositionIsRenderedWithoutResettingOrChangingTheBoard() {
        var board = new ChessBoard();
        board.addPiece(new ChessPosition(4, 3), new ChessPiece(TeamColor.WHITE, ChessPiece.PieceType.KING));
        board.addPiece(new ChessPosition(7, 7), new ChessPiece(TeamColor.BLACK, ChessPiece.PieceType.ROOK));
        var before = board.copy();
        var white = plain(renderer.render(board, TeamColor.WHITE)).lines().toList();
        var black = plain(renderer.render(board, TeamColor.BLACK)).lines().toList();
        assertEquals('K', white.get(5).charAt(10));
        assertEquals('r', white.get(2).charAt(22));
        assertEquals('K', black.get(4).charAt(19));
        assertEquals('r', black.get(7).charAt(7));
        assertEquals(before, board);
    }

    private String plain(String text) {
        return text.replaceAll("\\x1B\\[[0-9;]*m", "");
    }

    private int occurrences(String text, String part) {
        return (text.length() - text.replace(part, "").length()) / part.length();
    }
}
