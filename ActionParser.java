/**
 * ActionParser
 * ------------
 * Splits a pipe-delimited macro actions string into individual tokens
 * and classifies each one into an ActionType.
 *
 * Input:  "Open Notes | Open Browser | Start Timer"
 * Output: [ParsedAction(OPEN_APP,"notes"), ParsedAction(OPEN_URL,...), ParsedAction(TIMER,...)]
 *
 * Requirements: JDK 8+, no external libraries.
 */
public class ActionParser {

    /** Classification of a single action token. */
    public enum ActionType {
        OPEN_URL,       // contains http/https/www  → open directly in browser
        FORCE_BROWSER,  // contains "web" keyword   → force browser (Spotify web, etc.)
        OPEN_APP,       // matches known app or open/launch/start prefix
        TIMER,          // contains "timer" or "wait"
        LOG_ONLY        // anything else — log it but don't crash
    }

    /** Represents one parsed action ready for execution. */
    public static class ParsedAction {
        public final String     raw;    // original string from user
        public final ActionType type;
        public final String     target; // extracted keyword, URL, or description

        ParsedAction(String raw, ActionType type, String target) {
            this.raw    = raw;
            this.type   = type;
            this.target = target;
        }

        @Override
        public String toString() {
            return "[" + type + "] raw=\"" + raw + "\" target=\"" + target + "\"";
        }
    }

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Split the pipe-delimited actions string into individual non-blank tokens.
     * e.g. "Open Notes | Open Browser" → ["Open Notes", "Open Browser"]
     */
    public static String[] split(String actions) {
        if (actions == null || actions.trim().isEmpty()) return new String[0];
        String[] parts = actions.split("\\|");
        java.util.List<String> result = new java.util.ArrayList<>();
        for (String p : parts) {
            String t = p.trim();
            if (!t.isEmpty()) result.add(t);
        }
        return result.toArray(new String[0]);
    }

    /**
     * Parse and classify a single action token.
     */
    public static ParsedAction parse(String raw) {
        if (raw == null || raw.trim().isEmpty())
            return new ParsedAction(raw, ActionType.LOG_ONLY, "");

        String lower = raw.toLowerCase().trim();

        // ── Rule 1: Direct URL ────────────────────────────────────────────
        if (lower.contains("http://") || lower.contains("https://") || lower.contains("www.")) {
            return new ParsedAction(raw, ActionType.OPEN_URL, extractUrl(raw));
        }

        // ── Rule 2: Force browser (e.g. "Open Spotify web") ───────────────
        if (lower.contains(" web") || lower.endsWith(" web")) {
            String query = lower
                .replaceFirst("^(open|launch|start)\\s+", "")
                .replaceAll("\\s*web\\s*$", "")
                .trim();
            return new ParsedAction(raw, ActionType.FORCE_BROWSER, query);
        }

        // ── Rule 3: Timer ─────────────────────────────────────────────────
        if (lower.contains("timer") || lower.equals("start timer") || lower.contains("wait")) {
            return new ParsedAction(raw, ActionType.TIMER, extractDuration(lower));
        }

        // ── Rule 4: Open / Launch / Start prefix ──────────────────────────
        if (lower.startsWith("open ")   ||
            lower.startsWith("launch ") ||
            lower.startsWith("start ")) {
            String keyword = lower
                .replaceFirst("^(open|launch|start)\\s+", "")
                .trim();
            return new ParsedAction(raw, ActionType.OPEN_APP, keyword);
        }

        // ── Rule 5: Bare keyword match against registry ───────────────────
        if (AppRegistry.isKnown(lower)) {
            return new ParsedAction(raw, ActionType.OPEN_APP, lower);
        }

        // ── Rule 6: Fallback — log only ───────────────────────────────────
        return new ParsedAction(raw, ActionType.LOG_ONLY, raw);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private static String extractUrl(String raw) {
        for (String token : raw.split("\\s+")) {
            if (token.startsWith("http://") || token.startsWith("https://")) return token;
            if (token.startsWith("www.")) return "https://" + token;
        }
        return "https://www.google.com/search?q=" + encode(raw);
    }

    private static String extractDuration(String lower) {
        java.util.regex.Matcher m = java.util.regex.Pattern
            .compile("(\\d+)\\s*(second|minute|hour|sec|min|hr)s?")
            .matcher(lower);
        if (m.find()) return m.group(1) + " " + m.group(2) + "(s)";
        return "1 minute";
    }

    /** URL-encode a query string, safe for all JDK versions. */
    static String encode(String query) {
        try {
            return java.net.URLEncoder.encode(query, "UTF-8");
        } catch (Exception e) {
            return query.replace(" ", "+");
        }
    }
}
