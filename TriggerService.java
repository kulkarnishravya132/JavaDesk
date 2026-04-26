import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * TriggerService
 * --------------
 * Background scheduler for time-based and condition-based macro triggers.
 * Polls every 60 seconds using ScheduledExecutorService.
 *
 * Two trigger types:
 *   TIME      — fire a macro at a specific HH:mm wall-clock time
 *   CONDITION — simulate condition checks (logged; no real APIs needed)
 *
 * Requirements: JDK 8+, no external libraries.
 *
 * Usage:
 *   TriggerService svc = TriggerService.getInstance();
 *   svc.addTimeTrigger("Study Mode", "18:00", "Open Notes | Open Browser");
 *   svc.addConditionTrigger("Idle Check", "if inactive for 1 hour", "Open Notes", 60);
 *   svc.start();
 */
public class TriggerService {

    // ── Data classes ──────────────────────────────────────────────────────

    /** A trigger that fires a macro at a specific wall-clock time each day. */
    public static class TimeTrigger {
        public final String name;
        public final String fireAtHHMM;   // e.g. "18:00"
        public final String actions;       // pipe-separated actions string

        private boolean firedToday     = false;
        private String  lastFiredDate  = "";

        public TimeTrigger(String name, String fireAtHHMM, String actions) {
            this.name       = name;
            this.fireAtHHMM = fireAtHHMM;
            this.actions    = actions;
        }
    }

    /** A trigger that simulates a condition check and fires when "met". */
    public static class ConditionTrigger {
        public final String name;
        public final String description;          // e.g. "if inactive for 1 hour"
        public final String actions;
        public final int    checkIntervalMinutes; // how often to evaluate (minutes)

        private int minutesSinceLastCheck = 0;

        public ConditionTrigger(String name, String description,
                                String actions, int checkIntervalMinutes) {
            this.name                 = name;
            this.description          = description;
            this.actions              = actions;
            this.checkIntervalMinutes = checkIntervalMinutes;
        }
    }

    // ── Singleton ─────────────────────────────────────────────────────────

    private static TriggerService instance;

    public static TriggerService getInstance() {
        if (instance == null) instance = new TriggerService();
        return instance;
    }

    private TriggerService() {}

    // ── State ─────────────────────────────────────────────────────────────

    private final List<TimeTrigger>      timeTriggers      = new ArrayList<>();
    private final List<ConditionTrigger> conditionTriggers = new ArrayList<>();
    private ScheduledExecutorService     scheduler;
    private boolean                      running           = false;

    // ════════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ════════════════════════════════════════════════════════════════════

    /**
     * Register a time-based trigger.
     * Example: addTimeTrigger("Study Mode", "18:00", "Open Notes | Open Browser")
     */
    public void addTimeTrigger(String name, String hhmm, String actions) {
        timeTriggers.add(new TimeTrigger(name, hhmm, actions));
        System.out.println("[TriggerService] Time trigger registered: \"" + name + "\" at " + hhmm);
    }

    /**
     * Register a condition-based trigger (simulated — no real APIs).
     * Example: addConditionTrigger("Idle Reminder", "if inactive for 1 hour", actions, 60)
     */
    public void addConditionTrigger(String name, String description,
                                     String actions, int intervalMinutes) {
        conditionTriggers.add(new ConditionTrigger(name, description, actions, intervalMinutes));
        System.out.println("[TriggerService] Condition trigger registered: \""
                + name + "\" — " + description);
    }

    /**
     * Scan a macro map for names containing a time hint like "at 18:00"
     * and auto-register them as time triggers.
     * Called from JavaDesk after DB macros are loaded.
     */
    public void registerFromMacros(Map<String, String> macros) {
        int count = 0;
        for (Map.Entry<String, String> entry : macros.entrySet()) {
            java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("at\\s+(\\d{1,2}:\\d{2})")
                .matcher(entry.getKey().toLowerCase());
            if (m.find()) {
                String raw  = m.group(1);
                String hhmm = raw.length() == 4 ? "0" + raw : raw; // pad to HH:mm
                addTimeTrigger(entry.getKey(), hhmm, entry.getValue());
                count++;
            }
        }
        System.out.println("[TriggerService] Auto-registered " + count
                + " time trigger(s) from macro names.");
    }

