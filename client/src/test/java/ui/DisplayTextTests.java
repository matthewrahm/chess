package ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DisplayTextTests {
    @ParameterizedTest
    @ValueSource(ints = {0, 7, 8, 9, 10, 13, 27, 127, 0x9b, 0x2028, 0x2029,
            0x061c, 0x200e, 0x200f, 0x202a, 0x202b, 0x202c, 0x202d, 0x202e,
            0x2066, 0x2067, 0x2068, 0x2069})
    void terminalAndDirectionalControlsBecomeSpaces(int character) {
        assertEquals("left right", DisplayText.singleLine("left" + (char) character + "right"));
    }

    @Test
    void readableUnicodeAndEmojiSequencesArePreserved() {
        String name = "Zoë 王 مرحبا 👩‍💻 e\u0301";
        assertEquals(name, DisplayText.singleLine(name));
        assertEquals("", DisplayText.singleLine(""));
    }
}
