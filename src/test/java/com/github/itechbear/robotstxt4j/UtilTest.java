package com.github.itechbear.robotstxt4j;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class UtilTest {
    static void TestPath(String url, String expected_path) {
        Assertions.assertEquals(expected_path, Util.GetPathParamsQuery(url));
    }

    static void TestEscape(String url, String expected) {
        String escaped_value = Util.MaybeEscapePattern(url);
        Assertions.assertEquals(expected, escaped_value);
    }

    @Test
    void getPathParamsQuery() {
        // Only testing URLs that are already correctly escaped here.
        TestPath("", "/");
        TestPath("http://www.example.com", "/");
        TestPath("http://www.example.com/", "/");
        TestPath("http://www.example.com/a", "/a");
        TestPath("http://www.example.com/a/", "/a/");
        TestPath("http://www.example.com/a/b?c=http://d.e/", "/a/b?c=http://d.e/");
        TestPath("http://www.example.com/a/b?c=d&e=f#fragment", "/a/b?c=d&e=f");
        TestPath("example.com", "/");
        TestPath("example.com/", "/");
        TestPath("example.com/a", "/a");
        TestPath("example.com/a/", "/a/");
        TestPath("example.com/a/b?c=d&e=f#fragment", "/a/b?c=d&e=f");
        TestPath("a", "/");
        TestPath("a/", "/");
        TestPath("/a", "/a");
        TestPath("a/b", "/b");
        TestPath("example.com?a", "/?a");
        TestPath("example.com/a;b#c", "/a;b");
        TestPath("//a/b/c", "/b/c");
    }

    @Test
    void maybeEscapePattern() {
        TestEscape("http://www.example.com", "http://www.example.com");
        TestEscape("/a/b/c", "/a/b/c");
        TestEscape("á", "%C3%A1");
        TestEscape("%aa", "%AA");
        TestEscape("/abc/ツ", "/abc/%E3%83%84");
    }
}