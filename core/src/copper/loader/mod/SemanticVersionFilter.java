package copper.loader.mod;

import java.util.*;

public class SemanticVersionFilter implements IVersionFilter {
    protected Expr root;

    public SemanticVersionFilter(String rule) {
        if (rule == null || rule.trim().isEmpty()) {
            this.root = v -> true;
        } else {
            Lexer lexer = new Lexer(rule.trim());
            Parser parser = new Parser(lexer.tokenize());
            this.root = parser.parseExpr();
        }
    }

    public boolean check(Version v) {
        if (v instanceof SemanticVersion ver)
            return root != null && root.evaluate(ver);
        return false;
    }

    // ==========================================
    // 1. Lexer
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

        List<Token> tokenize() {
            List<Token> tokens = new ArrayList<>();
            while (pos < input.length()) {
                char c = input.charAt(pos);

                // process implicit And in Parser, strip ' ' here.
                if (Character.isWhitespace(c)) { pos++; continue; }

                if (c == '(') { tokens.add(new Token(TokenType.LPAREN, "(")); pos++; continue; }
                if (c == ')') { tokens.add(new Token(TokenType.RPAREN, ")")); pos++; continue; }
                if (c == '-') { tokens.add(new Token(TokenType.HYPHEN, "-")); pos++; continue; }

                // parse && and ||
                if (c == '&' && pos + 1 < input.length() && input.charAt(pos + 1) == '&') {
                    tokens.add(new Token(TokenType.AND, "&&")); pos += 2; continue;
                }
                if (c == '|' && pos + 1 < input.length() && input.charAt(pos + 1) == '|') {
                    tokens.add(new Token(TokenType.OR, "||")); pos += 2; continue;
                }

                // ops (>=, <=, !=, >, <, =, ^, ~)
                if (isOperatorChar(c)) {
                    tokens.add(new Token(TokenType.OPERATOR, readOperator()));
                    continue;
                }

                // vernum (number, x, X, *, .)
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
    // 2. Parser AST
    // ==========================================

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

        // parse and construct full expr
        Expr parseExpr() {
            Expr left = parsePrimary();

            while (pos < tokens.size()) {
                Token t = tokens.get(pos);
                if (t.type == TokenType.EOF || t.type == TokenType.RPAREN) break;

                // explicit && or ||
                if (t.type == TokenType.AND || t.type == TokenType.OR) {
                    pos++;
                    Expr right = parsePrimary();
                    if (t.type == TokenType.AND) left = new AndExpr(left, right);
                    else left = new OrExpr(left, right);
                } else {
                    // implicit And
                    Expr right = parsePrimary();
                    left = new AndExpr(left, right);
                }
            }
            return left;
        }

        // parse basic rule
        Expr parsePrimary() {
            if (pos >= tokens.size() || tokens.get(pos).type == TokenType.EOF) return v -> true;

            Token t = tokens.get(pos);

            // process ()
            if (t.type == TokenType.LPAREN) {
                pos++; // consume '('
                Expr e = parseExpr();
                if (pos < tokens.size() && tokens.get(pos).type == TokenType.RPAREN) {
                    pos++; // consume ')'
                }
                return e;
            }

            // process rule
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

            // check somethine likes "1.0.0 - 2.0.0"
            if (pos < tokens.size() && tokens.get(pos).type == TokenType.HYPHEN) {
                pos++; // 消耗 '-'
                Token nextT = tokens.get(pos);
                String versionB = "";
                if (nextT.type == TokenType.VERSION) {
                    versionB = nextT.value;
                    pos++;
                }
                // convert A - B to >= A && <= B
                return new AndExpr(new SemverRule(">=", versionStr), new SemverRule("<=", versionB));
            }

            return new SemverRule(op, versionStr);
        }
    }

    // ==========================================
    // 3. SemverRule
    // ==========================================

    protected static class SemverRule implements Expr {
        String op;
        int maj = -1, min = -1, pat = -1;

        public SemverRule(String op, String verStr) {
            this.op = (op == null || op.isEmpty()) ? "=" : op;
            String[] parts = verStr.split("\\.");
            if (parts.length > 0 && !parts[0].isEmpty()) maj = parsePart(parts[0]);
            if (parts.length > 1) min = parsePart(parts[1]);
            if (parts.length > 2) pat = parsePart(parts[2]);
        }

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

        private boolean matchEq(SemanticVersion v) {
            if (maj != -1 && v.major != maj) return false;
            if (min != -1 && v.minor != min) return false;
            if (pat != -1 && v.patch != pat) return false;
            return true;
        }

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

        private boolean matchTilde(SemanticVersion v) {
            if (!matchEq(v) && !matchGt(v)) return false;
            if (min == -1) return v.major == maj;
            return v.major == maj && v.minor == min;
        }

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
