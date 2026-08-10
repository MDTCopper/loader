package copper.loader.mod;

import java.util.*;

/**
 * A version filter that evaluates semantic version expressions.
 *
 * <p>Supports comparison operators ({@code >=, <=, >, <, =, !=}),
 * caret ({@code ^}) and tilde ({@code ~}) ranges, wildcards ({@code *}, {@code x}),
 * hyphen ranges ({@code A - B}), parentheses grouping, and logical operators
 * ({@code &&}, {@code ||}, and implicit AND between adjacent expressions).</p>
 *
 * <p>Architecture: a rule string is tokenized by {@link Lexer}, parsed into an
 * {@link Expr} AST by {@link Parser}, and evaluated against a {@link SemanticVersion}.</p>
 */
public class SemanticVersionFilter implements IVersionFilter {
    protected Expr root;

    /**
     * @param rule the version filter expression string, or {@code null}/empty for "always match"
     */
    public SemanticVersionFilter(String rule) {
        if (rule == null || rule.trim().isEmpty()) {
            this.root = v -> true;
        } else {
            Lexer lexer = new Lexer(rule.trim());
            Parser parser = new Parser(lexer.tokenize());
            this.root = parser.parseExpr();
        }
    }

    @Override
    public boolean check(Version v) {
        if (v instanceof SemanticVersion ver)
            return root != null && root.evaluate(ver);
        return false;
    }

    // ==========================================
    // 1. Lexer — tokenizes the rule string
    // ==========================================

    protected enum TokenType {
        VERSION, OPERATOR, AND, OR, LPAREN, RPAREN, HYPHEN, EOF
    }

    protected static class Token {
        TokenType type;
        String value;
        public Token(TokenType type, String value) { this.type = type; this.value = value; }
    }

    protected static class Lexer {
        private final String input;
        private int pos = 0;

        Lexer(String input) { this.input = input; }

        /**
         * Tokenizes the input string into a flat token list.
         * Whitespace is stripped; implicit AND is handled later by the parser.
         */
        List<Token> tokenize() {
            List<Token> tokens = new ArrayList<>();
            while (pos < input.length()) {
                char c = input.charAt(pos);

                // Whitespace is stripped — implicit AND is handled in Parser.
                if (Character.isWhitespace(c)) { pos++; continue; }

                if (c == '(') { tokens.add(new Token(TokenType.LPAREN, "(")); pos++; continue; }
                if (c == ')') { tokens.add(new Token(TokenType.RPAREN, ")")); pos++; continue; }
                if (c == '-') { tokens.add(new Token(TokenType.HYPHEN, "-")); pos++; continue; }

                // Parse && and ||.
                if (c == '&' && pos + 1 < input.length() && input.charAt(pos + 1) == '&') {
                    tokens.add(new Token(TokenType.AND, "&&")); pos += 2; continue;
                }
                if (c == '|' && pos + 1 < input.length() && input.charAt(pos + 1) == '|') {
                    tokens.add(new Token(TokenType.OR, "||")); pos += 2; continue;
                }

                // Operators (>=, <=, !=, >, <, =, ^, ~).
                if (isOperatorChar(c)) {
                    tokens.add(new Token(TokenType.OPERATOR, readOperator()));
                    continue;
                }

                // Version numbers (digits, x, X, *, .).
                if (isVersionChar(c)) {
                    tokens.add(new Token(TokenType.VERSION, readVersion()));
                    continue;
                }

                throw new RuntimeException("Syntax error at position " + pos + ": unexpected char '" + c + "'");
            }
            tokens.add(new Token(TokenType.EOF, ""));
            return tokens;
        }

        private boolean isOperatorChar(char c) {
            return c == '>' || c == '<' || c == '=' || c == '!' || c == '^' || c == '~';
        }

        private String readOperator() {
            StringBuilder sb = new StringBuilder();
            while (pos < input.length() && isOperatorChar(input.charAt(pos))) {
                sb.append(input.charAt(pos++));
            }
            return sb.toString();
        }

        private boolean isVersionChar(char c) {
            return Character.isDigit(c) || c == '.' || c == 'x' || c == 'X' || c == '*';
        }

        private String readVersion() {
            StringBuilder sb = new StringBuilder();
            while (pos < input.length() && isVersionChar(input.charAt(pos))) {
                sb.append(input.charAt(pos++));
            }
            return sb.toString();
        }
    }

    // ==========================================
    // 2. Parser — builds an Expr AST from tokens
    // ==========================================

    /** An AST node that can be evaluated against a SemanticVersion. */
    protected interface Expr { boolean evaluate(SemanticVersion v); }

    protected static class AndExpr implements Expr {
        Expr left, right;
        AndExpr(Expr l, Expr r) { left = l; right = r; }
        public boolean evaluate(SemanticVersion v) { return left.evaluate(v) && right.evaluate(v); }
    }

    protected static class OrExpr implements Expr {
        Expr left, right;
        OrExpr(Expr l, Expr r) { left = l; right = r; }
        public boolean evaluate(SemanticVersion v) { return left.evaluate(v) || right.evaluate(v); }
    }

    protected static class Parser {
        private final List<Token> tokens;
        private int pos = 0;

        Parser(List<Token> tokens) { this.tokens = tokens; }

