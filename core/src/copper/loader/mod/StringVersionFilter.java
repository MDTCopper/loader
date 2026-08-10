package copper.loader.mod;

import java.util.*;

public class StringVersionFilter implements IVersionFilter {
    public ArrayList<String> versions;

    public StringVersionFilter(ArrayList<String> versions) {
        this.versions = versions;
    }

    public StringVersionFilter(String version) {
        this();
        versions.add(version.trim());
    }

    public StringVersionFilter() {
        this(new ArrayList<>());
    }

    @Override
    public boolean check(Version v) {
        String ver = v.toString().trim();
        for (String support : versions)
            if (support.trim().equals(ver))
                return true;
        return false;
    }
}
