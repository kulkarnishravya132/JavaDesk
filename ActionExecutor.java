import java.awt.Desktop;
import java.net.URI;

/**
 * ActionExecutor
 * --------------
 * Main execution engine for JavaDesk macros.
 *
 * PUBLIC API (signature must not change):
 *   public static void execute(String actions)
 *
 * Each action runs on its own daemon Thread — the Swing UI never freezes.
 * Falls back gracefully if an app is not installed; never crashes.
 *
 * Requirements: JDK 8+, no external libraries.
 *
 * HOW TO COMPILE (all files in same folder):
 *   javac -cp ".;sqlite-jdbc-3.x.x.jar" *.java        (Windows)
 *   javac -cp ".:sqlite-jdbc-3.x.x.jar" *.java        (Mac/Linux)
 *
 * HOW TO RUN:
 *   java -cp ".;sqlite-jdbc-3.x.x.jar" JavaDesk       (Windows)
 *   java -cp ".:sqlite-jdbc-3.x.x.jar" JavaDesk       (Mac/Linux)
 */
public class ActionExecutor {

    // ════════════════════════════════════════════════════════════════════
    // PUBLIC API — do not rename or change signature
    // ════════════════════════════════════════════════════════════════════

    /**
     * Execute all pipe-separated actions in the given string.
     * Each action is dispatched on its own background thread.
     *
     * @param actions  e.g. "Open Notes | Open Browser | Start Timer"
     */
    public static void execute(String actions) {
        System.out.println("[ActionExecutor] execute() called with: " + actions);
        System.out.println("[ActionExecutor] Detected OS: " + AppRegistry.currentOS());

        String[] tokens = ActionParser.split(actions);
        if (tokens.length == 0) {
            System.out.println("[ActionExecutor] No actions to execute.");
            return;
        }

        for (String token : tokens) {
            ActionParser.ParsedAction parsed = ActionParser.parse(token);
            System.out.println("[ActionExecutor] Parsed → " + parsed);

            // Each action on its own daemon thread — UI stays live
            final ActionParser.ParsedAction p = parsed;
            Thread t = new Thread(() -> dispatch(p));
            t.setDaemon(true);
            t.setName("action-" + token.trim().replace(" ", "-").toLowerCase());
            t.start();
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // PRIVATE DISPATCH
    // ════════════════════════════════════════════════════════════════════

    private static void dispatch(ActionParser.ParsedAction action) {
        System.out.println("[ActionExecutor] Dispatching: " + action.raw);
        try {
            switch (action.type) {
                case OPEN_URL:      openInBrowser(action.target);                  break;
                case FORCE_BROWSER: openInBrowser(buildSearchUrl(action.target));  break;
                case OPEN_APP:      openApp(action.target);                        break;
                case TIMER:         runTimer(action.target);                       break;
                case LOG_ONLY:      logOnly(action.raw);                           break;
                default:            logOnly(action.raw);                           break;
            }
        } catch (Exception e) {
            System.out.println("[ActionExecutor] Unexpected error for \"" + action.raw + "\": " + e.getMessage());
            // Last-resort fallback: Google it
            openInBrowser(buildSearchUrl(action.raw));
        }
    }

    // ── Open URL in system default browser ────────────────────────────────

    private static void openInBrowser(String url) {
        System.out.println("[ActionExecutor] Opening in browser: " + url);

        // Strategy 1: java.awt.Desktop (works on most systems with a GUI)
        try {
            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
                System.out.println("[ActionExecutor] Browser opened via Desktop API.");
                return;
            }
        } catch (Exception e) {
            System.out.println("[ActionExecutor] Desktop API failed: " + e.getMessage());
        }

        // Strategy 2: OS-level command
        try {
            new ProcessBuilder(browserCommand(url))
                .inheritIO()
                .start();
            System.out.println("[ActionExecutor] Browser opened via ProcessBuilder.");
        } catch (Exception e) {
            System.out.println("[ActionExecutor] Could not open browser: " + e.getMessage());
        }
    }

    // ── Open a native application ─────────────────────────────────────────

    private static void openApp(String appKeyword) {
        System.out.println("[ActionExecutor] Attempting to open app: \"" + appKeyword + "\"");

        String[] cmd = AppRegistry.getCommand(appKeyword);

        if (cmd != null) {
            try {
                new ProcessBuilder(cmd)
                    .inheritIO()
                    .start();
                System.out.println("[ActionExecutor] App launched: " + java.util.Arrays.toString(cmd));
                return;
            } catch (Exception e) {
                System.out.println("[ActionExecutor] App not found on this machine: " + appKeyword);
            }
        } else {
            System.out.println("[ActionExecutor] \"" + appKeyword + "\" not in registry.");
        }

        // Fallback: search in browser
        System.out.println("[ActionExecutor] App not found, opening in browser instead.");
        openInBrowser(buildSearchUrl(appKeyword));
    }

    // ── Timer ─────────────────────────────────────────────────────────────

    private static void runTimer(String duration) {
        int seconds = parseDurationSeconds(duration);
        System.out.println("[ActionExecutor] Timer started: " + duration + " (" + seconds + "s)");
        try {
            Thread.sleep((long) seconds * 1000L);
            System.out.println("[ActionExecutor] Timer finished: " + duration);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            System.out.println("[ActionExecutor] Timer interrupted.");
        }
    }

    // ── Log-only ──────────────────────────────────────────────────────────

    private static void logOnly(String raw) {
        System.out.println("[ActionExecutor] Action logged (no system execution): " + raw);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private static String buildSearchUrl(String query) {
        return "https://www.google.com/search?q=" + ActionParser.encode(query);
    }

    /** Returns the OS command array to open a URL in the default browser. */
    private static String[] browserCommand(String url) {
        switch (AppRegistry.currentOS()) {
            case "windows": return new String[]{"cmd", "/c", "start", url};
            case "mac":     return new String[]{"open", url};
            default:        return new String[]{"xdg-open", url};
        }
    }

    /** Parse "5 minute(s)" → 300, "30 second(s)" → 30, default → 60 */
    private static int parseDurationSeconds(String desc) {
        try {
            java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(\\d+)\\s*(second|minute|hour|sec|min|hr)")
                .matcher(desc.toLowerCase());
            if (m.find()) {
                int n    = Integer.parseInt(m.group(1));
                String u = m.group(2);
                if (u.startsWith("sec")) return n;
                if (u.startsWith("min")) return n * 60;
                if (u.startsWith("hr") || u.startsWith("hour")) return n * 3600;
            }
        } catch (Exception ignored) {}
        return 60; // default: 1 minute
    }
}
