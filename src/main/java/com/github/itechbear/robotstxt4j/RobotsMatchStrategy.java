package com.github.itechbear.robotstxt4j;

public abstract class RobotsMatchStrategy {
    protected static boolean Matches(String path, String pattern) {
        int pathlen = path.length();
        // bsl::FixedArray<size_t> pos(pathlen + 1);
        int[] pos = new int[pathlen + 1];
        int numpos;

        // The pos[] array holds a sorted list of indexes of 'path', with length
        // 'numpos'.  At the start and end of each iteration of the main loop below,
        // the pos[] array will hold a list of the prefixes of the 'path' which can
        // match the current prefix of 'pattern'. If this list is ever empty,
        // return false. If we reach the end of 'pattern' with at least one element
        // in pos[], return true.

        pos[0] = 0;
        numpos = 1;

        for (int j = 0; j < pattern.length(); ++j) {
            if (pattern.charAt(j) == '$' && j == pattern.length() - 1) {
                return (pos[numpos - 1] == pathlen);
            }
            if (pattern.charAt(j) == '*') {
                numpos = pathlen - pos[0] + 1;
                for (int i = 1; i < numpos; i++) {
                    pos[i] = pos[i - 1] + 1;
                }
            } else {
                // Includes '$' when not at end of pattern.
                int newnumpos = 0;
                for (int i = 0; i < numpos; i++) {
                    if (pos[i] < pathlen && path.charAt(pos[i]) == pattern.charAt(j)) {
                        pos[newnumpos++] = pos[i] + 1;
                    }
                }
                numpos = newnumpos;
                if (numpos == 0) return false;
            }
        }

        return true;
    }

    abstract public int MatchAllow(String path, String pattern);

    abstract public int MatchDisallow(String path, String pattern);
}
