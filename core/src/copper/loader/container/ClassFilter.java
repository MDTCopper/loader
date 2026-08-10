package copper.loader.container;

import copper.loader.util.*;
import java.util.*;

/**
 * A filter that checks class names against a list of include/exclude rules.
 *
 * <p>Each rule is a string in the form {@code "include <pattern>"} or {@code "exclude <pattern>"},
 * where the pattern supports wildcards ({@code *}, {@code ?}). Rules are evaluated in order;
 * the first matching rule determines the result. If no rule matches, the default is deny.</p>
 */
public class ClassFilter {
    protected ArrayList<Rule> rules;

    public ClassFilter() {
        rules = new ArrayList<>();
    }

    /**
     * Adds a rule string ({@code "include <pattern>"} or {@code "exclude <pattern>"}).
     *
     * @throws RuntimeException if the rule string is invalid
     */
    public void addRule(String txt) {
        rules.add(new Rule(txt));
    }

    /** Removes all rules. */
    public void clearRules() {
        rules.clear();
    }

    /**
     * Checks whether a class name passes this filter.
     *
     * @param clazz fully qualified class name (dots or slashes)
     * @return {@code true} if the class should be visible
     */
    public boolean check(String clazz) {
        if (rules.isEmpty())
            return false;
        clazz = clazz.replace('/', '.');
        for (Rule rule : rules)
            if (rule.match(clazz))
                return rule.type == Rule.Type.Include;
        return false;
    }

    /** A single include/exclude rule. */
    protected static class Rule {
        public Type type;
        public WildcardPattern pattern;

        /**
         * Parses a rule string like {@code "include com.example.*"}.
         *
         * @param rule the rule string
         * @throws RuntimeException if the format is invalid
         */
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
