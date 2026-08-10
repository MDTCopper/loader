package copper.loader.util;

import copper.loader.func.*;
import java.util.*;

/**
 * Command-line argument parser with support for short/long options,
 * flags, argument values, bundled flags ({@code -abc}), and
 * {@code --} positional argument separation. Auto-registers {@code -h/--help}
 * and allows value retrieval by option name.
 */
public class ArgParser {
    private final String programName;
    private final String description;
    private final ArrayList<Option> options = new ArrayList<>();
    private final ArrayList<String> positionalArgs = new ArrayList<>();
    /** Maps option name (short or long) to all values provided. */
    private final Map<String, ArrayList<String>> optionValuesMap = new HashMap<>();
    /** Options that were present on the command line (for flags). */
    private final Set<String> flagSet = new HashSet<>();
    private String positionalDescription = null;
    private boolean helpRequested = false;

    /**
     * Constructs a parser.
     *
     * @param programName name of the program (used in help output)
     * @param description brief description of the program
     */
    public ArgParser(String programName, String description) {
        this.programName = programName;
        this.description = description;
        // Auto-register -h/--help.
        addFlag("h", "help", "Show this help message", () -> helpRequested = true);
    }

    /**
     * Sets a brief description for positional arguments shown in help.
     */
    public void setPositionalDescription(String positionalDescription) {
        this.positionalDescription = positionalDescription;
    }

    /**
     * Registers a flag (option without argument) with a callback action.
     */
    public void addFlag(String shortOpt, String longOpt, String desc, Runnable action) {
        options.add(new Option(shortOpt, longOpt, desc, false, null, action, null));
    }

    /**
     * Registers a flag without an action (presence can be checked via {@link #hasOption}).
     */
    public void addFlag(String shortOpt, String longOpt, String desc) {
        options.add(new Option(shortOpt, longOpt, desc, false, null, null, null));
    }

    /**
     * Registers an option with an argument and a callback handler.
     */
    public void addOption(String shortOpt, String longOpt, String desc, String argName, Cons<String> action) {
        options.add(new Option(shortOpt, longOpt, desc, true, argName, null, action));
    }

    /**
     * Registers an option with an argument without a handler (value can be retrieved later).
     */
    public void addOption(String shortOpt, String longOpt, String desc, String argName) {
        options.add(new Option(shortOpt, longOpt, desc, true, argName, null, null));
    }

