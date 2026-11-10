package ui;

/** Keeps user-provided names on one terminal line without changing stored values. */
public final class DisplayText {
    private DisplayText() {
    }

    public static String singleLine(String value) {
        var text = new StringBuilder();
        value.codePoints().forEach(character -> text.appendCodePoint(unsafe(character) ? ' ' : character));
        return text.toString();
    }

    private static boolean unsafe(int character) {
        int type = Character.getType(character);
        return Character.isISOControl(character)
                || type == Character.LINE_SEPARATOR || type == Character.PARAGRAPH_SEPARATOR
                || character == 0x061c || character == 0x200e || character == 0x200f
                || (character >= 0x202a && character <= 0x202e)
                || (character >= 0x2066 && character <= 0x2069);
    }
}
