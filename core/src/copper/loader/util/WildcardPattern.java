package copper.loader.util;

public class WildcardPattern {
    private final String pattern;

    public WildcardPattern(String pattern) {
        this.pattern = compress(pattern == null ? "" : pattern);
    }

    private static String compress(String pattern) {
        if (pattern.isEmpty()) {
            return pattern;
        }
        StringBuilder sb = new StringBuilder();
        boolean lastStar = false;
        for (char c : pattern.toCharArray()) {
            if (c == '*') {
                if (!lastStar) {
                    sb.append('*');
                    lastStar = true;
                }
            } else {
                sb.append(c);
                lastStar = false;
            }
        }
        return sb.toString();
    }

    public boolean match(String text) {
        if (text == null) {
            text = "";
        }
        final String p = this.pattern;
        int i = 0, j = 0;
        int starIdx = -1;
        int matchIdx = 0;

        while (i < text.length()) {
            if (j < p.length() && (p.charAt(j) == '?' || p.charAt(j) == text.charAt(i))) {
                i++;
                j++;
            } else if (j < p.length() && p.charAt(j) == '*') {
                starIdx = j;
                matchIdx = i;
                j++;
            } else if (starIdx != -1) {
                j = starIdx + 1;
                matchIdx++;
                i = matchIdx;
            } else {
                return false;
            }
        }

        while (j < p.length() && p.charAt(j) == '*') {
            j++;
        }
        return j == p.length();
    }
}