    /**
     * Parses command-line arguments.
     *
     * @throws IllegalArgumentException if an unknown option or missing argument is encountered
     */
    public void parse(String[] args) {
        int i = 0;
        while (i < args.length) {
            String arg = args[i];
            if (arg.equals("--")) {
                for (int j = i + 1; j < args.length; j++) {
                    positionalArgs.add(args[j]);
                }
                break;
            } else if (arg.startsWith("--")) {
                String longOpt = arg.substring(2);
                int eqIdx = longOpt.indexOf('=');
                String optName;
                String optValue = null;
                if (eqIdx != -1) {
                    optName = longOpt.substring(0, eqIdx);
                    optValue = longOpt.substring(eqIdx + 1);
                } else {
                    optName = longOpt;
                }
                Option opt = findOptionByLong(optName);
                if (opt == null) {
                    throw new IllegalArgumentException("Unknown option: " + arg);
                }
                if (opt.hasArg && optValue == null) {
                    if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                        optValue = args[i + 1];
                        i++; // consume value token
                    } else {
                        throw new IllegalArgumentException("Option " + opt.toDisplayString() + " requires an argument");
                    }
                }
                processOption(opt, optValue);
                i++;
            } else if (arg.startsWith("-") && arg.length() > 1) {
                String optString = arg.substring(1);
                if (optString.length() == 1) {
                    String shortOpt = optString;
                    Option opt = findOptionByShort(shortOpt);
                    if (opt == null) {
                        throw new IllegalArgumentException("Unknown option: " + arg);
                    }
                    String optValue = null;
                    if (opt.hasArg) {
                        if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                            optValue = args[i + 1];
                            i++; // consume value token
                        } else {
                            throw new IllegalArgumentException("Option " + arg + " requires an argument");
                        }
                    }
                    processOption(opt, optValue);
                    i++;
                } else {
                    // Bundled flags, e.g. -abc (a, b, c are flags).
                    for (int j = 0; j < optString.length(); j++) {
                        char c = optString.charAt(j);
                        String shortOpt = String.valueOf(c);
                        Option opt = findOptionByShort(shortOpt);
                        if (opt == null) {
                            throw new IllegalArgumentException("Unknown option: -" + c);
                        }
                        if (opt.hasArg) {
                            throw new IllegalArgumentException("Bundled option cannot contain an option that requires an argument: -" + c);
                        }
                        processOption(opt, null);
                    }
                    i++;
                }
            } else {
                positionalArgs.add(arg);
                i++;
            }
        }

        if (helpRequested) {
            printHelp();
            System.exit(0);
        }
    }

    private void processOption(Option opt, String value) {
        if (opt.hasArg && value == null) {
            throw new IllegalArgumentException("Option " + opt.toDisplayString() + " requires an argument");
        }
        if (!opt.hasArg && value != null) {
            throw new IllegalArgumentException("Option " + opt.toDisplayString() + " does not accept an argument");
        }

        if (opt.hasArg) {
            // Store the value for both short and long names.
            if (opt.shortOpt != null) {
                optionValuesMap.computeIfAbsent(opt.shortOpt, k -> new ArrayList<>()).add(value);
            }
            if (opt.longOpt != null) {
                optionValuesMap.computeIfAbsent(opt.longOpt, k -> new ArrayList<>()).add(value);
            }
            if (opt.optionAction != null) {
                opt.optionAction.get(value);
            }
        } else {
            if (opt.shortOpt != null) flagSet.add(opt.shortOpt);
            if (opt.longOpt != null) flagSet.add(opt.longOpt);
            if (opt.flagAction != null) {
                opt.flagAction.run();
            }
        }
    }

    private Option findOptionByShort(String shortOpt) {
        for (Option opt : options) {
            if (shortOpt.equals(opt.shortOpt)) {
                return opt;
            }
        }
        return null;
    }

    private Option findOptionByLong(String longOpt) {
        for (Option opt : options) {
            if (longOpt.equals(opt.longOpt)) {
                return opt;
            }
        }
        return null;
    }

    /**
     * Checks whether a flag or option was present on the command line.
     *
     * @param name short or long option name (without leading dashes)
     * @return {@code true} if the option was given
     */
    public boolean hasOption(String name) {
        return flagSet.contains(name) || optionValuesMap.containsKey(name);
    }

    /**
     * Returns the first argument value for a given option.
     *
     * @param name short or long option name (without leading dashes)
     * @return the first value, or {@code null} if the option was not given or has no argument
     */
    public String getOptionValue(String name) {
        List<String> list = optionValuesMap.get(name);
        return (list != null && !list.isEmpty()) ? list.get(0) : null;
    }

    /**
     * Returns all argument values for a given option (preserves order of occurrence).
     */
    public List<String> getOptionValues(String name) {
        ArrayList<String> list = optionValuesMap.get(name);
        return list != null ? list : new ArrayList<>();
    }

    /** Returns the list of positional arguments (non-option arguments). */
    public List<String> getPositionalArgs() {
        return positionalArgs;
    }

    /** Prints the help message to stdout. */
    public void printHelp() {
        System.out.println("Usage: " + programName + " [options] [arguments...]");
        if (description != null && !description.isEmpty()) {
            System.out.println(description);
        }
        System.out.println("\nOptions:");
        for (Option opt : options) {
            if (opt.shortOpt == null && opt.longOpt == null) continue;
            StringBuilder sb = new StringBuilder("  ");
            if (opt.shortOpt != null) {
                sb.append("-").append(opt.shortOpt);
                if (opt.longOpt != null) sb.append(", ");
            }
            if (opt.longOpt != null) {
                sb.append("--").append(opt.longOpt);
            }
            if (opt.hasArg) {
                sb.append(" <").append(opt.argName != null ? opt.argName : "arg").append(">");
            }
            while (sb.length() < 30) sb.append(' ');
            sb.append(opt.description != null ? opt.description : "");
            System.out.println(sb.toString());
        }
        if (positionalDescription != null)
            System.out.println("\nWhen '--' is encountered, all following arguments are treated as " + positionalDescription + ".");
    }

    /** Internal option representation. */
    private static class Option {
        final String shortOpt;
        final String longOpt;
        final String description;
        final boolean hasArg;
        final String argName;
        final Runnable flagAction;
        final Cons<String> optionAction;

        Option(String shortOpt, String longOpt, String description, boolean hasArg,
               String argName, Runnable flagAction, Cons<String> optionAction) {
            this.shortOpt = shortOpt;
            this.longOpt = longOpt;
            this.description = description;
            this.hasArg = hasArg;
            this.argName = argName;
            this.flagAction = flagAction;
            this.optionAction = optionAction;
        }

        String toDisplayString() {
            return (shortOpt != null) ? "-" + shortOpt : "--" + longOpt;
        }
    }
}
