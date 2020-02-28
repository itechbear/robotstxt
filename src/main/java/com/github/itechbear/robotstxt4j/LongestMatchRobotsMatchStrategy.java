package com.github.itechbear.robotstxt4j;

public class LongestMatchRobotsMatchStrategy extends RobotsMatchStrategy {
    public LongestMatchRobotsMatchStrategy() {
    }

    public int MatchAllow(String path, String pattern) {
        return Matches(path, pattern) ? pattern.length() : -1;
    }

    public int MatchDisallow(String path, String pattern) {
        return Matches(path, pattern) ? pattern.length() : -1;
    }
}
