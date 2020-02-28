package com.github.itechbear.robotstxt4j;

import java.util.Collections;
import java.util.List;

public class RobotsMatcher implements RobotsParseHandler {
    private MatchHierarchy allow_;       // Characters of 'url' matching Allow.
    private MatchHierarchy disallow_;    // Characters of 'url' matching Disallow.
    private boolean seen_global_agent_;         // True if processing global agent rules.
    private boolean seen_specific_agent_;       // True if processing our specific agent.
    private boolean ever_seen_specific_agent_;  // True if we ever saw a block for our agent.
    private boolean seen_separator_;            // True if saw any key: value pair.

    // The path we want to pattern match. Not owned and only a valid pointer
    // during the lifetime of *AllowedByRobots calls.
    private String path_;
    // The User-Agents we are interested in. Not owned and only a valid
    // pointer during the lifetime of *AllowedByRobots calls.
    private List<String> user_agents_;

    private RobotsMatchStrategy match_strategy_;

    public RobotsMatcher() {
        allow_ = new MatchHierarchy();
        disallow_ = new MatchHierarchy();
        seen_global_agent_ = false;
        seen_specific_agent_ = false;
        ever_seen_specific_agent_ = false;
        seen_separator_ = false;
        path_ = null;
        user_agents_ = null;
        match_strategy_ = new LongestMatchRobotsMatchStrategy();
    }

    // Returns true iff 'url' is allowed to be fetched by any member of the
    // "user_agents" vector. 'url' must be %-encoded according to RFC3986.
    public boolean AllowedByRobots(String robots_body,
                                   List<String> user_agents,
                                   String url) {
        // The url is not normalized (escaped, percent encoded) here because the user
        // is asked to provide it in escaped form already.
        String path = Util.GetPathParamsQuery(url);
        InitUserAgentsAndPath(user_agents, path);
        Util.ParseRobotsTxt(robots_body, this);
        return !Disallow();
    }

    // Do robots check for 'url' when there is only one user agent. 'url' must
    // be %-encoded according to RFC3986.
    public boolean OneAgentAllowedByRobots(String robots_txt,
                                           String user_agent,
                                           String url) {
        return AllowedByRobots(robots_txt, Collections.singletonList(user_agent), url);
    }

    // Verifies that the given user agent is valid to be matched against
    // robots.txt. Valid user agent strings only contain the characters
    // [a-zA-Z_-].
    protected static boolean IsValidUserAgentToObey(String user_agent) {
        return user_agent.length() > 0 && ExtractUserAgent(user_agent).equals(user_agent);
    }

    protected static String ExtractUserAgent(String user_agent) {
        // Allowed characters in user-agent are [a-zA-Z_-].
        int end = 0;
        while (end < user_agent.length() && (Util.isEnglishLetter(user_agent.charAt(end)) || user_agent.charAt(end) == '-' || user_agent.charAt(end) == '_')) {
            ++end;
        }
        return user_agent.substring(0, end);
    }


    // Returns true if we are disallowed from crawling a matching URI.
    protected boolean Disallow() {
        if (allow_.specific.priority() > 0 || disallow_.specific.priority() > 0) {
            return (disallow_.specific.priority() > allow_.specific.priority());
        }

        if (ever_seen_specific_agent_) {
            // Matching group for user-agent but either without disallow or empty one,
            // i.e. priority == 0.
            return false;
        }

        if (disallow_.global.priority() > 0 || allow_.global.priority() > 0) {
            return disallow_.global.priority() > allow_.global.priority();
        }
        return false;
    }

    // Returns true if we are disallowed from crawling a matching URI. Ignores any
    // rules specified for the default user agent, and bases its results only on
    // the specified user agents.
    protected boolean DisallowIgnoreGlobal() {
        if (allow_.specific.priority() > 0 || disallow_.specific.priority() > 0) {
            return disallow_.specific.priority() > allow_.specific.priority();
        }
        return false;
    }

    // Returns true iff, when AllowedByRobots() was called, the robots file
    // referred explicitly to one of the specified user agents.
    protected boolean ever_seen_specific_agent() {
        return ever_seen_specific_agent_;
    }

    // Returns the line that matched or 0 if none matched.
    protected int MatchingLine() {
        if (ever_seen_specific_agent_) {
            return Match.HigherPriorityMatch(disallow_.specific, allow_.specific).line();
        }
        return Match.HigherPriorityMatch(disallow_.global, allow_.global).line();
    }

