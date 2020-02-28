package com.github.itechbear.robotstxt4j;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RobotsMatcherTest {
    public static boolean IsUserAgentAllowed(String robotstxt,
                                             String useragent,
                                             String url) {
        RobotsMatcher matcher = new RobotsMatcher();
        return matcher.OneAgentAllowedByRobots(robotstxt, useragent, url);
    }

    // Google-specific: system test.
    @Test
    public void GoogleOnly_SystemTest() {
        String robotstxt = "user-agent: FooBot\n" +
                "disallow: /\n";
        // Empty robots.txt: everything allowed.
        Assertions.assertTrue(IsUserAgentAllowed("", "FooBot", ""));

        // Empty user-agent to be matched: everything allowed.
        Assertions.assertTrue(IsUserAgentAllowed(robotstxt, "", ""));

        // Empty url: implicitly disallowed, see method comment for GetPathParamsQuery
        // in robots.cc.
        Assertions.assertFalse(IsUserAgentAllowed(robotstxt, "FooBot", ""));

        // All params empty: same as robots.txt empty, everything allowed.
        Assertions.assertTrue(IsUserAgentAllowed("", "", ""));
    }


    // Rules are colon separated name-value pairs. The following names are
    // provisioned:
    //     user-agent: <value>
    //     allow: <value>
    //     disallow: <value>
    // See REP I-D section "Protocol Definition".
    // https://tools.ietf.org/html/draft-koster-rep#section-2.1
    //
    // Google specific: webmasters sometimes miss the colon separator, but it's
    // obvious what they mean by "disallow /", so we assume the colon if it's
    // missing.
    @Test
    public void ID_LineSyntax_Line() {
        String robotstxt_correct = "user-agent: FooBot\n" +
                "disallow: /\n";
        String robotstxt_incorrect = "foo: FooBot\n" +
                "bar: /\n";
        String robotstxt_incorrect_accepted = "user-agent FooBot\n" +
                "disallow /\n";
        String url = "http://foo.bar/x/y";

        Assertions.assertFalse(IsUserAgentAllowed(robotstxt_correct, "FooBot", url));
        Assertions.assertTrue(IsUserAgentAllowed(robotstxt_incorrect, "FooBot", url));
        Assertions.assertFalse(IsUserAgentAllowed(robotstxt_incorrect_accepted, "FooBot", url));
    }

    // A group is one or more user-agent line followed by rules, and terminated
    // by a another user-agent line. Rules for same user-agents are combined
    // opaquely into one group. Rules outside groups are ignored.
    // See REP I-D section "Protocol Definition".
    // https://tools.ietf.org/html/draft-koster-rep#section-2.1
    @Test
    public void ID_LineSyntax_Groups() {
        String robotstxt =
                "allow: /foo/bar/\n" +
                        "\n" +
                        "user-agent: FooBot\n" +
                        "disallow: /\n" +
                        "allow: /x/\n" +
                        "user-agent: BarBot\n" +
                        "disallow: /\n" +
                        "allow: /y/\n" +
                        "\n" +
                        "\n" +
                        "allow: /w/\n" +
                        "user-agent: BazBot\n" +
                        "\n" +
                        "user-agent: FooBot\n" +
                        "allow: /z/\n" +
                        "disallow: /\n";

        String url_w = "http://foo.bar/w/a";
        String url_x = "http://foo.bar/x/b";
        String url_y = "http://foo.bar/y/c";
        String url_z = "http://foo.bar/z/d";
        String url_foo = "http://foo.bar/foo/bar/";

        Assertions.assertTrue(IsUserAgentAllowed(robotstxt, "FooBot", url_x));
        Assertions.assertTrue(IsUserAgentAllowed(robotstxt, "FooBot", url_z));
        Assertions.assertFalse(IsUserAgentAllowed(robotstxt, "FooBot", url_y));
        Assertions.assertTrue(IsUserAgentAllowed(robotstxt, "BarBot", url_y));
        Assertions.assertTrue(IsUserAgentAllowed(robotstxt, "BarBot", url_w));
        Assertions.assertFalse(IsUserAgentAllowed(robotstxt, "BarBot", url_z));
        Assertions.assertTrue(IsUserAgentAllowed(robotstxt, "BazBot", url_z));

        // Lines with rules outside groups are ignored.
        Assertions.assertFalse(IsUserAgentAllowed(robotstxt, "FooBot", url_foo));
        Assertions.assertFalse(IsUserAgentAllowed(robotstxt, "BarBot", url_foo));
        Assertions.assertFalse(IsUserAgentAllowed(robotstxt, "BazBot", url_foo));
    }

    // REP lines are case insensitive. See REP I-D section "Protocol Definition".
    // https://tools.ietf.org/html/draft-koster-rep#section-2.1
    @Test
    public void ID_REPLineNamesCaseInsensitive() {
        String robotstxt_upper =
                "USER-AGENT: FooBot\n" +
                        "ALLOW: /x/\n" +
                        "DISALLOW: /\n";
        String robotstxt_lower =
                "user-agent: FooBot\n" +
                        "allow: /x/\n" +
                        "disallow: /\n";
        String robotstxt_camel =
                "uSeR-aGeNt: FooBot\n" +
                        "AlLoW: /x/\n" +
                        "dIsAlLoW: /\n";
        String url_allowed = "http://foo.bar/x/y";
        String url_disallowed = "http://foo.bar/a/b";

        Assertions.assertTrue(IsUserAgentAllowed(robotstxt_upper, "FooBot", url_allowed));
        Assertions.assertTrue(IsUserAgentAllowed(robotstxt_lower, "FooBot", url_allowed));
        Assertions.assertTrue(IsUserAgentAllowed(robotstxt_camel, "FooBot", url_allowed));
        Assertions.assertFalse(IsUserAgentAllowed(robotstxt_upper, "FooBot", url_disallowed));
        Assertions.assertFalse(IsUserAgentAllowed(robotstxt_lower, "FooBot", url_disallowed));
        Assertions.assertFalse(IsUserAgentAllowed(robotstxt_camel, "FooBot", url_disallowed));
    }

    // A user-agent line is expected to contain only [a-zA-Z_-] characters and must
    // not be empty. See REP I-D section "The user-agent line".
    // https://tools.ietf.org/html/draft-koster-rep#section-2.2.1
    @Test
    public void ID_VerifyValidUserAgentsToObey() {
        //Assertions.assertTrue(RobotsMatcher.IsValidUserAgentToObey("Foobot"));
        //Assertions.assertTrue(RobotsMatcher.IsValidUserAgentToObey("Foobot-Bar"));
        //Assertions.assertTrue(RobotsMatcher.IsValidUserAgentToObey("Foo_Bar"));
        //
        //Assertions.assertFalse(RobotsMatcher.IsValidUserAgentToObey(new String()));
        //Assertions.assertFalse(RobotsMatcher.IsValidUserAgentToObey(""));
        Assertions.assertFalse(RobotsMatcher.IsValidUserAgentToObey("ツ"));

        //Assertions.assertFalse(RobotsMatcher.IsValidUserAgentToObey("Foobot*"));
        //Assertions.assertFalse(RobotsMatcher.IsValidUserAgentToObey(" Foobot "));
        //Assertions.assertFalse(RobotsMatcher.IsValidUserAgentToObey("Foobot/2.1"));
        //
        //Assertions.assertFalse(RobotsMatcher.IsValidUserAgentToObey("Foobot Bar"));
    }

    // User-agent line values are case insensitive. See REP I-D section "The
    // user-agent line".
    // https://tools.ietf.org/html/draft-koster-rep#section-2.2.1
    @Test
    public void ID_UserAgentValueCaseInsensitive() {
        String robotstxt_upper =
                "User-Agent: FOO BAR\n" +
                        "Allow: /x/\n" +
                        "Disallow: /\n";
        String robotstxt_lower =
                "User-Agent: foo bar\n" +
                        "Allow: /x/\n" +
                        "Disallow: /\n";
        String robotstxt_camel =
                "User-Agent: FoO bAr\n" +
                        "Allow: /x/\n" +
                        "Disallow: /\n";
        String url_allowed = "http://foo.bar/x/y";
        String url_disallowed = "http://foo.bar/a/b";

        Assertions.assertTrue(IsUserAgentAllowed(robotstxt_upper, "Foo", url_allowed));
        Assertions.assertTrue(IsUserAgentAllowed(robotstxt_lower, "Foo", url_allowed));
        Assertions.assertTrue(IsUserAgentAllowed(robotstxt_camel, "Foo", url_allowed));
        Assertions.assertFalse(IsUserAgentAllowed(robotstxt_upper, "Foo", url_disallowed));
        Assertions.assertFalse(IsUserAgentAllowed(robotstxt_lower, "Foo", url_disallowed));
        Assertions.assertFalse(IsUserAgentAllowed(robotstxt_camel, "Foo", url_disallowed));
        Assertions.assertTrue(IsUserAgentAllowed(robotstxt_upper, "foo", url_allowed));
        Assertions.assertTrue(IsUserAgentAllowed(robotstxt_lower, "foo", url_allowed));
        Assertions.assertTrue(IsUserAgentAllowed(robotstxt_camel, "foo", url_allowed));
        Assertions.assertFalse(IsUserAgentAllowed(robotstxt_upper, "foo", url_disallowed));
        Assertions.assertFalse(IsUserAgentAllowed(robotstxt_lower, "foo", url_disallowed));
        Assertions.assertFalse(IsUserAgentAllowed(robotstxt_camel, "foo", url_disallowed));
    }

    // Google specific: accept user-agent value up to the first space. Space is not
    // allowed in user-agent values, but that doesn't stop webmasters from using
    // them. This is more restrictive than the I-D, since in case of the bad value
    // "Googlebot Images" we'd still obey the rules with "Googlebot".
    // Extends REP I-D section "The user-agent line"
    // https://tools.ietf.org/html/draft-koster-rep#section-2.2.1
    @Test
    public void GoogleOnly_AcceptUserAgentUpToFirstSpace() {
        Assertions.assertFalse(RobotsMatcher.IsValidUserAgentToObey("Foobot Bar"));
        String robotstxt =
                "User-Agent: *\n" +
                        "Disallow: /\n" +
                        "User-Agent: Foo Bar\n" +
                        "Allow: /x/\n" +
                        "Disallow: /\n";
        String url = "http://foo.bar/x/y";

        Assertions.assertTrue(IsUserAgentAllowed(robotstxt, "Foo", url));
        Assertions.assertFalse(IsUserAgentAllowed(robotstxt, "Foo Bar", url));
    }

    // If no group matches the user-agent, crawlers must obey the first group with a
    // user-agent line with a "*" value, if present. If no group satisfies either
    // condition, or no groups are present at all, no rules apply.
    // See REP I-D section "The user-agent line".
    // https://tools.ietf.org/html/draft-koster-rep#section-2.2.1
    @Test
    public void ID_GlobalGroups_Secondary() {
        String robotstxt_empty = "";
        String robotstxt_global =
                "user-agent: *\n" +
                        "allow: /\n" +
                        "user-agent: FooBot\n" +
                        "disallow: /\n";
        String robotstxt_only_specific =
                "user-agent: FooBot\n" +
                        "allow: /\n" +
                        "user-agent: BarBot\n" +
                        "disallow: /\n" +
                        "user-agent: BazBot\n" +
                        "disallow: /\n";
        String url = "http://foo.bar/x/y";

        Assertions.assertTrue(IsUserAgentAllowed(robotstxt_empty, "FooBot", url));
        Assertions.assertFalse(IsUserAgentAllowed(robotstxt_global, "FooBot", url));
        Assertions.assertTrue(IsUserAgentAllowed(robotstxt_global, "BarBot", url));
        Assertions.assertTrue(IsUserAgentAllowed(robotstxt_only_specific, "QuxBot", url));
    }

    // Matching rules against URIs is case sensitive.
    // See REP I-D section "The Allow and Disallow lines".
    // https://tools.ietf.org/html/draft-koster-rep#section-2.2.2
    @Test
    public void ID_AllowDisallow_Value_CaseSensitive() {
        String robotstxt_lowercase_url =
                "user-agent: FooBot\n" +
                        "disallow: /x/\n";
        String robotstxt_uppercase_url =
                "user-agent: FooBot\n" +
                        "disallow: /X/\n";
        String url = "http://foo.bar/x/y";

        Assertions.assertFalse(IsUserAgentAllowed(robotstxt_lowercase_url, "FooBot", url));
        Assertions.assertTrue(IsUserAgentAllowed(robotstxt_uppercase_url, "FooBot", url));
    }

    // The most specific match found MUST be used. The most specific match is the
    // match that has the most octets. In case of multiple rules with the same
    // length, the least strict rule must be used.
    // See REP I-D section "The Allow and Disallow lines".
    // https://tools.ietf.org/html/draft-koster-rep#section-2.2.2
    @Test
    public void ID_LongestMatch() {
        String url = "http://foo.bar/x/page.html";
        {
            String robotstxt =
                    "user-agent: FooBot\n" +
                            "disallow: /x/page.html\n" +
                            "allow: /x/\n";

            Assertions.assertFalse(IsUserAgentAllowed(robotstxt, "FooBot", url));
        }
        {
            String robotstxt =
                    "user-agent: FooBot\n" +
                            "allow: /x/page.html\n" +
                            "disallow: /x/\n";

            Assertions.assertTrue(IsUserAgentAllowed(robotstxt, "FooBot", url));
            Assertions.assertFalse(IsUserAgentAllowed(robotstxt, "FooBot", "http://foo.bar/x/"));
        }
        {
            String robotstxt =
                    "user-agent: FooBot\n" +
                            "disallow: \n" +
                            "allow: \n";
            // In case of equivalent disallow and allow patterns for the same
            // user-agent, allow is used.
            Assertions.assertTrue(IsUserAgentAllowed(robotstxt, "FooBot", url));
        }
        {
            String robotstxt =
                    "user-agent: FooBot\n" +
                            "disallow: /\n" +
                            "allow: /\n";
            // In case of equivalent disallow and allow patterns for the same
            // user-agent, allow is used.
            Assertions.assertTrue(IsUserAgentAllowed(robotstxt, "FooBot", url));
        }
        {
            String url_a = "http://foo.bar/x";
            String url_b = "http://foo.bar/x/";
            String robotstxt =
                    "user-agent: FooBot\n" +
                            "disallow: /x\n" +
                            "allow: /x/\n";
            Assertions.assertFalse(IsUserAgentAllowed(robotstxt, "FooBot", url_a));
            Assertions.assertTrue(IsUserAgentAllowed(robotstxt, "FooBot", url_b));
        }

        {
            String robotstxt =
                    "user-agent: FooBot\n" +
                            "disallow: /x/page.html\n" +
                            "allow: /x/page.html\n";
            // In case of equivalent disallow and allow patterns for the same
            // user-agent, allow is used.
            Assertions.assertTrue(IsUserAgentAllowed(robotstxt, "FooBot", url));
        }
        {
            String robotstxt =
                    "user-agent: FooBot\n" +
                            "allow: /page\n" +
                            "disallow: /*.html\n";
            // Longest match wins.
            Assertions.assertFalse(
                    IsUserAgentAllowed(robotstxt, "FooBot", "http://foo.bar/page.html"));
            Assertions.assertTrue(
                    IsUserAgentAllowed(robotstxt, "FooBot", "http://foo.bar/page"));
        }
        {
            String robotstxt =
                    "user-agent: FooBot\n" +
                            "allow: /x/page.\n" +
                            "disallow: /*.html\n";
            // Longest match wins.
            Assertions.assertTrue(IsUserAgentAllowed(robotstxt, "FooBot", url));
            Assertions.assertFalse(
                    IsUserAgentAllowed(robotstxt, "FooBot", "http://foo.bar/x/y.html"));
        }
        {
            String robotstxt =
                    "User-agent: *\n" +
                            "Disallow: /x/\n" +
                            "User-agent: FooBot\n" +
                            "Disallow: /y/\n";
            // Most specific group for FooBot allows implicitly /x/page.
            Assertions.assertTrue(
                    IsUserAgentAllowed(robotstxt, "FooBot", "http://foo.bar/x/page"));
            Assertions.assertFalse(
                    IsUserAgentAllowed(robotstxt, "FooBot", "http://foo.bar/y/page"));
        }
    }

    // Octets in the URI and robots.txt paths outside the range of the US-ASCII
    // coded character set, and those in the reserved range defined by RFC3986,
    // MUST be percent-encoded as defined by RFC3986 prior to comparison.
    // See REP I-D section "The Allow and Disallow lines".
    // https://tools.ietf.org/html/draft-koster-rep#section-2.2.2
    //
    // NOTE: It's up to the caller to percent encode a URL before passing it to the
    // parser. Percent encoding URIs in the rules is unnecessary.
    @Test
    public void ID_Encoding() {
        // /foo/bar?baz=http://foo.bar stays unencoded.
        {
            String robotstxt =
                    "User-agent: FooBot\n" +
                            "Disallow: /\n" +
                            "Allow: /foo/bar?qux=taz&baz=http://foo.bar?tar&par\n";
            Assertions.assertTrue(IsUserAgentAllowed(
                    robotstxt, "FooBot",
                    "http://foo.bar/foo/bar?qux=taz&baz=http://foo.bar?tar&par"));
        }

        // 3 byte character: /foo/bar/ツ -> /foo/bar/%E3%83%84
        {
            String robotstxt =
                    "User-agent: FooBot\n" +
                            "Disallow: /\n" +
                            "Allow: /foo/bar/ツ\n";
            Assertions.assertTrue(IsUserAgentAllowed(robotstxt, "FooBot",
                    "http://foo.bar/foo/bar/%E3%83%84"));
            // The parser encodes the 3-byte character, but the URL is not %-encoded.
            Assertions.assertFalse(
                    IsUserAgentAllowed(robotstxt, "FooBot", "http://foo.bar/foo/bar/ツ"));
        }
        // Percent encoded 3 byte character: /foo/bar/%E3%83%84 -> /foo/bar/%E3%83%84
        {
            String robotstxt =
                    "User-agent: FooBot\n" +
                            "Disallow: /\n" +
                            "Allow: /foo/bar/%E3%83%84\n";
            Assertions.assertTrue(IsUserAgentAllowed(robotstxt, "FooBot",
                    "http://foo.bar/foo/bar/%E3%83%84"));
            Assertions.assertFalse(
                    IsUserAgentAllowed(robotstxt, "FooBot", "http://foo.bar/foo/bar/ツ"));
        }
        // Percent encoded unreserved US-ASCII: /foo/bar/%62%61%7A -> NULL
        // This is illegal according to RFC3986 and while it may work here due to
        // simple string matching, it should not be relied on.
        {
            String robotstxt =
                    "User-agent: FooBot\n" +
                            "Disallow: /\n" +
                            "Allow: /foo/bar/%62%61%7A\n";
            Assertions.assertFalse(
                    IsUserAgentAllowed(robotstxt, "FooBot", "http://foo.bar/foo/bar/baz"));
            Assertions.assertTrue(IsUserAgentAllowed(robotstxt, "FooBot",
                    "http://foo.bar/foo/bar/%62%61%7A"));
        }
    }

    // The REP I-D defines the following characters that have special meaning in
    // robots.txt:
    // # - inline comment.
    // $ - end of pattern.
    // * - any number of characters.
    // See REP I-D section "Special Characters".
    // https://tools.ietf.org/html/draft-koster-rep#section-2.2.3
    @Test
    public void ID_SpecialCharacters() {
        {
            String robotstxt =
                    "User-agent: FooBot\n" +
                            "Disallow: /foo/bar/quz\n" +
                            "Allow: /foo/*/qux\n";
            Assertions.assertFalse(
                    IsUserAgentAllowed(robotstxt, "FooBot", "http://foo.bar/foo/bar/quz"));
            Assertions.assertTrue(
                    IsUserAgentAllowed(robotstxt, "FooBot", "http://foo.bar/foo/quz"));
            Assertions.assertTrue(
                    IsUserAgentAllowed(robotstxt, "FooBot", "http://foo.bar/foo//quz"));
            Assertions.assertTrue(
                    IsUserAgentAllowed(robotstxt, "FooBot", "http://foo.bar/foo/bax/quz"));
        }
        {
            String robotstxt =
                    "User-agent: FooBot\n" +
                            "Disallow: /foo/bar$\n" +
                            "Allow: /foo/bar/qux\n";
            Assertions.assertFalse(
                    IsUserAgentAllowed(robotstxt, "FooBot", "http://foo.bar/foo/bar"));
            Assertions.assertTrue(
                    IsUserAgentAllowed(robotstxt, "FooBot", "http://foo.bar/foo/bar/qux"));
            Assertions.assertTrue(
                    IsUserAgentAllowed(robotstxt, "FooBot", "http://foo.bar/foo/bar/"));
            Assertions.assertTrue(
                    IsUserAgentAllowed(robotstxt, "FooBot", "http://foo.bar/foo/bar/baz"));
        }
        {
            String robotstxt =
                    "User-agent: FooBot\n" +
                            "# Disallow: /\n" +
                            "Disallow: /foo/quz#qux\n" +
                            "Allow: /\n";
            Assertions.assertTrue(
                    IsUserAgentAllowed(robotstxt, "FooBot", "http://foo.bar/foo/bar"));
            Assertions.assertFalse(
                    IsUserAgentAllowed(robotstxt, "FooBot", "http://foo.bar/foo/quz"));
        }
    }

    // Google-specific: "index.html" (and only that) at the end of a pattern is
    // equivalent to "/".
    @Test
    public void GoogleOnly_IndexHTMLisDirectory() {
        String robotstxt =
                "User-Agent: *\n" +
                        "Allow: /allowed-slash/index.html\n" +
                        "Disallow: /\n";
        // If index.html is allowed, we interpret this as / being allowed too.
        Assertions.assertTrue(
                IsUserAgentAllowed(robotstxt, "foobot", "http://foo.com/allowed-slash/"));
        // Does not exatly match.
        Assertions.assertFalse(IsUserAgentAllowed(robotstxt, "foobot",
                "http://foo.com/allowed-slash/index.htm"));
        // Exact match.
        Assertions.assertTrue(IsUserAgentAllowed(robotstxt, "foobot",
                "http://foo.com/allowed-slash/index.html"));
        Assertions.assertFalse(
                IsUserAgentAllowed(robotstxt, "foobot", "http://foo.com/anyother-url"));
    }

    // Google-specific: long lines are ignored after 8 * 2083 bytes. See comment in
    // RobotsTxtParser::Parse().
    @Test
    public void GoogleOnly_LineTooLong() {
        int kEOLLen = "\n".length();
        int kMaxLineLen = 2083 * 8;
        String allow = "allow: ";
        String disallow = "disallow: ";

        // Disallow rule pattern matches the URL after being cut off at kMaxLineLen.
        {
            StringBuilder robotstxt = new StringBuilder("user-agent: FooBot\n");
            StringBuilder longline = new StringBuilder("/x/");
            int max_length = kMaxLineLen - longline.length() - disallow.length() + kEOLLen;
            while (longline.length() < max_length) {
                longline.append('a');
            }
            robotstxt.append(disallow).append(longline).append("/qux\n");

            // Matches nothing, so URL is allowed.
            Assertions.assertTrue(IsUserAgentAllowed(robotstxt.toString(), "FooBot", "http://foo.bar/fux"));
            // Matches cut off disallow rule.
            Assertions.assertFalse(IsUserAgentAllowed(robotstxt.toString(), "FooBot", "http://foo.bar" + longline + "/fux"));
        }

        {
            StringBuilder robotstxt = new StringBuilder("user-agent: FooBot\n" +
                    "disallow: /\n");
            StringBuilder longline_a = new StringBuilder("/x/");
            StringBuilder longline_b = new StringBuilder("/x/");
            int max_length =
                    kMaxLineLen - longline_a.length() - allow.length() + kEOLLen;
            while (longline_a.length() < max_length) {
                longline_a.append('a');
                longline_b.append('b');
            }
            robotstxt.append(allow).append(longline_a).append("/qux\n");
            robotstxt.append(allow).append(longline_b).append("/qux\n");

            // URL matches the disallow rule.
            Assertions.assertFalse(IsUserAgentAllowed(robotstxt.toString(), "FooBot", "http://foo.bar/"));
            // Matches the allow rule exactly.
            Assertions.assertTrue(
                    IsUserAgentAllowed(robotstxt.toString(), "FooBot",
                            "http://foo.bar" + longline_a + "/qux"));
            // Matches cut off allow rule.
            Assertions.assertTrue(
                    IsUserAgentAllowed(robotstxt.toString(), "FooBot",
                            "http://foo.bar" + longline_b + "/fux"));
        }
    }

    @Test
    public void GoogleOnly_DocumentationChecks() {
        // Test documentation from
        // https://developers.google.com/search/reference/robots_txt
        // Section "URL matching based on path values".
        {
            String robotstxt =
                    "user-agent: FooBot\n" +
                            "disallow: /\n" +
                            "allow: /fish\n";
            Assertions.assertFalse(IsUserAgentAllowed(robotstxt, "FooBot", "http://foo.bar/bar"));

            Assertions.assertTrue(IsUserAgentAllowed(robotstxt, "FooBot", "http://foo.bar/fish"));
            Assertions.assertTrue(
                    IsUserAgentAllowed(robotstxt, "FooBot", "http://foo.bar/fish.html"));
            Assertions.assertTrue(IsUserAgentAllowed(robotstxt, "FooBot",
                    "http://foo.bar/fish/salmon.html"));
            Assertions.assertTrue(
                    IsUserAgentAllowed(robotstxt, "FooBot", "http://foo.bar/fishheads"));
            Assertions.assertTrue(IsUserAgentAllowed(robotstxt, "FooBot",
                    "http://foo.bar/fishheads/yummy.html"));
            Assertions.assertTrue(IsUserAgentAllowed(robotstxt, "FooBot",
                    "http://foo.bar/fish.html?id=anything"));

            Assertions.assertFalse(
                    IsUserAgentAllowed(robotstxt, "FooBot", "http://foo.bar/Fish.asp"));
            Assertions.assertFalse(
                    IsUserAgentAllowed(robotstxt, "FooBot", "http://foo.bar/catfish"));
            Assertions.assertFalse(
                    IsUserAgentAllowed(robotstxt, "FooBot", "http://foo.bar/?id=fish"));
        }
        // "/fish*" equals "/fish"
        {
            String robotstxt =
                    "user-agent: FooBot\n" +
                            "disallow: /\n" +
                            "allow: /fish*\n";
            Assertions.assertFalse(
                    IsUserAgentAllowed(robotstxt, "FooBot", "http://foo.bar/bar"));

            Assertions.assertTrue(IsUserAgentAllowed(robotstxt, "FooBot", "http://foo.bar/fish"));
            Assertions.assertTrue(
                    IsUserAgentAllowed(robotstxt, "FooBot", "http://foo.bar/fish.html"));
            Assertions.assertTrue(IsUserAgentAllowed(robotstxt, "FooBot",
                    "http://foo.bar/fish/salmon.html"));
            Assertions.assertTrue(
                    IsUserAgentAllowed(robotstxt, "FooBot", "http://foo.bar/fishheads"));
            Assertions.assertTrue(IsUserAgentAllowed(robotstxt, "FooBot",
                    "http://foo.bar/fishheads/yummy.html"));
            Assertions.assertTrue(IsUserAgentAllowed(robotstxt, "FooBot",
                    "http://foo.bar/fish.html?id=anything"));

            Assertions.assertFalse(
                    IsUserAgentAllowed(robotstxt, "FooBot", "http://foo.bar/Fish.bar"));
            Assertions.assertFalse(
                    IsUserAgentAllowed(robotstxt, "FooBot", "http://foo.bar/catfish"));
            Assertions.assertFalse(
                    IsUserAgentAllowed(robotstxt, "FooBot", "http://foo.bar/?id=fish"));
        }
        // "/fish/" does not equal "/fish"
        {
            String robotstxt =
                    "user-agent: FooBot\n" +
                            "disallow: /\n" +
                            "allow: /fish/\n";
            Assertions.assertFalse(IsUserAgentAllowed(robotstxt, "FooBot", "http://foo.bar/bar"));

            Assertions.assertTrue(
                    IsUserAgentAllowed(robotstxt, "FooBot", "http://foo.bar/fish/"));
            Assertions.assertTrue(
                    IsUserAgentAllowed(robotstxt, "FooBot", "http://foo.bar/fish/salmon"));
            Assertions.assertTrue(
                    IsUserAgentAllowed(robotstxt, "FooBot", "http://foo.bar/fish/?salmon"));
            Assertions.assertTrue(IsUserAgentAllowed(robotstxt, "FooBot",
                    "http://foo.bar/fish/salmon.html"));
            Assertions.assertTrue(IsUserAgentAllowed(robotstxt, "FooBot",
                    "http://foo.bar/fish/?id=anything"));

            Assertions.assertFalse(
                    IsUserAgentAllowed(robotstxt, "FooBot", "http://foo.bar/fish"));
            Assertions.assertFalse(
                    IsUserAgentAllowed(robotstxt, "FooBot", "http://foo.bar/fish.html"));
            Assertions.assertFalse(IsUserAgentAllowed(robotstxt, "FooBot",
                    "http://foo.bar/Fish/Salmon.html"));
        }
        // "/*.php"
        {
            String robotstxt =
                    "user-agent: FooBot\n" +
                            "disallow: /\n" +
                            "allow: /*.php\n";
            Assertions.assertFalse(IsUserAgentAllowed(robotstxt, "FooBot", "http://foo.bar/bar"));

            Assertions.assertTrue(
                    IsUserAgentAllowed(robotstxt, "FooBot", "http://foo.bar/filename.php"));
            Assertions.assertTrue(IsUserAgentAllowed(robotstxt, "FooBot",
                    "http://foo.bar/folder/filename.php"));
            Assertions.assertTrue(IsUserAgentAllowed(
                    robotstxt, "FooBot", "http://foo.bar/folder/filename.php?parameters"));
            Assertions.assertTrue(IsUserAgentAllowed(robotstxt, "FooBot",
                    "http://foo.bar//folder/any.php.file.html"));
            Assertions.assertTrue(IsUserAgentAllowed(robotstxt, "FooBot",
                    "http://foo.bar/filename.php/"));
            Assertions.assertTrue(IsUserAgentAllowed(robotstxt, "FooBot",
                    "http://foo.bar/index?f=filename.php/"));
            Assertions.assertFalse(IsUserAgentAllowed(robotstxt, "FooBot",
                    "http://foo.bar/php/"));
            Assertions.assertFalse(IsUserAgentAllowed(robotstxt, "FooBot",
                    "http://foo.bar/index?php"));

            Assertions.assertFalse(
                    IsUserAgentAllowed(robotstxt, "FooBot", "http://foo.bar/windows.PHP"));
        }
        // "/*.php$"
        {
            String robotstxt =
                    "user-agent: FooBot\n" +
                            "disallow: /\n" +
                            "allow: /*.php$\n";
            Assertions.assertFalse(IsUserAgentAllowed(robotstxt, "FooBot", "http://foo.bar/bar"));

            Assertions.assertTrue(
                    IsUserAgentAllowed(robotstxt, "FooBot", "http://foo.bar/filename.php"));
            Assertions.assertTrue(IsUserAgentAllowed(robotstxt, "FooBot",
                    "http://foo.bar/folder/filename.php"));

            Assertions.assertFalse(IsUserAgentAllowed(robotstxt, "FooBot",
                    "http://foo.bar/filename.php?parameters"));
            Assertions.assertFalse(IsUserAgentAllowed(robotstxt, "FooBot",
                    "http://foo.bar/filename.php/"));
            Assertions.assertFalse(IsUserAgentAllowed(robotstxt, "FooBot",
                    "http://foo.bar/filename.php5"));
            Assertions.assertFalse(IsUserAgentAllowed(robotstxt, "FooBot",
                    "http://foo.bar/php/"));
            Assertions.assertFalse(IsUserAgentAllowed(robotstxt, "FooBot",
                    "http://foo.bar/filename?php"));
            Assertions.assertFalse(IsUserAgentAllowed(robotstxt, "FooBot",
                    "http://foo.bar/aaaphpaaa"));
            Assertions.assertFalse(
                    IsUserAgentAllowed(robotstxt, "FooBot", "http://foo.bar//windows.PHP"));
        }
        // "/fish*.php"
        {
            String robotstxt =
                    "user-agent: FooBot\n" +
                            "disallow: /\n" +
                            "allow: /fish*.php\n";
            Assertions.assertFalse(IsUserAgentAllowed(robotstxt, "FooBot", "http://foo.bar/bar"));

            Assertions.assertTrue(
                    IsUserAgentAllowed(robotstxt, "FooBot", "http://foo.bar/fish.php"));
            Assertions.assertTrue(
                    IsUserAgentAllowed(robotstxt, "FooBot",
                            "http://foo.bar/fishheads/catfish.php?parameters"));

            Assertions.assertFalse(
                    IsUserAgentAllowed(robotstxt, "FooBot", "http://foo.bar/Fish.PHP"));
        }
        // Section "Order of precedence for group-member records".
        {
            String robotstxt =
                    "user-agent: FooBot\n" +
                            "allow: /p\n" +
                            "disallow: /\n";
            String url = "http://example.com/page";
            Assertions.assertTrue(IsUserAgentAllowed(robotstxt, "FooBot", url));
        }
        {
            String robotstxt =
                    "user-agent: FooBot\n" +
                            "allow: /folder\n" +
                            "disallow: /folder\n";
            String url = "http://example.com/folder/page";
            Assertions.assertTrue(IsUserAgentAllowed(robotstxt, "FooBot", url));
        }
        {
            String robotstxt =
                    "user-agent: FooBot\n" +
                            "allow: /page\n" +
                            "disallow: /*.htm\n";
            String url = "http://example.com/page.htm";
            Assertions.assertFalse(IsUserAgentAllowed(robotstxt, "FooBot", url));
        }
        {
            String robotstxt =
                    "user-agent: FooBot\n" +
                            "allow: /$\n" +
                            "disallow: /\n";
            String url = "http://example.com/";
            String url_page = "http://example.com/page.html";
            Assertions.assertTrue(IsUserAgentAllowed(robotstxt, "FooBot", url));
            Assertions.assertFalse(IsUserAgentAllowed(robotstxt, "FooBot", url_page));
        }
    }

    public static class RobotsStatsReporter implements RobotsParseHandler {
        public void HandleRobotsStart() {
            last_line_seen_ = 0;
            valid_directives_ = 0;
            unknown_directives_ = 0;
            sitemap_ = "";
        }

        public void HandleRobotsEnd() {
        }

        public void HandleUserAgent(int line_num, String value) {
            Digest(line_num);
        }

        public void HandleAllow(int line_num, String value) {
            Digest(line_num);
        }

        public void HandleDisallow(int line_num, String value) {
            Digest(line_num);
        }

        public void HandleSitemap(int line_num, String value) {
            Digest(line_num);
            sitemap_ = value;
        }

        // Any other unrecognized name/v pairs.
        public void HandleUnknownAction(int line_num, String action,
                                        String value) {
            last_line_seen_ = line_num;
            unknown_directives_++;
        }

        int last_line_seen() {
            return last_line_seen_;
        }

        // All directives found, including unknown.
        int valid_directives() {
            return valid_directives_;
        }

        // Number of unknown directives.
        int unknown_directives() {
            return unknown_directives_;
        }

        // Parsed sitemap line.
        String sitemap() {
            return sitemap_;
        }

        private void Digest(int line_num) {
            Assertions.assertTrue(line_num >= last_line_seen_);
            last_line_seen_ = line_num;
            valid_directives_++;
        }

        int last_line_seen_ = 0;
        int valid_directives_ = 0;
        int unknown_directives_ = 0;
        String sitemap_;
    }

    ;

    // Different kinds of line endings are all supported: %x0D / %x0A / %x0D.0A
    @Test
    public void ID_LinesNumbersAreCountedCorrectly() {
        RobotsStatsReporter report = new RobotsStatsReporter();
        String kUnixFile =
                "User-Agent: foo\n" +
                        "Allow: /some/path\n" +
                        "User-Agent: bar\n" +
                        "\n" +
                        "\n" +
                        "Disallow: /\n";
        Util.ParseRobotsTxt(kUnixFile, report);
        Assertions.assertEquals(4, report.valid_directives());
        Assertions.assertEquals(6, report.last_line_seen());

        String kDosFile =
                "User-Agent: foo\r\n" +
                        "Allow: /some/path\r\n" +
                        "User-Agent: bar\r\n" +
                        "\r\n" +
                        "\r\n" +
                        "Disallow: /\r\n";
        Util.ParseRobotsTxt(kDosFile, report);
        Assertions.assertEquals(4, report.valid_directives());
        Assertions.assertEquals(6, report.last_line_seen());

        String kMacFile =
                "User-Agent: foo\r" +
                        "Allow: /some/path\r" +
                        "User-Agent: bar\r" +
                        "\r" +
                        "\r" +
                        "Disallow: /\r";
        Util.ParseRobotsTxt(kMacFile, report);
        Assertions.assertEquals(4, report.valid_directives());
        Assertions.assertEquals(6, report.last_line_seen());

        String kNoFinalNewline =
                "User-Agent: foo\n" +
                        "Allow: /some/path\n" +
                        "User-Agent: bar\n" +
                        "\n" +
                        "\n" +
                        "Disallow: /";
        Util.ParseRobotsTxt(kNoFinalNewline, report);
        Assertions.assertEquals(4, report.valid_directives());
        Assertions.assertEquals(6, report.last_line_seen());

        String kMixedFile =
                "User-Agent: foo\n" +
                        "Allow: /some/path\r\n" +
                        "User-Agent: bar\n" +
                        "\r\n" +
                        "\n" +
                        "Disallow: /";
        Util.ParseRobotsTxt(kMixedFile, report);
        Assertions.assertEquals(4, report.valid_directives());
        Assertions.assertEquals(6, report.last_line_seen());
    }

    // BOM characters are unparseable and thus skipped. The rules following the line
    // are used.
    @Test
    public void ID_UTF8ByteOrderMarkIsSkipped() {
        RobotsStatsReporter report = new RobotsStatsReporter();
        String kUtf8FileFullBOM =
                "\u00EF\u00BB\u00BF" +
                        "User-Agent: foo\n" +
                        "Allow: /AnyValue\n";
        Util.ParseRobotsTxt(kUtf8FileFullBOM, report);
        Assertions.assertEquals(2, report.valid_directives());
        Assertions.assertEquals(0, report.unknown_directives());

        // We allow as well partial ByteOrderMarks.
        String kUtf8FilePartial2BOM =
                "\u00EF\u00BB" +
                        "User-Agent: foo\n" +
                        "Allow: /AnyValue\n";
        Util.ParseRobotsTxt(kUtf8FilePartial2BOM, report);
        Assertions.assertEquals(2, report.valid_directives());
        Assertions.assertEquals(0, report.unknown_directives());

        String kUtf8FilePartial1BOM =
                "\u00EF" +
                        "User-Agent: foo\n" +
                        "Allow: /AnyValue\n";
        Util.ParseRobotsTxt(kUtf8FilePartial1BOM, report);
        Assertions.assertEquals(2, report.valid_directives());
        Assertions.assertEquals(0, report.unknown_directives());

        // If the BOM is not the right sequence, the first line looks like garbage
        // that is skipped (we essentially see "\x11\xBFUser-Agent").
        String kUtf8FileBrokenBOM =
                "\u00EF\u0011\u00BF" +
                        "User-Agent: foo\n" +
                        "Allow: /AnyValue\n";
        Util.ParseRobotsTxt(kUtf8FileBrokenBOM, report);
        Assertions.assertEquals(1, report.valid_directives());
        Assertions.assertEquals(1, report.unknown_directives());  // We get one broken line.

        // Some other messed up file: BOMs only valid in the beginning of the file.
        String kUtf8BOMSomewhereInMiddleOfFile =
                "User-Agent: foo\n" +
                        "\u00EF\u00BB\u00BF" +
                        "Allow: /AnyValue\n";
        Util.ParseRobotsTxt(kUtf8BOMSomewhereInMiddleOfFile, report);
        Assertions.assertEquals(1, report.valid_directives());
        Assertions.assertEquals(1, report.unknown_directives());
    }

    // Google specific: the I-D allows any line that crawlers might need, such as
    // sitemaps, which Google supports.
    // See REP I-D section "Other records".
    // https://tools.ietf.org/html/draft-koster-rep#section-2.2.4
    @Test
    public void ID_NonStandardLineExample_Sitemap() {
        RobotsStatsReporter report = new RobotsStatsReporter();
        {
            String sitemap_loc = "http://foo.bar/sitemap.xml";
            StringBuilder robotstxt = new StringBuilder(
                    "User-Agent: foo\n" +
                            "Allow: /some/path\n" +
                            "User-Agent: bar\n" +
                            "\n" +
                            "\n");
            robotstxt.append("Sitemap: ").append(sitemap_loc).append("\n");

            Util.ParseRobotsTxt(robotstxt.toString(), report);
            Assertions.assertEquals(sitemap_loc, report.sitemap());
        }
        // A sitemap line may appear anywhere in the file.
        {
            StringBuilder robotstxt = new StringBuilder();
            String sitemap_loc = "http://foo.bar/sitemap.xml";
            String robotstxt_temp =
                    "User-Agent: foo\n" +
                            "Allow: /some/path\n" +
                            "User-Agent: bar\n" +
                            "\n" +
                            "\n";
            robotstxt.append("Sitemap: ").append(sitemap_loc).append("\n").append(robotstxt_temp);

            Util.ParseRobotsTxt(robotstxt.toString(), report);
            Assertions.assertEquals(sitemap_loc, report.sitemap());
        }
    }
}
