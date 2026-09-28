package copper.loader;

import copper.loader.mod.*;
import java.io.*;
import java.util.*;

/**
 * The loader's own version, parsed from the {@code version.properties} the loader jar carries: the
 * numeric version mod dependency checks compare, plus what identifies the build it came from.
 */
public class LoaderVersion extends SemanticVersion {
    /** Whether this build is a snapshot, which never claims a version of its own. */
    public boolean snapshot = false;
    /** The commit a snapshot was built from; empty when the remote does not have that commit. */
    public String commit = "";
    /** Whether this build came from a changed tree or a local commit, so no version or commit names it. */
    public boolean custom = false;

    /**
     * Parses the properties text, which {@code :core:writeVersion} writes at build time.
     *
     * @param text the contents of {@code version.properties}
     * @throws RuntimeException if the text is unreadable or its version is not numeric
     */
    public LoaderVersion(String text) {
        this(parse(text));
    }

    private LoaderVersion(Properties properties) {
        super(properties.getProperty("version", "0.0.0"));
        snapshot = Boolean.parseBoolean(properties.getProperty("snapshot", "false"));
        commit = properties.getProperty("commit", "").trim();
        custom = Boolean.parseBoolean(properties.getProperty("custom", "false"));
    }

    /**
     * The version as the startup lines and the game title print it: {@code v0.2.0} for a release,
     * {@code snapshot+a1b2c3d} for a snapshot built from a commit the remote has, and
     * {@code snapshot+custom} when it was not. A snapshot never prints the numeric version, which
     * exists for mod dependency checks alone.
     */
    public String versionLabel() {
        if (!snapshot)
            return "v" + this;
        if (custom)
            return "snapshot+custom";
        return commit.isEmpty() ? "snapshot" : "snapshot+" + commit;
    }

    /** Reads the text into a {@link Properties} object, the shape the other constructor works on. */
    private static Properties parse(String text) {
        Properties properties = new Properties();
        try {
            properties.load(new StringReader(text));
        } catch (IOException e) {
            throw new RuntimeException("failed to parse version properties", e);
        }
        return properties;
    }
}
