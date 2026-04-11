import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

public class JavaDesk extends JFrame {

    // ── Palette ───────────────────────────────────────────────────────────
    static final Color BG_DARK      = new Color(5,   4,  15);
    static final Color BG_PANEL     = new Color(12,  8,  28);
    static final Color BG_CARD      = new Color(20, 14,  42);
    static final Color ACCENT_BLUE  = new Color(80, 140, 255);
    static final Color ACCENT_PURPLE= new Color(180, 60, 255);
    static final Color ACCENT_PINK  = new Color(255, 40, 180);
    static final Color ACCENT_TEAL  = new Color(0,  230, 200);
    static final Color TEXT_PRIMARY = new Color(220,200, 255);
    static final Color TEXT_MUTED   = new Color(100, 80, 140);
    static final Color SUCCESS      = new Color(50, 255, 160);
    static final Color WARNING      = new Color(255,180,   0);

    // ── In-memory storage (no database) ──────────────────────────────────
    // User-defined macros: key = macro name, value = pipe-separated actions
    private final LinkedHashMap<String, String> macros = new LinkedHashMap<>();
    // Log entries
    private final List<String> logs = new ArrayList<>();

    // ── UI state ──────────────────────────────────────────────────────────
    private JPanel     contentPanel;
    private CardLayout cards;
    private JLabel     clockLabel;
    private JLabel     statusBar;
    private String     activeMode = "None";

    // Live dashboard labels
    private JLabel dashMacroVal;
    private JLabel dashModeVal;

    // Table models
    private DefaultTableModel macroModel;
    private JTextArea         logArea;

    // Cleanup state
    private Path cleanupStagingDir = null;
    private final LinkedHashMap<String, Path> originalPaths = new LinkedHashMap<>();
    private DefaultTableModel cleanupModel;
    private JLabel cleanupSummaryLabel;

    // ══════════════════════════════════════════════════════════════════════
    public JavaDesk() {
        seedDefaultMacros();
        initUI();
        startClock();
        log("JavaDesk started.");
    }

    // ── Seed 3 example macros on first launch ─────────────────────────────
    private void seedDefaultMacros() {
        macros.put("Study Mode",  "Open Notes | Open Browser | Start Timer");
        macros.put("Work Mode",   "Open Email | Open Calendar | Focus Music");
        macros.put("Chill Mode",  "Open Music | Dim Screen | Do Not Disturb");
    }

