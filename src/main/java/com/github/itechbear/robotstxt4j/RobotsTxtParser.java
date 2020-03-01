package com.github.itechbear.robotstxt4j;

import java.util.AbstractMap;
import java.util.Map;

public class RobotsTxtParser {
    private static final char[] utf_bom = {0xEF, 0xBB, 0xBF};
    private static final int kMaxLineLen = 2083 << 3;

    private final String robots_body_;
    private final RobotsParseHandler handler_;


    RobotsTxtParser(String robots_body,
                    RobotsParseHandler handler) {
        this.robots_body_ = robots_body;
        this.handler_ = handler;
    }

    private static Map.Entry<String, String> GetKeyAndValueFrom(String line) {
        // remove trailing comments
        int pos = line.indexOf('#');
        if (pos >= 0) {
            line = line.substring(0, pos);
        }
        line = line.trim();

        // Rules must match the following pattern:
        //   <key>[ \t]*:[ \t]*<value>
        // char* sep = strchr(line, ':');
        int sep = line.indexOf(':');
        if (sep < 0) {
            // Google-specific optimization: some people forget the colon, so we need to
            // accept whitespace in its stead.
            // static const char * const kWhite = " \t";
            // sep = strpbrk(line, kWhite);
            sep = Util.FindFirstCharOf(line, " \t", 0);
        }
        if (sep < 0) {
            return null;
        }

        String key = line.substring(0, sep).trim();
        String value = line.substring(sep + 1).trim();
        if (value.isEmpty()) {
            return null;
        }
        return new AbstractMap.SimpleImmutableEntry<String, String>(key, value);
    }

    public void Parse() {
        // UTF-8 byte order marks.


        // Certain browsers limit the URL length to 2083 bytes. In a robots.txt, it's
        // fairly safe to assume any valid line isn't going to be more than many times
        // that max url length of 2KB. We want some padding for
        // UTF-8 encoding/nulls/etc. but a much smaller bound would be okay as well.
        // If so, we can ignore the chars on a line past that.

        // Allocate a buffer used to process the current line.
        StringBuilder line_buffer = new StringBuilder(kMaxLineLen);
        // last_line_pos is the last writeable pos within the line array
        // (only a final '\0' may go here).
        int line_pos = 0;
        int line_num = 0;
        int bom_pos = 0;
        boolean last_was_carriage_return = false;
        handler_.HandleRobotsStart();


        for (char ch : robots_body_.toCharArray()) {
            // Google-specific optimization: UTF-8 byte order marks should never
            // appear in a robots.txt file, but they do nevertheless. Skipping
            // possible BOM-prefix in the first bytes of the input.
            if (bom_pos < utf_bom.length && ch == utf_bom[bom_pos++]) {
                continue;
            }
            bom_pos = utf_bom.length;
            if (ch != 0x0A && ch != 0x0D) {  // Non-line-ending char case.
                // Put in next spot on current line, as long as there's room.
                if (line_pos < kMaxLineLen - 1) {
                    line_buffer.append(ch);
                    ++line_pos;
                }
            } else {                         // Line-ending character char case.
                // Only emit an empty line if this was not due to the second character
                // of the DOS line-ending \r\n .
                boolean is_CRLF_continuation = (line_pos == 0) && last_was_carriage_return && ch == 0x0A;
                if (!is_CRLF_continuation) {
                    ParseAndEmitLine(++line_num, line_buffer.toString());
                }
                line_buffer = new StringBuilder();
                line_pos = 0;
                last_was_carriage_return = (ch == 0x0D);
            }
        }

        ParseAndEmitLine(++line_num, line_buffer.toString());
        handler_.HandleRobotsEnd();
    }

    private void ParseAndEmitLine(int current_line, String line) {
        final Map.Entry<String, String> keyAndValueFrom = GetKeyAndValueFrom(line);
        if (null == keyAndValueFrom) {
            return;
        }

        ParsedRobotsKey key = new ParsedRobotsKey();
        key.Parse(keyAndValueFrom.getKey());
        if (NeedEscapeValueForKey(key)) {
            String escaped_value = Util.MaybeEscapePattern(keyAndValueFrom.getValue());
            Util.EmitKeyValueToHandler(current_line, key, escaped_value, handler_);
        } else {
            Util.EmitKeyValueToHandler(current_line, key, keyAndValueFrom.getValue(), handler_);
        }
    }

    private boolean NeedEscapeValueForKey(ParsedRobotsKey key) {
        switch (key.Type()) {
            case USER_AGENT:
            case SITEMAP:
                return false;
            default:
                return true;
        }
    }
}
