package copper.loader.mod;

public class StringVersion extends Version {
    public String version;

    public StringVersion(String txt) {
        version = txt.trim();
    }

    @Override
    public String toString() {
        return version;
    }

    @Override
    public boolean equals(Version obj) {
        if (obj instanceof StringVersion ver)
            return ver.version.equals(version);
        return false;
    }
}
