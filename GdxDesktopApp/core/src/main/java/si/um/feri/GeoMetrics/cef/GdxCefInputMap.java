package si.um.feri.GeoMetrics.cef;


import java.util.HashMap;
import java.util.Map;



public class GdxCefInputMap {
    private static final Map<Integer, Integer> keyCodeMap = new HashMap<>();


    private GdxCefInputMap(){};


    static {

        // Numbers (top row)
        keyCodeMap.put(7, 48);   // NUM_0
        keyCodeMap.put(8, 49);   // NUM_1
        keyCodeMap.put(9, 50);   // NUM_2
        keyCodeMap.put(10, 51);  // NUM_3
        keyCodeMap.put(11, 52);  // NUM_4
        keyCodeMap.put(12, 53);  // NUM_5
        keyCodeMap.put(13, 54);  // NUM_6
        keyCodeMap.put(14, 55);  // NUM_7
        keyCodeMap.put(15, 56);  // NUM_8
        keyCodeMap.put(16, 57);  // NUM_9

        // Letters
        keyCodeMap.put(29, 65);  // A
        keyCodeMap.put(30, 66);  // B
        keyCodeMap.put(31, 67);  // C
        keyCodeMap.put(32, 68);  // D
        keyCodeMap.put(33, 69);  // E
        keyCodeMap.put(34, 70);  // F
        keyCodeMap.put(35, 71);  // G
        keyCodeMap.put(36, 72);  // H
        keyCodeMap.put(37, 73);  // I
        keyCodeMap.put(38, 74);  // J
        keyCodeMap.put(39, 75);  // K
        keyCodeMap.put(40, 76);  // L
        keyCodeMap.put(41, 77);  // M
        keyCodeMap.put(42, 78);  // N
        keyCodeMap.put(43, 79);  // O
        keyCodeMap.put(44, 80);  // P
        keyCodeMap.put(45, 81);  // Q
        keyCodeMap.put(46, 82);  // R
        keyCodeMap.put(47, 83);  // S
        keyCodeMap.put(48, 84);  // T
        keyCodeMap.put(49, 85);  // U
        keyCodeMap.put(50, 86);  // V
        keyCodeMap.put(51, 87);  // W
        keyCodeMap.put(52, 88);  // X
        keyCodeMap.put(53, 89);  // Y
        keyCodeMap.put(54, 90);  // Z

        // Modifier keys
        keyCodeMap.put(57, 18);  // ALT_LEFT
        keyCodeMap.put(58, 18);  // ALT_RIGHT (same keyCode in JS)
        keyCodeMap.put(59, 16);  // SHIFT_LEFT
        keyCodeMap.put(60, 16);  // SHIFT_RIGHT (same keyCode in JS)
        keyCodeMap.put(129, 17); // CONTROL_LEFT
        keyCodeMap.put(130, 17); // CONTROL_RIGHT (same keyCode in JS)
        keyCodeMap.put(115, 20); // CAPS_LOCK

        // Punctuation
        keyCodeMap.put(75, 222); // APOSTROPHE
        keyCodeMap.put(77, null); // AT - no standard keyCode
        keyCodeMap.put(73, 220); // BACKSLASH
        keyCodeMap.put(55, 188); // COMMA
        keyCodeMap.put(70, 187); // EQUALS
        keyCodeMap.put(68, 192); // GRAVE (backtick)
        keyCodeMap.put(71, 219); // LEFT_BRACKET
        keyCodeMap.put(72, 221); // RIGHT_BRACKET
        keyCodeMap.put(69, 189); // MINUS
        keyCodeMap.put(56, 190); // PERIOD
        keyCodeMap.put(74, 186); // SEMICOLON
        keyCodeMap.put(76, 191); // SLASH
        keyCodeMap.put(62, 32);  // SPACE
        keyCodeMap.put(243, 186); // COLON (same as semicolon in JS)

        // Navigation
        keyCodeMap.put(19, 38);  // UP/DPAD_UP
        keyCodeMap.put(20, 40);  // DOWN/DPAD_DOWN
        keyCodeMap.put(21, 37);  // LEFT/DPAD_LEFT
        keyCodeMap.put(22, 39);  // RIGHT/DPAD_RIGHT
        keyCodeMap.put(23, null); // CENTER/DPAD_CENTER - no standard equivalent
        keyCodeMap.put(3, 36);   // HOME
        keyCodeMap.put(123, 35); // END
        keyCodeMap.put(92, 33);  // PAGE_UP
        keyCodeMap.put(93, 34);  // PAGE_DOWN

        // Editing keys
        keyCodeMap.put(67, 8);   // BACKSPACE/DEL
        keyCodeMap.put(112, 46); // FORWARD_DEL
        keyCodeMap.put(66, 13);  // ENTER
        keyCodeMap.put(61, 9);   // TAB
        keyCodeMap.put(111, 27); // ESCAPE
        keyCodeMap.put(124, 45); // INSERT

        // Function keys
        keyCodeMap.put(131, 112); // F1
        keyCodeMap.put(132, 113); // F2
        keyCodeMap.put(133, 114); // F3
        keyCodeMap.put(134, 115); // F4
        keyCodeMap.put(135, 116); // F5
        keyCodeMap.put(136, 117); // F6
        keyCodeMap.put(137, 118); // F7
        keyCodeMap.put(138, 119); // F8
        keyCodeMap.put(139, 120); // F9
        keyCodeMap.put(140, 121); // F10
        keyCodeMap.put(141, 122); // F11
        keyCodeMap.put(142, 123); // F12
        keyCodeMap.put(183, 124); // F13
        keyCodeMap.put(184, 125); // F14
        keyCodeMap.put(185, 126); // F15
        keyCodeMap.put(186, 127); // F16
        keyCodeMap.put(187, 128); // F17
        keyCodeMap.put(188, 129); // F18
        keyCodeMap.put(189, 130); // F19
        keyCodeMap.put(190, 131); // F20
        keyCodeMap.put(191, null); // F21 - not standard
        keyCodeMap.put(192, null); // F22 - not standard
        keyCodeMap.put(193, null); // F23 - not standard
        keyCodeMap.put(194, null); // F24 - not standard

        // Numpad
        keyCodeMap.put(144, 96);  // NUMPAD_0
        keyCodeMap.put(145, 97);  // NUMPAD_1
        keyCodeMap.put(146, 98);  // NUMPAD_2
        keyCodeMap.put(147, 99);  // NUMPAD_3
        keyCodeMap.put(148, 100); // NUMPAD_4
        keyCodeMap.put(149, 101); // NUMPAD_5
        keyCodeMap.put(150, 102); // NUMPAD_6
        keyCodeMap.put(151, 103); // NUMPAD_7
        keyCodeMap.put(152, 104); // NUMPAD_8
        keyCodeMap.put(153, 105); // NUMPAD_9
        keyCodeMap.put(154, 111); // NUMPAD_DIVIDE
        keyCodeMap.put(155, 106); // NUMPAD_MULTIPLY
        keyCodeMap.put(156, 109); // NUMPAD_SUBTRACT
        keyCodeMap.put(157, 107); // NUMPAD_ADD
        keyCodeMap.put(158, 110); // NUMPAD_DOT
        keyCodeMap.put(159, 188); // NUMPAD_COMMA (not standard, using comma)
        keyCodeMap.put(160, 13);  // NUMPAD_ENTER (same as regular enter)
        keyCodeMap.put(161, 187); // NUMPAD_EQUALS (not standard, using equals)
        keyCodeMap.put(162, null); // NUMPAD_LEFT_PAREN - no equivalent
        keyCodeMap.put(163, null); // NUMPAD_RIGHT_PAREN - no equivalent
        keyCodeMap.put(143, 144); // NUM_LOCK

        // Special system keys
        keyCodeMap.put(120, 44);  // PRINT_SCREEN
        keyCodeMap.put(121, 19);  // PAUSE
        keyCodeMap.put(116, 145); // SCROLL_LOCK

        // Media keys (not standard keyCodes)
        keyCodeMap.put(90, null); // MEDIA_FAST_FORWARD
        keyCodeMap.put(87, 176); // MEDIA_NEXT
        keyCodeMap.put(85, 179); // MEDIA_PLAY_PAUSE
        keyCodeMap.put(88, 177); // MEDIA_PREVIOUS
        keyCodeMap.put(89, null); // MEDIA_REWIND
        keyCodeMap.put(86, 178); // MEDIA_STOP
        keyCodeMap.put(91, 173); // MUTE
        keyCodeMap.put(24, 175); // VOLUME_UP
        keyCodeMap.put(25, 174); // VOLUME_DOWN
    }

    public static Integer getJavaScriptKeyCode(int gdxInputCode) {
        return keyCodeMap.get(gdxInputCode);
    }

    public static Map<Integer, Integer> getKeyCodeMap() {
        return new HashMap<>(keyCodeMap);
    }
}
