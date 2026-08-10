package copper.loader.mod;

public abstract class Version {
    public String toString() {
        return "";
    }

    public abstract boolean equals(Version version);

    public boolean equals(Object other) {
        if (other instanceof Version version)
            return equals(version);
        return false;
    }
}
