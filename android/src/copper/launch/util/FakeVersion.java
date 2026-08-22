package copper.launch.util;

/**
 * Stand-in for {@code java.lang.Runtime.Version}, which does not exist on Android.
 *
 * <p>Only the {@code major} number is kept meaningful: {@link #current()} always
 * reports 17 (the java level the builder compiles with), so code that checks
 * {@code Runtime.version().major() >= N} behaves like a modern JVM.</p>
 */
public class FakeVersion implements Comparable<FakeVersion> {
    public int major;

    public FakeVersion() {}

    /**
     * Parses a version string like {@code "17.0.1"}.
     * Special-cases {@code 1.x} legacy versions to use the second number
     * (e.g. {@code "1.8.0"} → 8). Falls back to 17 on any parse failure.
     */
    public static FakeVersion parse(String s) {
        FakeVersion v = new FakeVersion();
        try {
            int dot1 = s.indexOf('.');
            String mainPart = (dot1 == -1) ? s : s.substring(0, dot1);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mainPart.length(); i++) {
                char c = mainPart.charAt(i);
                if (c >= '0' && c <= '9') sb.append(c);
            }
            int m = Integer.parseInt(sb.toString());

            if (m == 1) {
                int dot2 = s.indexOf('.', dot1 + 1);
                String sub = (dot2 == -1) ? s.substring(dot1 + 1) : s.substring(dot1 + 1, dot2);
                StringBuilder sb2 = new StringBuilder();
                for (int i = 0; i < sub.length(); i++) {
                    char c = sub.charAt(i);
                    if (c >= '0' && c <= '9') sb2.append(c);
                }
                m = Integer.parseInt(sb2.toString());
            }
            v.major = m;
        } catch (Exception e) {
            v.major = 17;
        }
        return v;
    }

    /** The version of the current runtime: always java 17. */
    public static FakeVersion current() {
        FakeVersion v = new FakeVersion();
        v.major = 17;
        return v;
    }

    public int feature() { return major; }
    public int major() { return major; }

    @Override
    public int compareTo(FakeVersion other) {
        if (this.major < other.major) return -1;
        if (this.major > other.major) return 1;
        return 0;
    }
}