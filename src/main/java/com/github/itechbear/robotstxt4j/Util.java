package com.github.itechbear.robotstxt4j;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

public class Util {
    private static final String kHexDigits = "0123456789ABCDEF";

    private static final Set<Byte> kHexDigitSet = new HashSet<Byte>() {{
        add((byte) '0');
        add((byte) '1');
        add((byte) '2');
        add((byte) '3');
        add((byte) '4');
        add((byte) '5');
        add((byte) '6');
        add((byte) '7');
        add((byte) '8');
        add((byte) '9');
        add((byte) 'a');
        add((byte) 'b');
        add((byte) 'c');
        add((byte) 'd');
        add((byte) 'e');
        add((byte) 'f');
        add((byte) 'A');
        add((byte) 'B');
        add((byte) 'C');
        add((byte) 'D');
        add((byte) 'E');
        add((byte) 'F');
    }};

    public static boolean StartsWithIgnoreCase(String text, String prefix) {
        if (text.length() < prefix.length()) {
            return false;
        }
        return text.substring(0, prefix.length()).equalsIgnoreCase(prefix);
    }

    // for any char of needle, find its first position in the haystack,
    // this is basically equivalent to c++'s string::find_first_of(haystack, needle, start).
    // However, c++'s version can apply to ascii char only, whereas this implementation
    // supports unicode chars.
    public static int FindFirstCharOf(String hayStack, String needle, int start) {
        Set<Character> set = new HashSet(needle.length());
        for (char c : needle.toCharArray()) {
            set.add(c);
        }
        for (int i = start; i < hayStack.length(); ++i) {
            if (set.contains(hayStack.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    public static String GetPathParamsQuery(String url) {
        // Initial two slashes are ignored.
        int search_start = 0;
        if (url.length() >= 2 && url.charAt(0) == '/' && url.charAt(1) == '/') search_start = 2;

        // int early_path = url.find_first_of("/?;", search_start);
        int early_path = FindFirstCharOf(url, "/?;", search_start);
        int protocol_end = url.indexOf("://", search_start);
        if (early_path < protocol_end) {
            // If path, param or query starts before ://, :// doesn't indicate protocol.
            protocol_end = -1;
        }
        if (protocol_end == -1) {
            protocol_end = search_start;
        } else {
            protocol_end += 3;
        }

        // int path_start = url.find_first_of("/?;", protocol_end);
        int path_start = FindFirstCharOf(url, "/?;", protocol_end);
        if (path_start != -1) {
            int hash_pos = url.indexOf('#', search_start);
            if (hash_pos >= 0 && hash_pos < path_start) return "/";
            int path_end = (hash_pos == -1) ? url.length() : hash_pos;
            if (url.charAt(path_start) != '/') {
                // Prepend a slash if the result would start e.g. with '?'.
                return "/" + url.substring(path_start, path_end);
            }
            return url.substring(path_start, path_end);
        }

        return "/";
    }

    public static boolean IsHexDigit(byte c) {
        return kHexDigitSet.contains(c);
    }

    public static String MaybeEscapePattern(String url) {
        final byte[] bytes = url.getBytes();
        int num_to_escape = 0;
        boolean need_capitalize = false;

        // First, scan the buffer to see if changes are needed. Most don't.
        for (int i = 0; i < bytes.length; ++i) {
            // (a) % escape sequence.
            if (bytes[i] == '%' && IsHexDigit(bytes[i + 1]) && IsHexDigit(bytes[i + 2])) {
                if (Character.isLowerCase((char) bytes[i + 1]) || Character.isLowerCase((char) bytes[i + 2])) {
                    need_capitalize = true;
                }
                i += 2;
                // (b) needs escaping.
            } else if ((bytes[i] & 0x80) != 0) {
                num_to_escape++;
            }
            // (c) Already escaped and escape-characters normalized (eg. %2f -> %2F).
        }
        // Return if no changes needed.
        if (num_to_escape == 0 && !need_capitalize) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        StringBuilder dst = new StringBuilder(num_to_escape * 2 + bytes.length + 1);
        for (int i = 0; i < bytes.length; i++) {
            // (a) Normalize %-escaped sequence (eg. %2f -> %2F).
            if (bytes[i] == '%' && IsHexDigit(bytes[i + 1]) && IsHexDigit(bytes[i + 2])) {
                dst.append((char) bytes[i]);
                ++i;
                dst.append(Character.toUpperCase((char) bytes[i]));
                ++i;
                dst.append(Character.toUpperCase((char) bytes[i]));
            } else if ((bytes[i] & 0x80) != 0) {
                // (c) Normal character, no modification needed.
                dst.append('%');
                dst.append(kHexDigits.charAt((bytes[i] >> 4) & 0xf));
                dst.append(kHexDigits.charAt(bytes[i] & 0xf));
            } else {
                dst.append((char) bytes[i]);
            }
        }
        return dst.toString();
    }

    public static boolean isEnglishLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    public static void EmitKeyValueToHandler(int line,
                                             ParsedRobotsKey key,
                                             String value,
                                             RobotsParseHandler handler) {
        switch (key.Type()) {
            case USER_AGENT:
                handler.HandleUserAgent(line, value);
                break;
            case ALLOW:
                handler.HandleAllow(line, value);
                break;
            case DISALLOW:
                handler.HandleDisallow(line, value);
                break;
            case SITEMAP:
                handler.HandleSitemap(line, value);
                break;
            case UNKNOWN:
                handler.HandleUnknownAction(line, key.GetUnknownText(), value);
                break;
            // No default case Key:: to have the compiler warn about new values.
        }
    }

    public static void ParseRobotsTxt(String robots_body,
                                      RobotsParseHandler parse_callback) {
        RobotsTxtParser parser = new RobotsTxtParser(robots_body, parse_callback);
        parser.Parse();
    }
}