        /**
         * Parses the full expression, combining primaries with AND/OR operators.
         */
        Expr parseExpr() {
            Expr left = parsePrimary();

            while (pos < tokens.size()) {
                Token t = tokens.get(pos);
                if (t.type == TokenType.EOF || t.type == TokenType.RPAREN) break;

                if (t.type == TokenType.AND || t.type == TokenType.OR) {
                    // Explicit && or ||.
                    pos++;
                    Expr right = parsePrimary();
                    if (t.type == TokenType.AND) left = new AndExpr(left, right);
                    else left = new OrExpr(left, right);
                } else {
                    // Implicit AND between adjacent expressions.
                    Expr right = parsePrimary();
                    left = new AndExpr(left, right);
                }
            }
            return left;
        }

        /**
         * Parses a single primary expression: parenthesized group, hyphen range, or comparison rule.
         */
        Expr parsePrimary() {
            if (pos >= tokens.size() || tokens.get(pos).type == TokenType.EOF) return v -> true;

            Token t = tokens.get(pos);

            // Parenthesized sub-expression.
            if (t.type == TokenType.LPAREN) {
                pos++; // consume '('
                Expr e = parseExpr();
                if (pos < tokens.size() && tokens.get(pos).type == TokenType.RPAREN) {
                    pos++; // consume ')'
                }
                return e;
            }

            // Optional leading operator (default is '=').
            String op = "=";
            if (t.type == TokenType.OPERATOR) {
                op = t.value;
                pos++;
                t = pos < tokens.size() ? tokens.get(pos) : new Token(TokenType.EOF, "");
            }

            String versionStr = "";
            if (t.type == TokenType.VERSION) {
                versionStr = t.value;
                pos++;
            }

            // Hyphen range: "A - B" → ">=A && <=B".
            if (pos < tokens.size() && tokens.get(pos).type == TokenType.HYPHEN) {
                pos++; // consume '-'
                Token nextT = tokens.get(pos);
                String versionB = "";
                if (nextT.type == TokenType.VERSION) {
                    versionB = nextT.value;
                    pos++;
                }
                return new AndExpr(new SemverRule(">=", versionStr), new SemverRule("<=", versionB));
            }

            return new SemverRule(op, versionStr);
        }
    }

    // ==========================================
    // 3. SemverRule — a single version constraint
    // ==========================================

    protected static class SemverRule implements Expr {
        String op;
        int maj = -1, min = -1, pat = -1;

        /**
         * @param op     the comparison operator ({@code =, !=, >, >=, <, <=, ^, ~})
         * @param verStr the version string, may contain {@code *} or {@code x} as wildcards
         */
        public SemverRule(String op, String verStr) {
            this.op = (op == null || op.isEmpty()) ? "=" : op;
            String[] parts = verStr.split("\\.");
            if (parts.length > 0 && !parts[0].isEmpty()) maj = parsePart(parts[0]);
            if (parts.length > 1) min = parsePart(parts[1]);
            if (parts.length > 2) pat = parsePart(parts[2]);
        }

        /** Parses a version component; returns -1 for {@code *}/@code{x} wildcards. */
        private int parsePart(String s) {
            if (s.equals("*") || s.equalsIgnoreCase("x")) return -1;
            try { return Integer.parseInt(s); } catch (Throwable e) { return -1; }
        }

        @Override
        public boolean evaluate(SemanticVersion v) {
            if (op.equals("=")) return matchEq(v);
            if (op.equals("!=")) return !matchEq(v);
            if (op.equals(">")) return matchGt(v);
            if (op.equals(">=")) return matchEq(v) || matchGt(v);
            if (op.equals("<")) return matchLt(v);
            if (op.equals("<=")) return matchEq(v) || matchLt(v);
            if (op.equals("~")) return matchTilde(v);
            if (op.equals("^")) return matchCaret(v);
            return false;
        }

        /** Exact match; wildcards (-1) skip the component. */
        private boolean matchEq(SemanticVersion v) {
            if (maj != -1 && v.major != maj) return false;
            if (min != -1 && v.minor != min) return false;
            if (pat != -1 && v.patch != pat) return false;
            return true;
        }

        /** Strictly greater than. */
        private boolean matchGt(SemanticVersion v) {
            if (maj == -1) return false;
            if (v.major > maj) return true;
            if (v.major < maj) return false;

            if (min == -1) return false;
            if (v.minor > min) return true;
            if (v.minor < min) return false;

            if (pat == -1) return false;
            return v.patch > pat;
        }

        /** Strictly less than. */
        private boolean matchLt(SemanticVersion v) {
            if (maj == -1) return false;
            if (v.major < maj) return true;
            if (v.major > maj) return false;

            if (min == -1) return false;
            if (v.minor < min) return true;
            if (v.minor > min) return false;

            if (pat == -1) return false;
            return v.patch < pat;
        }

        /** Tilde range: {@code ~X.Y.Z} means {@code >=X.Y.Z && <X.(Y+1).0}. */
        private boolean matchTilde(SemanticVersion v) {
            if (!matchEq(v) && !matchGt(v)) return false;
            if (min == -1) return v.major == maj;
            return v.major == maj && v.minor == min;
        }

        /** Caret range: {@code ^X.Y.Z} allows changes that do not modify the leftmost non-zero digit. */
        private boolean matchCaret(SemanticVersion v) {
            if (!matchEq(v) && !matchGt(v)) return false;
            if (maj != 0) return v.major == maj;
            if (min != 0 && min != -1) return v.major == maj && v.minor == min;
            if (pat != 0 && pat != -1) return v.major == maj && v.minor == min && v.patch == pat;
            if (min == -1) return v.major == 0;
            if (pat == -1) return v.major == 0 && v.minor == 0;
            return v.major == 0 && v.minor == 0 && v.patch == 0;
        }
    }
}