    /**
     * Start the background scheduler (polls every 60 seconds).
     * Safe to call multiple times — only starts once.
     */
    public void start() {
        if (running) {
            System.out.println("[TriggerService] Already running.");
            return;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "trigger-service");
            t.setDaemon(true); // won't prevent JVM shutdown
            return t;
        });
        scheduler.scheduleAtFixedRate(this::tick, 0, 60, TimeUnit.SECONDS);
        running = true;
        System.out.println("[TriggerService] Started — polling every 60 seconds.");
    }

    /** Shut down the scheduler gracefully. */
    public void stop() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
            running = false;
            System.out.println("[TriggerService] Stopped.");
        }
    }

    public boolean isRunning() { return running; }

    // ════════════════════════════════════════════════════════════════════
    // PRIVATE — tick() called every 60 seconds
    // ════════════════════════════════════════════════════════════════════

    private void tick() {
        String nowHHMM = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        String today   = LocalDate.now().toString();

        // ── 1. Time-based triggers ────────────────────────────────────────
        for (TimeTrigger trigger : timeTriggers) {
            // Reset the daily flag on a new calendar day
            if (!today.equals(trigger.lastFiredDate)) {
                trigger.firedToday   = false;
                trigger.lastFiredDate = today;
            }
            if (!trigger.firedToday && nowHHMM.equals(trigger.fireAtHHMM)) {
                trigger.firedToday = true;
                System.out.println("[TriggerService] TIME trigger FIRED: \""
                        + trigger.name + "\" at " + nowHHMM);
                System.out.println("[TriggerService] Executing: " + trigger.actions);
                ActionExecutor.execute(trigger.actions);
            }
        }

        // ── 2. Condition-based triggers ───────────────────────────────────
        for (ConditionTrigger trigger : conditionTriggers) {
            trigger.minutesSinceLastCheck++;
            if (trigger.minutesSinceLastCheck >= trigger.checkIntervalMinutes) {
                trigger.minutesSinceLastCheck = 0;
                boolean met = simulateCondition(trigger.description);
                System.out.println("[TriggerService] CONDITION check \"" + trigger.name
                        + "\" (" + trigger.description + "): "
                        + (met ? "MET" : "not met"));
                if (met) {
                    System.out.println("[TriggerService] Condition met → executing macro: "
                            + trigger.name);
                    ActionExecutor.execute(trigger.actions);
                }
            }
        }
    }

    /**
     * Simulate a condition check. No real APIs — demonstrates the flow.
     * Replace individual branches with real OS/API checks as needed.
     */
    private boolean simulateCondition(String description) {
        String lower = description.toLowerCase();

        if (lower.contains("inactive") || lower.contains("idle")) {
            // Real impl: check OS idle time via JNA or native call
            System.out.println("[TriggerService] Simulating idle-time check...");
            System.out.println("[TriggerService] Condition met → executing macro");
            return true;
        }

        if (lower.contains("file") && lower.contains("unused")) {
            // Real impl: scan filesystem for stale files
            System.out.println("[TriggerService] Simulating stale-file check...");
            System.out.println("[TriggerService] Condition met → executing macro");
            return true;
        }

        // Generic: 30 % random chance so demos actually fire occasionally
        boolean met = Math.random() < 0.3;
        System.out.println("[TriggerService] Generic condition evaluated: "
                + (met ? "met" : "not met"));
        return met;
    }

    // ── Standalone demo ───────────────────────────────────────────────────

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== TriggerService standalone demo ===\n");
        TriggerService svc = TriggerService.getInstance();

        // Fire 1 minute from now
        String demoTime = LocalTime.now().plusMinutes(1)
            .format(DateTimeFormatter.ofPattern("HH:mm"));
        svc.addTimeTrigger("Demo Mode", demoTime, "Open Notes | Start Timer");
        svc.addConditionTrigger("Idle Check",     "if inactive for 1 hour",   "Open Notes", 1);
        svc.addConditionTrigger("Stale File Check","if file unused for 7 days","Open Notes", 2);

        svc.start();
        System.out.println("\nRunning for 3 minutes (Ctrl+C to stop)\n");
        Thread.sleep(3 * 60 * 1000L);
        svc.stop();
    }
}
