package copper.loader.util;

/**
 * Simple wildcard pattern matcher.
 *
 * <p>Supports:
 * <ul>
 *   <li>{@code *} — matches any sequence of characters (including empty)</li>
 *   <li>{@code ?} — matches exactly one character</li>
 * </ul>
 * Consecutive stars are compressed to a single star.
 */
public class WildcardPattern {
    private final String pattern;

    /**
     * @param pattern the wildcard pattern string
     */
    public WildcardPattern(String pattern) {
        this.pattern = compress(pattern == null ? "" : pattern);
    }

    /** Compresses consecutive {@code *} into a single {@code *}. */
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

    /**
     * Tests whether the given text matches this pattern.
     *
     * @param text the string to test (null treated as empty)
     * @return {@code true} if the text matches
     */
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

        // Consume trailing stars.
        while (j < p.length() && p.charAt(j) == '*') {
            j++;
        }
        return j == p.length();
    }
}