    // ── UI init ───────────────────────────────────────────────────────────
    private void initUI() {
        setTitle("JavaDesk");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 720);
        setMinimumSize(new Dimension(900, 620));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout());
        buildContentArea();
        add(buildTopBar(),    BorderLayout.NORTH);
        add(buildSidebar(),   BorderLayout.WEST);
        add(contentPanel,     BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
        setVisible(true);
    }

    // ── Top Bar ───────────────────────────────────────────────────────────
    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(BG_PANEL);
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ACCENT_PURPLE.darker()));
        bar.setPreferredSize(new Dimension(0, 62));

        JLabel logo = new JLabel("  JavaDesk");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 22));
        logo.setForeground(ACCENT_PURPLE);
        bar.add(logo, BorderLayout.WEST);

        clockLabel = new JLabel("", SwingConstants.CENTER);
        clockLabel.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        clockLabel.setForeground(ACCENT_BLUE);
        bar.add(clockLabel, BorderLayout.CENTER);

        JPanel mp = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 16));
        mp.setOpaque(false);
        JLabel modeLabel = new JLabel("Mode: None  ");
        modeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        modeLabel.setForeground(ACCENT_PINK);
        modeLabel.setName("modeLabel");
        mp.add(modeLabel);
        bar.add(mp, BorderLayout.EAST);
        return bar;
    }

    // ── Sidebar ───────────────────────────────────────────────────────────
    private JPanel buildSidebar() {
        JPanel side = new JPanel();
        side.setBackground(BG_PANEL);
        side.setPreferredSize(new Dimension(185, 0));
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, ACCENT_PURPLE.darker()));
        side.add(Box.createVerticalStrut(20));

        String[][] nav = {
            {"Dashboard",      "DASH"},
            {"Macros / Modes", "MACRO"},
            {"Command",        "CMD"},
            {"Logs",           "LOGS"}
        };
        ButtonGroup bg = new ButtonGroup();
        boolean first = true;
        for (String[] item : nav) {
            JToggleButton btn = makeNavButton(item[0], item[1]);
            bg.add(btn); side.add(btn); side.add(Box.createVerticalStrut(4));
            if (first) { btn.setSelected(true); first = false; }
        }
        side.add(Box.createVerticalGlue());
        JLabel ver = new JLabel("v1.0 prototype", SwingConstants.CENTER);
        ver.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        ver.setForeground(TEXT_MUTED);
        ver.setAlignmentX(Component.CENTER_ALIGNMENT);
        side.add(ver);
        side.add(Box.createVerticalStrut(10));
        return side;
    }

    private JToggleButton makeNavButton(String label, String card) {
        JToggleButton btn = new JToggleButton(label);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btn.setMaximumSize(new Dimension(175, 42));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setFocusPainted(false); btn.setBorderPainted(false); btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        styleNavBtn(btn, false);
        btn.addItemListener(e -> {
            styleNavBtn(btn, btn.isSelected());
            if (btn.isSelected() && cards != null && contentPanel != null)
                cards.show(contentPanel, card);
        });
        return btn;
    }

    private void styleNavBtn(JToggleButton btn, boolean sel) {
        btn.setBackground(sel ? BG_CARD : BG_PANEL);
        btn.setForeground(sel ? ACCENT_PURPLE : TEXT_MUTED);
    }

    // ── Content area ──────────────────────────────────────────────────────
    private void buildContentArea() {
        cards = new CardLayout();
        contentPanel = new JPanel(cards);
        contentPanel.setBackground(BG_DARK);
        contentPanel.add(buildDashboard(),  "DASH");
        contentPanel.add(buildMacroPanel(), "MACRO");
        contentPanel.add(buildCmdPanel(),   "CMD");
        contentPanel.add(buildLogsPanel(),  "LOGS");
    }

    // ── Dashboard ─────────────────────────────────────────────────────────
    private JPanel buildDashboard() {
        JPanel p = darkPanel(new BorderLayout(0, 0));

        JPanel header = darkPanel(new FlowLayout(FlowLayout.LEFT, 24, 18));
        header.add(sectionTitle("Dashboard"));
        p.add(header, BorderLayout.NORTH);

        JPanel grid = darkPanel(new GridLayout(1, 2, 24, 0));
        grid.setBorder(new EmptyBorder(60, 80, 60, 80));

        // Macros count card — live reference
        JPanel mc = statCardPanel(ACCENT_PURPLE);
        mc.add(statCardLabel("Saved Macros"), BorderLayout.NORTH);
        dashMacroVal = statCardBigValue(String.valueOf(macros.size()), ACCENT_PURPLE);
        mc.add(dashMacroVal, BorderLayout.CENTER);
        grid.add(mc);

        // Active mode card — live reference
        JPanel modec = statCardPanel(ACCENT_TEAL);
        modec.add(statCardLabel("Active Mode"), BorderLayout.NORTH);
        dashModeVal = new JLabel(activeMode);
        dashModeVal.setFont(new Font("Segoe UI", Font.BOLD, 20));
        dashModeVal.setForeground(ACCENT_TEAL);
        dashModeVal.setBorder(new EmptyBorder(8, 0, 0, 0));
        modec.add(dashModeVal, BorderLayout.CENTER);
        grid.add(modec);

        p.add(grid, BorderLayout.CENTER);

        JPanel qPanel = darkPanel(new FlowLayout(FlowLayout.CENTER, 12, 14));
        JLabel ql = new JLabel("Quick Command:");
        ql.setForeground(TEXT_MUTED);
        ql.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        JTextField qf = styledField(30);
        JButton qb = accentButton("Execute", ACCENT_PURPLE);
        qb.addActionListener(e -> { processCommand(qf.getText()); qf.setText(""); });
        qf.addActionListener(e -> qb.doClick());
        qPanel.add(ql); qPanel.add(qf); qPanel.add(qb);
        p.add(qPanel, BorderLayout.SOUTH);
        return p;
    }

    private JPanel statCardPanel(Color accent) {
        JPanel c = new JPanel(new BorderLayout());
        c.setBackground(BG_CARD);
        c.setBorder(new CompoundBorder(
            new LineBorder(accent.darker(), 1, true),
            new EmptyBorder(24, 28, 24, 28)));
        return c;
    }
    private JLabel statCardLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        l.setForeground(TEXT_MUTED);
        return l;
    }
    private JLabel statCardBigValue(String val, Color accent) {
        JLabel l = new JLabel(val);
        l.setFont(new Font("Segoe UI", Font.BOLD, 56));
        l.setForeground(accent);
        return l;
    }

    /** Updates both dashboard stat cards instantly */
    private void refreshDashboard() {
        SwingUtilities.invokeLater(() -> {
            if (dashMacroVal != null) dashMacroVal.setText(String.valueOf(macros.size()));
            if (dashModeVal  != null) dashModeVal.setText(activeMode);
        });
    }

    // ── Macro Panel ───────────────────────────────────────────────────────
    private JPanel buildMacroPanel() {
        JPanel p = darkPanel(new BorderLayout(0, 0));

        JPanel header = darkPanel(new FlowLayout(FlowLayout.LEFT, 24, 14));
        header.add(sectionTitle("Macros / Modes"));
        p.add(header, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setBackground(BG_DARK); split.setBorder(null);
        split.setDividerSize(6); split.setDividerLocation(200);
        split.setContinuousLayout(true);

        // ── TOP: Built-in macros ──────────────────────────────────────────
        JPanel builtinPanel = darkPanel(new BorderLayout(0, 0));
        JPanel bh = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 8));
        bh.setBackground(BG_PANEL);
        bh.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ACCENT_PURPLE.darker()));
        JLabel bt = new JLabel("  Built-in Macros");
        bt.setFont(new Font("Segoe UI", Font.BOLD, 13)); bt.setForeground(ACCENT_PURPLE);
        JLabel bs = new JLabel("  system macros with real actions");
        bs.setFont(new Font("Segoe UI", Font.PLAIN, 11)); bs.setForeground(TEXT_MUTED);
        bh.add(bt); bh.add(bs);
        builtinPanel.add(bh, BorderLayout.NORTH);

        JPanel builtinCards = darkPanel(new FlowLayout(FlowLayout.LEFT, 14, 10));
        builtinCards.setBorder(new EmptyBorder(4, 8, 4, 8));

        JPanel cleanupCard = builtinMacroCard("Desk Cleanup",
            "Scans a folder for files unused for N days, stages them for review, then delete or restore.",
            ACCENT_PINK);
        JButton runCleanup = accentButton("Run", ACCENT_PINK);
        runCleanup.addActionListener(e -> showCleanupDialog());
        cleanupCard.add(runCleanup, BorderLayout.SOUTH);
        builtinCards.add(cleanupCard);

        JPanel folderCard = builtinMacroCard("Folder Organiser",
            "Sort files into subfolders by type (Images, Docs, Videos).", ACCENT_BLUE);
        JButton cs = accentButton("Coming Soon", TEXT_MUTED); cs.setEnabled(false);
        folderCard.add(cs, BorderLayout.SOUTH);
        builtinCards.add(folderCard);

        JScrollPane bScroll = new JScrollPane(builtinCards);
        bScroll.setBorder(null); bScroll.getViewport().setBackground(BG_DARK);
        bScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        bScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        builtinPanel.add(bScroll, BorderLayout.CENTER);
        split.setTopComponent(builtinPanel);

        // ── BOTTOM: User macros ───────────────────────────────────────────
        JPanel userPanel = darkPanel(new BorderLayout(0, 0));
        JPanel uh = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 8));
        uh.setBackground(BG_PANEL);
        uh.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ACCENT_TEAL.darker()));
        JLabel ut = new JLabel("  User Macros");
        ut.setFont(new Font("Segoe UI", Font.BOLD, 13)); ut.setForeground(ACCENT_TEAL);
        JLabel us = new JLabel("  add a name & actions — count updates on Dashboard instantly");
        us.setFont(new Font("Segoe UI", Font.PLAIN, 11)); us.setForeground(TEXT_MUTED);
        uh.add(ut); uh.add(us);
        userPanel.add(uh, BorderLayout.NORTH);

        String[] cols = {"Mode Name", "Actions", "Last Run"};
        macroModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = styledTable(macroModel);
        table.getColumnModel().getColumn(0).setPreferredWidth(180);
        table.getColumnModel().getColumn(1).setPreferredWidth(380);
        table.getColumnModel().getColumn(2).setPreferredWidth(120);
        userPanel.add(styledScroll(table), BorderLayout.CENTER);
        loadMacros();

        // Input row
        JPanel row = darkPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        JTextField nf = styledField(18); nf.setToolTipText("Mode name, e.g. Study Mode");
        JTextField af = styledField(28); af.setToolTipText("Actions separated by |, e.g. Open Notes | Open Browser");
        JButton addBtn = accentButton("+ Save",   ACCENT_PURPLE);
        JButton runBtn = accentButton("Activate", ACCENT_TEAL);
        JButton delBtn = accentButton("Delete",   ACCENT_PINK);

        JLabel nl = new JLabel("Name:");    nl.setForeground(TEXT_MUTED);
        JLabel al = new JLabel("Actions:"); al.setForeground(TEXT_MUTED);
        row.add(nl); row.add(nf); row.add(al); row.add(af);
        row.add(addBtn); row.add(runBtn); row.add(delBtn);

        addBtn.addActionListener(e -> {
            String name    = nf.getText().trim();
            String actions = af.getText().trim();
            if (name.isEmpty())    { setStatus("Enter a macro name"); nf.requestFocus(); return; }
            if (actions.isEmpty()) { setStatus("Enter actions for the macro"); af.requestFocus(); return; }
            macros.put(name, actions);   // store in HashMap
            nf.setText(""); af.setText("");
            loadMacros();
            refreshDashboard();          // dashboard count increments immediately
            setStatus("Macro saved: \"" + name + "\"  |  Dashboard updated");
            log("Macro saved: " + name);
        });

        runBtn.addActionListener(e -> {
            int r = table.getSelectedRow();
            if (r < 0) { setStatus("Select a macro to activate"); return; }
            String name    = (String) macroModel.getValueAt(r, 0);
            String actions = (String) macroModel.getValueAt(r, 1);
            activateMode(name, actions);
        });

        delBtn.addActionListener(e -> {
            int r = table.getSelectedRow();
            if (r < 0) { setStatus("Select a macro to delete"); return; }
            String name = (String) macroModel.getValueAt(r, 0);
            macros.remove(name);
            loadMacros();
            refreshDashboard();
            setStatus("Macro deleted: \"" + name + "\"  |  Dashboard updated");
            log("Macro deleted: " + name);
        });

        userPanel.add(row, BorderLayout.SOUTH);
        split.setBottomComponent(userPanel);
        p.add(split, BorderLayout.CENTER);
        return p;
    }

    private JPanel builtinMacroCard(String title, String desc, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(BG_CARD);
        card.setPreferredSize(new Dimension(240, 130));
        card.setBorder(new CompoundBorder(
            new LineBorder(accent.darker(), 1, true),
            new EmptyBorder(10, 14, 10, 14)));
        JLabel t = new JLabel(title);
        t.setFont(new Font("Segoe UI", Font.BOLD, 13)); t.setForeground(accent);
        card.add(t, BorderLayout.NORTH);
        JTextArea d = new JTextArea(desc);
        d.setFont(new Font("Segoe UI", Font.PLAIN, 11)); d.setForeground(TEXT_MUTED);
        d.setBackground(BG_CARD); d.setEditable(false);
        d.setWrapStyleWord(true); d.setLineWrap(true);
        card.add(d, BorderLayout.CENTER);
        return card;
    }

    private void showCleanupDialog() {
        JDialog dlg = new JDialog(this, "Desk Cleanup", true);
        dlg.setSize(820, 580);
        dlg.setLocationRelativeTo(this);
        dlg.getContentPane().setBackground(BG_DARK);
        dlg.getContentPane().add(buildCleanupPanel());
        dlg.setVisible(true);
    }

    // ── Command Panel ─────────────────────────────────────────────────────
    private JPanel buildCmdPanel() {
        JPanel p = darkPanel(new BorderLayout(12, 12));
        p.setBorder(new EmptyBorder(0, 16, 0, 16));

        JPanel header = darkPanel(new FlowLayout(FlowLayout.LEFT, 8, 18));
        header.add(sectionTitle("Command Interface"));
        p.add(header, BorderLayout.NORTH);

        JTextArea out = new JTextArea();
        out.setFont(new Font("Consolas", Font.PLAIN, 13));
        out.setBackground(new Color(2, 2, 10));
        out.setForeground(ACCENT_TEAL);
        out.setCaretColor(ACCENT_TEAL);
        out.setEditable(false);
        out.setText(
            "  JavaDesk Command Interface\n" +
            "  -----------------------------------------\n" +
            "  activate <mode name>   Activate a macro\n" +
            "  list macros            Show all macros\n" +
            "  sysinfo                System info\n" +
            "  clear                  Clear console\n" +
            "  help                   Show this help\n" +
            "  -----------------------------------------\n\n");
        JScrollPane sp = new JScrollPane(out);
        sp.setBorder(new LineBorder(ACCENT_TEAL.darker(), 1));
        p.add(sp, BorderLayout.CENTER);

        JPanel inputRow = darkPanel(new BorderLayout(8, 0));
        inputRow.setBorder(new EmptyBorder(8, 0, 8, 0));
        JLabel prompt = new JLabel("  >");
        prompt.setFont(new Font("Consolas", Font.BOLD, 14));
        prompt.setForeground(ACCENT_TEAL);
        JTextField input = new JTextField();
        input.setFont(new Font("Consolas", Font.PLAIN, 13));
        input.setBackground(new Color(2, 2, 10));
        input.setForeground(ACCENT_TEAL);
        input.setCaretColor(ACCENT_TEAL);
        input.setBorder(new LineBorder(ACCENT_TEAL.darker(), 1));
        JButton exec = accentButton("Execute", ACCENT_TEAL);
        exec.setForeground(BG_DARK);

        ActionListener run = e -> {
            String cmd = input.getText().trim();
            if (cmd != null && !cmd.isEmpty()) {
                if ("clear".equalsIgnoreCase(cmd)) {
                    out.setText("");
                } else {
                    out.append("  > " + cmd + "\n");
                    out.append("  " + processCommand(cmd) + "\n\n");
                    out.setCaretPosition(out.getDocument().getLength());
                }
                input.setText("");
            }
        };
        exec.addActionListener(run);
        input.addActionListener(run);
        inputRow.add(prompt, BorderLayout.WEST);
        inputRow.add(input,  BorderLayout.CENTER);
        inputRow.add(exec,   BorderLayout.EAST);
        p.add(inputRow, BorderLayout.SOUTH);
        return p;
    }

    // ── Logs Panel ────────────────────────────────────────────────────────
    private JPanel buildLogsPanel() {
        JPanel p = darkPanel(new BorderLayout(0, 0));
        JPanel header = darkPanel(new FlowLayout(FlowLayout.LEFT, 24, 18));
        header.add(sectionTitle("Activity Log"));
        p.add(header, BorderLayout.NORTH);

        logArea = new JTextArea();
        logArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        logArea.setBackground(BG_CARD); logArea.setForeground(TEXT_PRIMARY);
        logArea.setEditable(false);
        logArea.setBorder(new EmptyBorder(10, 14, 10, 14));
        p.add(styledScroll(logArea), BorderLayout.CENTER);

        JPanel btnRow = darkPanel(new FlowLayout(FlowLayout.RIGHT, 14, 10));
        JButton refresh = accentButton("Refresh", ACCENT_BLUE);
        JButton clear   = accentButton("Clear",   ACCENT_PINK);
        refresh.addActionListener(e -> renderLogs());
        clear.addActionListener(e -> { logs.clear(); renderLogs(); });
        btnRow.add(refresh); btnRow.add(clear);
        p.add(btnRow, BorderLayout.SOUTH);
        renderLogs();
        return p;
    }

    // ── Desk Cleanup Panel ────────────────────────────────────────────────
    private JPanel buildCleanupPanel() {
        JPanel p = darkPanel(new BorderLayout(0, 0));

        JPanel header = darkPanel(new FlowLayout(FlowLayout.LEFT, 24, 14));
        header.add(sectionTitle("Desk Cleanup"));
        JLabel sub = new JLabel("Scan a folder and quarantine files untouched for N days");
        sub.setForeground(TEXT_MUTED); sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        header.add(sub);
        p.add(header, BorderLayout.NORTH);

        String[] cols = {"File Name", "Size", "Last Modified", "Original Location"};
        cleanupModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = styledTable(cleanupModel);
        table.getColumnModel().getColumn(0).setPreferredWidth(220);
        table.getColumnModel().getColumn(1).setPreferredWidth(80);
        table.getColumnModel().getColumn(2).setPreferredWidth(150);
        table.getColumnModel().getColumn(3).setPreferredWidth(300);
        p.add(styledScroll(table), BorderLayout.CENTER);

        JPanel south = darkPanel(new BorderLayout());
        JPanel configRow = darkPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        JLabel folderLbl = new JLabel("Folder:"); folderLbl.setForeground(TEXT_MUTED);
        JTextField folderField = styledField(28);
        folderField.setText(System.getProperty("user.home") + File.separator + "Desktop");
        JButton browse = accentButton("Browse", ACCENT_BLUE);
        browse.addActionListener(e -> {
            JFileChooser fc = new JFileChooser(folderField.getText());
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
                folderField.setText(fc.getSelectedFile().getAbsolutePath());
        });
        JLabel daysLbl = new JLabel("Days unused:"); daysLbl.setForeground(TEXT_MUTED);
        JSpinner daysSpinner = new JSpinner(new SpinnerNumberModel(30, 1, 3650, 1));
        daysSpinner.setPreferredSize(new Dimension(70, 30));
        ((JSpinner.DefaultEditor) daysSpinner.getEditor()).getTextField().setBackground(BG_CARD);
        ((JSpinner.DefaultEditor) daysSpinner.getEditor()).getTextField().setForeground(TEXT_PRIMARY);
        JButton scan = accentButton("Scan", ACCENT_PURPLE);
        configRow.add(folderLbl); configRow.add(folderField); configRow.add(browse);
        configRow.add(daysLbl); configRow.add(daysSpinner); configRow.add(scan);

        JPanel actionRow = darkPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        cleanupSummaryLabel = new JLabel("  Run a scan to find old files.");
        cleanupSummaryLabel.setForeground(WARNING);
        cleanupSummaryLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        JButton openFolder = accentButton("Open Staging Folder", ACCENT_BLUE);
        JButton restore    = accentButton("Restore All",          ACCENT_TEAL);
        JButton deleteAll  = accentButton("Delete All Permanently", ACCENT_PINK);
        openFolder.setVisible(false); restore.setVisible(false); deleteAll.setVisible(false);
        actionRow.add(cleanupSummaryLabel); actionRow.add(openFolder);
        actionRow.add(restore); actionRow.add(deleteAll);
        south.add(configRow, BorderLayout.NORTH);
        south.add(actionRow, BorderLayout.SOUTH);
        p.add(south, BorderLayout.SOUTH);

        scan.addActionListener(e -> {
            Path target = Paths.get(folderField.getText().trim());
            int days = (int) daysSpinner.getValue();
            if (!Files.isDirectory(target)) { setStatus("Not a valid directory"); return; }
            scan.setEnabled(false); setStatus("Scanning...");
            new Thread(() -> {
                try {
                    List<Path> stale = scanStaleFiles(target, days);
                    SwingUtilities.invokeLater(() -> {
                        if (stale.isEmpty()) {
                            cleanupSummaryLabel.setText("  No files older than " + days + " days found.");
                            cleanupModel.setRowCount(0);
                            openFolder.setVisible(false); restore.setVisible(false); deleteAll.setVisible(false);
                            scan.setEnabled(true); setStatus("Scan complete — folder is clean."); return;
                        }
                        try {
                            stageFiles(stale); populateCleanupTable(stale);
                            cleanupSummaryLabel.setText("  " + stale.size() + " file(s) staged  —  " + formatSize(stagedTotalSize()));
                            openFolder.setVisible(true); restore.setVisible(true); deleteAll.setVisible(true);
                            scan.setEnabled(true);
                            log("Cleanup: " + stale.size() + " files staged from " + target);
                            setStatus("Staging complete — review files below.");
                        } catch (Exception ex) { scan.setEnabled(true); setStatus("Error staging: " + ex.getMessage()); }
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> { scan.setEnabled(true); setStatus("Scan error: " + ex.getMessage()); });
                }
            }).start();
        });

        openFolder.addActionListener(e -> {
            if (cleanupStagingDir != null && Files.exists(cleanupStagingDir))
                try { Desktop.getDesktop().open(cleanupStagingDir.toFile()); }
                catch (Exception ex) { setStatus("Could not open folder: " + ex.getMessage()); }
        });

        restore.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(this, "Restore all staged files to their original locations?",
                    "Restore Files", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
            int ok = 0, fail = 0;
            for (Map.Entry<String, Path> entry : originalPaths.entrySet()) {
                try {
                    Files.createDirectories(entry.getValue().getParent());
                    Files.move(cleanupStagingDir.resolve(entry.getKey()), entry.getValue(), StandardCopyOption.REPLACE_EXISTING);
                    ok++;
                } catch (Exception ex) { fail++; }
            }
            try { Files.deleteIfExists(cleanupStagingDir); } catch (Exception ig) {}
            cleanupModel.setRowCount(0); originalPaths.clear(); cleanupStagingDir = null;
            openFolder.setVisible(false); restore.setVisible(false); deleteAll.setVisible(false);
            cleanupSummaryLabel.setText("  Restored " + ok + " file(s)" + (fail > 0 ? " (" + fail + " failed)" : ""));
            log("Cleanup: restored " + ok + " files.");
        });

        deleteAll.addActionListener(e -> {
            int count = cleanupModel.getRowCount();
            String msg = "PERMANENT — Cannot be undone!\n\n"
                + count + " file(s) will be deleted forever.\nTotal: " + formatSize(stagedTotalSize())
                + "\n\nAre you absolutely sure?";
            if (JOptionPane.showConfirmDialog(this, msg, "Confirm Permanent Delete",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
            if (JOptionPane.showConfirmDialog(this,
                    "Last chance — permanently delete " + count + " file(s)?",
                    "Final Confirmation", JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
            long freed = 0;
            try { freed = deleteDirectoryRecursive(cleanupStagingDir); }
            catch (Exception ex) { setStatus("Partial delete error: " + ex.getMessage()); }
            cleanupModel.setRowCount(0); originalPaths.clear(); cleanupStagingDir = null;
            openFolder.setVisible(false); restore.setVisible(false); deleteAll.setVisible(false);
            final long f = freed;
            cleanupSummaryLabel.setText("  Deleted " + count + " file(s)  —  " + formatSize(f) + " freed.");
            log("Cleanup: deleted " + count + " files, freed " + formatSize(f));
            setStatus("Cleanup complete. Freed: " + formatSize(f));
        });

        return p;
    }

    // ── Cleanup helpers ───────────────────────────────────────────────────
    private List<Path> scanStaleFiles(Path dir, int days) throws IOException {
        List<Path> result = new ArrayList<>();
        long cutoff = System.currentTimeMillis() - (long) days * 86_400_000L;
        Files.walk(dir, 1).filter(pp -> !pp.equals(dir)).filter(pp -> {
            try { return Files.isRegularFile(pp) && Files.getLastModifiedTime(pp).toMillis() < cutoff; }
            catch (IOException ex) { return false; }
        }).forEach(result::add);
        return result;
    }

    private void stageFiles(List<Path> files) throws IOException {
        String ts = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        cleanupStagingDir = Paths.get(System.getProperty("user.home"), "JavaDesk_Cleanup_" + ts);
        Files.createDirectories(cleanupStagingDir);
        originalPaths.clear();
        for (Path file : files) {
            String name = file.getFileName().toString();
            Path dest = cleanupStagingDir.resolve(name);
            int counter = 1;
            while (Files.exists(dest)) {
                String base = name.contains(".") ? name.substring(0, name.lastIndexOf('.')) : name;
                String ext  = name.contains(".") ? name.substring(name.lastIndexOf('.')) : "";
                dest = cleanupStagingDir.resolve(base + "_" + counter++ + ext);
            }
            Files.move(file, dest, StandardCopyOption.ATOMIC_MOVE);
            originalPaths.put(dest.getFileName().toString(), file.toAbsolutePath());
        }
    }

    private void populateCleanupTable(List<Path> originals) {
        cleanupModel.setRowCount(0);
        for (Path orig : originals) {
            Path staged = originalPaths.entrySet().stream()
                .filter(en -> en.getValue().equals(orig.toAbsolutePath()))
                .map(en -> cleanupStagingDir.resolve(en.getKey()))
                .findFirst().orElse(orig);
            try {
                long size = Files.size(staged);
                String mtime = LocalDateTime.ofInstant(
                    Files.getLastModifiedTime(staged).toInstant(),
                    java.time.ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("dd MMM yyyy  HH:mm"));
                cleanupModel.addRow(new Object[]{ orig.getFileName().toString(), formatSize(size), mtime, orig.getParent().toString() });
            } catch (IOException ex) {
                cleanupModel.addRow(new Object[]{ orig.getFileName().toString(), "?", "?", orig.getParent().toString() });
            }
        }
    }

    private long deleteDirectoryRecursive(Path dir) throws IOException {
        AtomicLong total = new AtomicLong(0);
        if (Files.exists(dir)) {
            Files.walk(dir).sorted(Comparator.reverseOrder()).forEach(pp -> {
                try { if (Files.isRegularFile(pp)) total.addAndGet(Files.size(pp)); Files.delete(pp); }
                catch (IOException ig) {}
            });
        }
        return total.get();
    }

    private long stagedTotalSize() {
        if (cleanupStagingDir == null || !Files.exists(cleanupStagingDir)) return 0;
        try {
            return Files.walk(cleanupStagingDir).filter(Files::isRegularFile)
                .mapToLong(pp -> { try { return Files.size(pp); } catch (IOException ex) { return 0; } })
                .sum();
        } catch (IOException ex) { return 0; }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024)           return bytes + " B";
        if (bytes < 1024 * 1024)    return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024*1024*1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    // ── Status bar ────────────────────────────────────────────────────────
    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 6));
        bar.setBackground(BG_PANEL.darker());
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ACCENT_PURPLE.darker()));
        statusBar = new JLabel("Ready");
        statusBar.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusBar.setForeground(TEXT_MUTED);
        bar.add(statusBar);
        return bar;
    }

    // ── Command processing ────────────────────────────────────────────────
    private String processCommand(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "No command entered.";
        String cmd = raw.trim().toLowerCase();
        if (cmd.equals("help"))
            return "Commands: activate <name> | list macros | sysinfo | clear";
        if (cmd.startsWith("activate "))
            return activateModeByName(raw.substring(9).trim());
        if (cmd.equals("list macros"))
            return listMacros();
        if (cmd.equals("sysinfo"))
            return sysInfo();
        return "Unknown: '" + raw + "'. Type 'help'.";
    }

    // ── Macro operations ──────────────────────────────────────────────────
    /** Rebuild the macro table from the in-memory HashMap */
    private void loadMacros() {
        if (macroModel == null) return;
        macroModel.setRowCount(0);
        for (Map.Entry<String, String> entry : macros.entrySet()) {
            macroModel.addRow(new Object[]{ entry.getKey(), entry.getValue(), "Never" });
        }
    }

    private void activateMode(String name, String actions) {
        activeMode = name;
        refreshDashboard();
        updateModeLabel();
        log("Mode activated: " + name);
        setStatus("Mode active: " + name);
        JOptionPane.showMessageDialog(this,
            "<html><b style='color:#B43CFF'>Mode: " + name + "</b><br><br>"
            + actions.replace("|", "<br>") + "</html>",
            "Mode Activated", JOptionPane.INFORMATION_MESSAGE);
    }

    private String activateModeByName(String name) {
        String key = macros.keySet().stream()
            .filter(k -> k.equalsIgnoreCase(name))
            .findFirst().orElse(null);
        if (key != null) {
            activateMode(key, macros.get(key));
            return "Activated: " + key;
        }
        return "Mode not found: " + name;
    }

    private String listMacros() {
        if (macros.isEmpty()) return "No macros saved.";
        StringBuilder sb = new StringBuilder("Macros:\n");
        for (Map.Entry<String, String> e : macros.entrySet())
            sb.append("  ").append(e.getKey()).append(": ").append(e.getValue()).append("\n");
        return sb.toString();
    }

    private String sysInfo() {
        Runtime rt = Runtime.getRuntime();
        long tot = rt.totalMemory() / (1024*1024), free = rt.freeMemory() / (1024*1024);
        return "OS: " + System.getProperty("os.name")
            + "  |  Cores: " + rt.availableProcessors()
            + "  |  RAM: " + (tot - free) + "MB/" + tot + "MB"
            + "  |  Java: " + System.getProperty("java.version");
    }

    // ── Logging ───────────────────────────────────────────────────────────
    private void log(String msg) {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM HH:mm"));
        logs.add(0, "[" + ts + "] " + msg);   // newest first
        if (logs.size() > 50) logs.remove(logs.size() - 1);
        if (logArea != null) renderLogs();
        setStatus(msg);
    }

    private void renderLogs() {
        if (logArea == null) return;
        StringBuilder sb = new StringBuilder();
        for (String entry : logs) sb.append(entry).append("\n");
        logArea.setText(sb.toString());
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private void setStatus(String msg) {
        SwingUtilities.invokeLater(() -> { if (statusBar != null) statusBar.setText(msg); });
    }

    private void updateModeLabel() {
        SwingUtilities.invokeLater(() -> {
            Container top = (Container)((BorderLayout) getContentPane().getLayout())
                .getLayoutComponent(BorderLayout.NORTH);
            searchAndUpdateMode(top);
        });
    }

    private void searchAndUpdateMode(Container c) {
        if (c == null) return;
        for (Component ch : c.getComponents()) {
            if (ch instanceof JLabel && "modeLabel".equals(ch.getName()))
                ((JLabel) ch).setText("Mode: " + activeMode + "  ");
            if (ch instanceof Container) searchAndUpdateMode((Container) ch);
        }
    }

    private void startClock() {
        Timer t = new Timer(true);
        t.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                String now = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy  -  HH:mm:ss"));
                SwingUtilities.invokeLater(() -> { if (clockLabel != null) clockLabel.setText(now); });
            }
        }, 0, 1000);
    }

    // ── UI factory helpers ────────────────────────────────────────────────
    private JPanel darkPanel(LayoutManager lm) {
        JPanel pp = new JPanel(lm); pp.setBackground(BG_DARK); return pp;
    }
    private JLabel sectionTitle(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 20));
        l.setForeground(TEXT_PRIMARY);
        return l;
    }
    private JTextField styledField(int cols) {
        JTextField f = new JTextField(cols);
        f.setBackground(BG_CARD); f.setForeground(TEXT_PRIMARY); f.setCaretColor(ACCENT_PURPLE);
        f.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        f.setBorder(new CompoundBorder(
            new LineBorder(ACCENT_PURPLE.darker(), 1, true),
            new EmptyBorder(4, 8, 4, 8)));
        return f;
    }
    private JButton accentButton(String text, Color accent) {
        JButton b = new JButton(text);
        b.setBackground(accent.darker().darker()); b.setForeground(accent);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setFocusPainted(false); b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new CompoundBorder(
            new LineBorder(accent.darker(), 1, true),
            new EmptyBorder(6, 14, 6, 14)));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(accent.darker()); }
            public void mouseExited (MouseEvent e) { b.setBackground(accent.darker().darker()); }
        });
        return b;
    }
    private JTable styledTable(TableModel model) {
        JTable t = new JTable(model);
        t.setBackground(BG_CARD); t.setForeground(TEXT_PRIMARY);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 13)); t.setRowHeight(32);
        t.setGridColor(BG_PANEL);
        t.setSelectionBackground(ACCENT_PURPLE.darker().darker());
        t.setSelectionForeground(ACCENT_PURPLE);
        t.setShowHorizontalLines(true); t.setShowVerticalLines(false);
        t.setIntercellSpacing(new Dimension(0, 1));
        JTableHeader h = t.getTableHeader();
        h.setBackground(BG_PANEL); h.setForeground(ACCENT_BLUE);
        h.setFont(new Font("Segoe UI", Font.BOLD, 12));
        h.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ACCENT_PURPLE.darker()));
        return t;
    }
    private <T extends JComponent> JScrollPane styledScroll(T c) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBackground(BG_DARK);
        sp.setBorder(new LineBorder(ACCENT_PURPLE.darker(), 1));
        sp.getViewport().setBackground(BG_CARD);
        sp.getVerticalScrollBar().setBackground(BG_PANEL);
        sp.getHorizontalScrollBar().setBackground(BG_PANEL);
        return sp;
    }

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        SwingUtilities.invokeLater(JavaDesk::new);
    }
}