    // Parse callbacks.
    // Protected because used in unittest. Never override RobotsMatcher, implement
    // googlebot::RobotsParseHandler instead.
    public void HandleRobotsStart() {
        // This is a new robots.txt file, so we need to reset all the instance member
        // variables. We do it in the same order the instance member variables are
        // declared, so it's easier to keep track of which ones we have (or maybe
        // haven't!) done.
        allow_.Clear();
        disallow_.Clear();

        seen_global_agent_ = false;
        seen_specific_agent_ = false;
        ever_seen_specific_agent_ = false;
        seen_separator_ = false;
    }

    public void HandleRobotsEnd() {

    }

    public void HandleUserAgent(int line_num, String user_agent) {
        if (seen_separator_) {
            seen_specific_agent_ = seen_global_agent_ = seen_separator_ = false;
        }

        // Google-specific optimization: a '*' followed by space and more characters
        // in a user-agent record is still regarded a global rule.
        if (user_agent.charAt(0) == '*' && (user_agent.length() == 1 || Character.isWhitespace(user_agent.charAt(1)))) {
            seen_global_agent_ = true;
        } else {
            user_agent = ExtractUserAgent(user_agent);
            for (String agent : user_agents_) {
                if (user_agent.equalsIgnoreCase(agent)) {
                    ever_seen_specific_agent_ = seen_specific_agent_ = true;
                    break;
                }
            }
        }
    }

    public void HandleAllow(int line_num, String value) {
        if (!SeenAnyAgent()) return;
        seen_separator_ = true;
        int priority = match_strategy_.MatchAllow(path_, value);
        if (priority >= 0) {
            if (seen_specific_agent_) {
                if (allow_.specific.priority() < priority) {
                    allow_.specific.Set(priority, line_num);
                }
            } else {
                assert (seen_global_agent_);
                if (allow_.global.priority() < priority) {
                    allow_.global.Set(priority, line_num);
                }
            }
        } else {
            // Google-specific optimization: 'index.htm' and 'index.html' are normalized
            // to '/'.
            int slash_pos = value.lastIndexOf('/');

            if (slash_pos >= 0 && value.substring(slash_pos).startsWith("/index.htm")) {
                int len = slash_pos + 1;
                HandleAllow(line_num, value.substring(0, len) + '$');
            }
        }
    }

    public void HandleDisallow(int line_num, String value) {
        if (!SeenAnyAgent()) return;
        seen_separator_ = true;
        int priority = match_strategy_.MatchDisallow(path_, value);
        if (priority >= 0) {
            if (seen_specific_agent_) {
                if (disallow_.specific.priority() < priority) {
                    disallow_.specific.Set(priority, line_num);
                }
            } else {
                assert (seen_global_agent_);
                if (disallow_.global.priority() < priority) {
                    disallow_.global.Set(priority, line_num);
                }
            }
        }
    }

    public void HandleSitemap(int line_num, String value) {
        seen_separator_ = true;
    }

    public void HandleUnknownAction(int line_num, String action,
                                    String value) {
        seen_separator_ = true;
    }

    protected void InitUserAgentsAndPath(List<String> user_agents,
                                         String path) {
        // The RobotsParser object doesn't own path_ or user_agents_, so overwriting
        // these pointers doesn't cause a memory leak.
        assert '/' == path.charAt(0);
        path_ = path;
        user_agents_ = user_agents;
    }

    // Returns true if any user-agent was seen.
    protected boolean SeenAnyAgent() {
        return seen_global_agent_ || seen_specific_agent_;
    }

    static class Match {
        private static final int kNoMatchPriority = -1;

        private int priority_;
        private int line_;

        public Match() {
            this.priority_ = kNoMatchPriority;
            this.line_ = 0;
        }

        public Match(int priority, int line) {
            this.priority_ = priority;
            this.line_ = line;
        }

        public static Match HigherPriorityMatch(Match a, Match b) {
            if (a.priority() > b.priority()) {
                return a;
            } else {
                return b;
            }
        }

        public void Set(int priority, int line) {
            priority_ = priority;
            line_ = line;
        }

        public void Clear() {
            Set(kNoMatchPriority, 0);
        }

        public int line() {
            return line_;
        }

        public int priority() {
            return priority_;
        }
    }


    static class MatchHierarchy {
        Match global;            // Match for '*'
        Match specific;          // Match for queried agent.

        public MatchHierarchy() {
            global = new Match();
            specific = new Match();
        }

        void Clear() {
            global.Clear();
            specific.Clear();
        }
    }
}
