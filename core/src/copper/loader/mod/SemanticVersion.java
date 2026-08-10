package copper.loader.mod;

public class SemanticVersion extends Version implements Comparable<SemanticVersion> {
    public int major;
    public int minor;
    public int patch;

    public SemanticVersion(String txt) {
        major = minor = patch = 0;
        String[] parts = txt.trim().split("\\.");
        try {
            if (parts.length >= 1)
                major = Integer.parseInt(parts[0]);
            if (parts.length >= 2)
                minor = Integer.parseInt(parts[1]);
            if (parts.length >= 3)
                patch = Integer.parseInt(parts[2]);
        } catch (Throwable e) {
            throw new RuntimeException("invalid version: " + txt);
        }
    }

    public SemanticVersion(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    @Override
    public boolean equals(Version other) {
        if (other instanceof SemanticVersion v)
            return major == v.major && minor == v.minor && patch == v.patch;
        return false;
    }

    @Override
    public int compareTo(SemanticVersion other) {
        int result = Integer.compare(major, other.major);
        if (result == 0)
            result = Integer.compare(minor, other.minor);
        if (result == 0)
            result = Integer.compare(patch, other.patch);
        return result;
    }

    @Override
    public String toString() {
        return String.format("%d.%d.%d", major, minor, patch);
    }
}
