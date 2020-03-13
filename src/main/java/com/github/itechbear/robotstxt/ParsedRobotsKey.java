package com.github.itechbear.robotstxt;


public class ParsedRobotsKey {
    private static final boolean kAllowFrequentTypos = true;

    private KeyType type_;

    private String key_text_;

    public ParsedRobotsKey() {
        type_ = KeyType.UNKNOWN;
    }

    private static boolean KeyIsUserAgent(String key) {
        return Util.StartsWithIgnoreCase(key, "user-agent")
                || (kAllowFrequentTypos && (Util.StartsWithIgnoreCase(key, "useragent") || Util.StartsWithIgnoreCase(key, "user agent")));
    }

    private static boolean KeyIsAllow(String key) {
        return Util.StartsWithIgnoreCase(key, "allow");
    }

    private static boolean KeyIsDisallow(String key) {
        return (
                Util.StartsWithIgnoreCase(key, "disallow") ||
                        (kAllowFrequentTypos && ((Util.StartsWithIgnoreCase(key, "dissallow")) ||
                                (Util.StartsWithIgnoreCase(key, "dissalow")) ||
                                (Util.StartsWithIgnoreCase(key, "disalow")) ||
                                (Util.StartsWithIgnoreCase(key, "diasllow")) ||
                                (Util.StartsWithIgnoreCase(key, "disallaw")))));

    }

    private static boolean KeyIsSitemap(String key) {
        return ((Util.StartsWithIgnoreCase(key, "sitemap")) ||
                (Util.StartsWithIgnoreCase(key, "site-map")));
    }

    public void Parse(String key) {
        if (KeyIsUserAgent(key)) {
            type_ = KeyType.USER_AGENT;
        } else if (KeyIsAllow(key)) {
            type_ = KeyType.ALLOW;
        } else if (KeyIsDisallow(key)) {
            type_ = KeyType.DISALLOW;
        } else if (KeyIsSitemap(key)) {
            type_ = KeyType.SITEMAP;
        } else {
            type_ = KeyType.UNKNOWN;
            key_text_ = key;
        }
    }

    // Returns the type of key.
    public KeyType Type() {
        return type_;
    }

    // If this is an unknown key, get the text.
    public String GetUnknownText() {
        assert type_ == KeyType.UNKNOWN && !key_text_.isEmpty();
        return key_text_;
    }


    public enum KeyType {
        // Generic highlevel fields.
        USER_AGENT,
        SITEMAP,

        // Fields within a user-agent.
        ALLOW,
        DISALLOW,

        // Unrecognized field; kept as-is. High number so that additions to the
        // enumeration above does not change the serialization.
        UNKNOWN,
    }
}
