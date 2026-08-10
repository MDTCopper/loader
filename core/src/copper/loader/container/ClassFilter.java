package copper.loader.container;

import copper.loader.util.*;
import java.util.*;

public class ClassFilter {
    protected ArrayList<Rule> rules;

    public ClassFilter() {
        rules = new ArrayList<>();
    }

    public void addRule(String txt) {
        rules.add(new Rule(txt));
    }

    public void clearRules() {
        rules.clear();
    }

    public boolean check(String clazz) {
        if (rules.isEmpty())
            return false;
        clazz = clazz.replace('/', '.');
        for (Rule rule : rules)
            if (rule.match(clazz))
                return rule.type == Rule.Type.Include;
        return false;
    }

    protected static class Rule {
        public Type type;
        public WildcardPattern pattern;

        public Rule(String rule) {
            boolean valid = false;
            try {
                String[] parts = rule.trim().split(" ");
                if (parts.length != 2)
                    return;

                if (parts[0].isEmpty())
                    return;
                parts[0] = parts[0].toUpperCase().charAt(0) + parts[0].toLowerCase().substring(1);
                type = Type.valueOf(parts[0]);

                if (parts[1].isEmpty())
                    return;
                if (parts[1].endsWith(".") || parts[1].startsWith("."))
                    return;
                pattern = new WildcardPattern(parts[1]);

                valid = true;
            } catch (Throwable ignored) {}
            if (!valid)
                throw new RuntimeException("invalid class filter rule: " + rule);
        }

        public boolean match(String name) {
            return pattern.match(name);
        }

        public enum Type {
            Exclude,
            Include
        }
    }
}
