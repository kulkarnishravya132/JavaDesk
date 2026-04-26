import java.util.HashMap;
import java.util.Map;

/**
 * AppRegistry
 * -----------
 * Maps friendly app names to OS-specific launch commands.
 * Detects Windows / macOS / Linux at runtime — no hardcoded paths.
 *
 * Requirements: JDK 8+, no external libraries.
 */
public class AppRegistry {

    private static final String OS = detectOS();
    private static final Map<String, String[]> REGISTRY = buildRegistry();

    public static String[] getCommand(String appName) {
        if (appName == null) return null;
        return REGISTRY.get(appName.toLowerCase().trim());
    }

    public static boolean isKnown(String appName) {
        if (appName == null) return false;
        return REGISTRY.containsKey(appName.toLowerCase().trim());
    }

    public static String currentOS() { return OS; }

    private static String detectOS() {
        String name = System.getProperty("os.name", "").toLowerCase();
        if (name.contains("win")) return "windows";
        if (name.contains("mac")) return "mac";
        return "linux";
    }

    private static Map<String, String[]> buildRegistry() {
        Map<String, String[]> map = new HashMap<>();
        switch (OS) {
            case "windows":
                map.put("notepad",      new String[]{"notepad.exe"});
                map.put("notes",        new String[]{"notepad.exe"});
                map.put("calculator",   new String[]{"cmd", "/c", "start", "calc.exe"});
                map.put("calendar",     new String[]{"cmd", "/c", "start", "outlookcal:"});
                map.put("email",        new String[]{"cmd", "/c", "start", "mailto:"});
                map.put("vscode",       new String[]{"cmd", "/c", "code"});
                map.put("vs code",      new String[]{"cmd", "/c", "code"});
                map.put("terminal",     new String[]{"cmd", "/c", "start", "cmd.exe"});
                map.put("spotify",      new String[]{"cmd", "/c", "start", "spotify:"});
                map.put("music",        new String[]{"cmd", "/c", "start", "spotify:"});
                map.put("focus music",  new String[]{"cmd", "/c", "start", "spotify:"});
                map.put("vlc",          new String[]{"cmd", "/c", "start", "vlc"});
                map.put("file manager", new String[]{"cmd", "/c", "start", "explorer.exe"});
                map.put("files",        new String[]{"cmd", "/c", "start", "explorer.exe"});
                map.put("browser",      new String[]{"cmd", "/c", "start", "https://www.google.com"});
                map.put("dim screen",   new String[]{"cmd", "/c", "start", "ms-settings:display"});
                map.put("do not disturb", new String[]{"cmd", "/c", "start", "ms-settings:notifications"});
                break;
            case "mac":
                map.put("notepad",      new String[]{"open", "-a", "TextEdit"});
                map.put("notes",        new String[]{"open", "-a", "Notes"});
                map.put("calculator",   new String[]{"open", "-a", "Calculator"});
                map.put("calendar",     new String[]{"open", "-a", "Calendar"});
                map.put("email",        new String[]{"open", "-a", "Mail"});
                map.put("vscode",       new String[]{"open", "-a", "Visual Studio Code"});
                map.put("vs code",      new String[]{"open", "-a", "Visual Studio Code"});
                map.put("terminal",     new String[]{"open", "-a", "Terminal"});
                map.put("spotify",      new String[]{"open", "-a", "Spotify"});
                map.put("music",        new String[]{"open", "-a", "Music"});
                map.put("focus music",  new String[]{"open", "-a", "Spotify"});
                map.put("vlc",          new String[]{"open", "-a", "VLC"});
                map.put("file manager", new String[]{"open", System.getProperty("user.home")});
                map.put("files",        new String[]{"open", System.getProperty("user.home")});
                map.put("browser",      new String[]{"open", "https://www.google.com"});
                map.put("dim screen",   new String[]{"osascript", "-e", "tell app \"System Preferences\" to activate"});
                map.put("do not disturb", new String[]{"osascript", "-e", "tell app \"System Preferences\" to activate"});
                break;
            default: // Linux
                map.put("notepad",      new String[]{"gedit"});
                map.put("notes",        new String[]{"gedit"});
                map.put("calculator",   new String[]{"gnome-calculator"});
                map.put("calendar",     new String[]{"gnome-calendar"});
                map.put("email",        new String[]{"xdg-open", "mailto:"});
                map.put("vscode",       new String[]{"code"});
                map.put("vs code",      new String[]{"code"});
                map.put("terminal",     new String[]{"x-terminal-emulator"});
                map.put("spotify",      new String[]{"spotify"});
                map.put("music",        new String[]{"rhythmbox"});
                map.put("focus music",  new String[]{"rhythmbox"});
                map.put("vlc",          new String[]{"vlc"});
                map.put("file manager", new String[]{"xdg-open", System.getProperty("user.home")});
                map.put("files",        new String[]{"xdg-open", System.getProperty("user.home")});
                map.put("browser",      new String[]{"xdg-open", "https://www.google.com"});
                map.put("dim screen",   new String[]{"xdg-open", "https://www.google.com"});
                map.put("do not disturb", new String[]{"xdg-open", "https://www.google.com"});
                break;
        }
        return map;
    }
}
