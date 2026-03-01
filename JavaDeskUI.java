import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import javafx.scene.Parent;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class JavaDeskUI extends Application {
    private TextArea outputArea;
    private TextField inputField;
    private Label timeLabel;
    private Label statusLabel;
    private ListView<String> recentCommandsList;
    private BorderPane root;
    private VBox leftPanel;
    private VBox centerPanel;
    private VBox rightPanel;
    private HBox topBar;
    private HBox bottomBar;
    private boolean isDarkMode = false;
    private java.util.HashMap<String, String> customCommands = new java.util.HashMap<>();

    @Override
    public void start(Stage stage) {
        // ===== Root Layout =====
        root = new BorderPane();
        root.setStyle("-fx-background-color: #2d0a4e;");

        // ===== TOP BAR WITH PURPLE THEME =====
        Label title = new Label("◆ JAVAdesk - Command Center");
        title.setFont(Font.font("Segoe UI", javafx.scene.text.FontWeight.BOLD, 20));
        title.setTextFill(Color.web("#E0AAFF"));

        timeLabel = new Label(getCurrentTime());
        timeLabel.setFont(Font.font("Segoe UI", javafx.scene.text.FontWeight.BOLD, 14));
        timeLabel.setTextFill(Color.web("#C77DFF"));
        startClockUpdate();

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        topBar = new HBox(20, title, spacer, timeLabel);
        topBar.setPadding(new Insets(18));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setStyle("-fx-background-color: #3c096c; -fx-border-color: #9D4EDD; -fx-border-width: 0 0 2 0;");

        root.setTop(topBar);

        // ===== LEFT PANEL (Modes & Features) =====
        leftPanel = createLeftPanel();
        root.setLeft(leftPanel);

        // ===== CENTER PANEL (Console) =====
        centerPanel = createCenterPanel();
        root.setCenter(centerPanel);

        // ===== RIGHT PANEL (Info & Quick Actions) =====
        rightPanel = createRightPanel();
        root.setRight(rightPanel);

        // ===== BOTTOM STATUS BAR =====
        statusLabel = new Label("STATUS: READY | SYSTEM ONLINE");
        statusLabel.setTextFill(Color.web("#E0E0E0"));
        statusLabel.setFont(Font.font("Segoe UI", javafx.scene.text.FontWeight.BOLD, 12));

        bottomBar = new HBox(statusLabel);
        bottomBar.setPadding(new Insets(12));
        bottomBar.setStyle("-fx-background-color: #16213e; -fx-border-color: #1E90FF; -fx-border-width: 2 0 0 0;");

        root.setBottom(bottomBar);

        // ===== Scene =====
        Scene scene = new Scene(root, 1200, 700);
        stage.setTitle("JavaDesk - Enhanced Desktop Assistant");
        stage.setScene(scene);
        stage.show();
    }

    private VBox createLeftPanel() {
        VBox leftPanel = new VBox(15);
        leftPanel.setPadding(new Insets(20));
        leftPanel.setPrefWidth(220);
        leftPanel.setStyle("-fx-background-color: #1a2f4a; -fx-border-color: #4a90e2; -fx-border-width: 0 2 0 0;");

        // Modes Section
        Label modeLabel = new Label("► MODES");
        modeLabel.setTextFill(Color.web("#C77DFF"));
        modeLabel.setFont(Font.font("Segoe UI", javafx.scene.text.FontWeight.BOLD, 13));

        ListView<String> modeList = new ListView<>();
        modeList.getItems().addAll("🎓 Study Mode", "💼 Work Mode", "🎵 Relax Mode");
        modeList.setPrefHeight(100);
        modeList.setStyle("-fx-control-inner-background: #5a189a; -fx-text-fill: #E0AAFF; -fx-font-size: 12px;");

        Button activateBtn = createPurpleButton("ACTIVATE", "#C77DFF");
        Button createBtn = createPurpleButton("CREATE MODE", "#E0AAFF");

        // Quick Actions Section
        Label quickLabel = new Label("► QUICK ACTIONS");
        quickLabel.setTextFill(Color.web("#B5179E"));
        quickLabel.setFont(Font.font("Segoe UI", javafx.scene.text.FontWeight.BOLD, 13));

        Button settingsBtn = createPurpleButton("⚙ SETTINGS", "#9D4EDD");
        Button helpBtn = createPurpleButton("❓ HELP", "#C77DFF");

        leftPanel.getChildren().addAll(modeLabel, modeList, activateBtn, createBtn, 
                                       new Separator(), quickLabel, settingsBtn, helpBtn);
        return leftPanel;
    }

    private VBox createCenterPanel() {
        VBox centerPanel = new VBox(15);
        centerPanel.setPadding(new Insets(20));

        Label consoleLabel = new Label("█ CONSOLE TERMINAL");
        consoleLabel.setTextFill(Color.web("#C77DFF"));
        consoleLabel.setFont(Font.font("Segoe UI", javafx.scene.text.FontWeight.BOLD, 15));

        outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setPrefHeight(350);
        outputArea.setWrapText(true);
        outputArea.setStyle(
                "-fx-control-inner-background: #2d5a8c;" +
                "-fx-text-fill: #cce5ff;" +
                "-fx-font-size: 12px;" +
                "-fx-font-family: 'Segoe UI';" +
                "-fx-border-color: #4a90e2;" +
                "-fx-border-width: 2;"
        );
        outputArea.setText("> INITIALIZING JAVADESCK SYSTEM...\n> LOADING COMMAND INTERFACE...\n> SYSTEM READY\n\n");

        // Search Bar
        HBox searchBox = createSearchBar();

        inputField = new TextField();
        inputField.setPromptText("INPUT COMMAND > ");
        inputField.setPrefHeight(40);
        inputField.setStyle(
                "-fx-background-color: #2d5a8c;" +
                "-fx-text-fill: #cce5ff;" +
                "-fx-prompt-text-fill: #a8d0f7;" +
                "-fx-font-size: 12px;" +
                "-fx-font-family: 'Segoe UI';" +
                "-fx-border-color: #4a90e2;" +
                "-fx-border-width: 2;" +
                "-fx-padding: 10;"
        );

        inputField.setOnAction(e -> executeCommand());

        Button executeBtn = createPurpleButton("► EXECUTE", "#9D4EDD");
        Button clearBtn = createPurpleButton("█ CLEAR", "#B5179E");
        Button createCmdBtn = createPurpleButton("➕ NEW COMMAND", "#C77DFF");

        HBox inputBox = new HBox(10, inputField, executeBtn, clearBtn, createCmdBtn);
        inputBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(inputField, Priority.ALWAYS);

        clearBtn.setOnAction(e -> outputArea.clear());
        createCmdBtn.setOnAction(e -> showCreateCommandDialog());

        centerPanel.getChildren().addAll(consoleLabel, searchBox, outputArea, inputBox);
        return centerPanel;
    }

    private VBox createRightPanel() {
        VBox rightPanel = new VBox(15);
        rightPanel.setPadding(new Insets(20));
        rightPanel.setPrefWidth(250);
        rightPanel.setStyle("-fx-background-color: #3c096c; -fx-border-color: #9D4EDD; -fx-border-width: 0 0 0 2;");

        // System Info Section
        Label infoLabel = new Label("█ SYSTEM INFO");
        infoLabel.setTextFill(Color.web("#a8d0f7"));
        infoLabel.setFont(Font.font("Segoe UI", javafx.scene.text.FontWeight.BOLD, 13));

        Label systemStatus = new Label("✓ STATUS: ACTIVE\n✓ MEMORY: OPTIMAL\n✓ CPU: NORMAL");
        systemStatus.setTextFill(Color.web("#cce5ff"));
        systemStatus.setFont(Font.font("Segoe UI", 11));

        // Recent Commands Section
        Label recentLabel = new Label("█ RECENT COMMANDS");
        recentLabel.setTextFill(Color.web("#a8d0f7"));
        recentLabel.setFont(Font.font("Segoe UI", javafx.scene.text.FontWeight.BOLD, 13));

        recentCommandsList = new ListView<>();
        recentCommandsList.getItems().addAll("help", "list", "time");
        recentCommandsList.setPrefHeight(120);
        recentCommandsList.setStyle("-fx-control-inner-background: #5a189a; -fx-text-fill: #E0AAFF; -fx-font-size: 11px; -fx-border-color: #9D4EDD;");

        // Theme Toggle
        Button themeBtn = createPurpleButton("☀ LIGHT MODE", "#5b8cc9");
        themeBtn.setOnAction(e -> toggleTheme());

        // Footer Info
        Label footerLabel = new Label("JAVAdesk v2.0 | PROFESSIONAL EDITION\n© 2026 COMMAND CENTER");
        footerLabel.setTextFill(Color.web("#4a90e2"));
        footerLabel.setFont(Font.font("Segoe UI", 9));
        footerLabel.setWrapText(true);

        rightPanel.getChildren().addAll(infoLabel, systemStatus, new Separator(), 
                                        recentLabel, recentCommandsList, new Separator(), 
                                        themeBtn);
        rightPanel.getChildren().add(footerLabel);
        VBox.setVgrow(recentCommandsList, Priority.ALWAYS);

        return rightPanel;
    }

    private HBox createSearchBar() {
        TextField searchField = new TextField();
        searchField.setPromptText("SEARCH COMMANDS > ");
        searchField.setPrefHeight(35);
        searchField.setStyle(
                "-fx-background-color: #2d5a8c;" +
                "-fx-text-fill: #cce5ff;" +
                "-fx-prompt-text-fill: #a8d0f7;" +
                "-fx-font-size: 11px;" +
                "-fx-font-family: 'Segoe UI';" +
                "-fx-border-color: #a8d0f7;" +
                "-fx-border-width: 1;"
        );

        Button searchBtn = createPurpleButton("🔍", "#4a90e2");
        HBox searchBox = new HBox(8, searchField, searchBtn);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        return searchBox;
    }

    private void executeCommand() {
        String command = inputField.getText().trim();
        if (!command.isEmpty()) {
            recentCommandsList.getItems().add(0, command);
            if (recentCommandsList.getItems().size() > 10) {
                recentCommandsList.getItems().remove(10);
            }

            String result = processCommand(command);
            outputArea.appendText("┌─ Command: " + command + "\n");
            outputArea.appendText("├─ Output:\n");
            outputArea.appendText("│  " + result.replace("\n", "\n│  ") + "\n");
            outputArea.appendText("└─────────────────────────────────\n\n");
            inputField.clear();
            updateStatus("Command executed: " + command);
        }
    }

    private String processCommand(String command) {
        String[] parts = command.toLowerCase().split(" ", 2);
        String cmd = parts[0];
        String args = parts.length > 1 ? parts[1] : "";
        
        return switch (cmd) {
            case "help" -> "Available Commands:\n" +
                          "  • help - Show this help menu\n" +
                          "  • time - Show current time\n" +
                          "  • date - Show current date\n" +
                          "  • info - Show system information\n" +
                          "  • clear - Clear console\n" +
                          "  • list - Show recent commands\n" +
                          "  • custom - Show custom commands\n" +
                          "  • echo [text] - Echo back the text\n" +
                          "  • calc [math] - Calculate math expression (e.g., calc 2+3*4)\n" +
                          "  • memory - Show memory usage\n" +
                          "  • cpu - Show CPU info\n" +
                          "  • uptime - Show system uptime";
            
            case "time" -> {
                String currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                yield "⏰ Current Time: " + currentTime;
            }
            
            case "date" -> {
                String currentDate = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy"));
                yield "📅 Current Date: " + currentDate;
            }
            
            case "echo" -> {
                if (args.isEmpty()) {
                    yield "Usage: echo [text]";
                }
                yield "Echo: " + args;
            }
            
            case "calc" -> {
                if (args.isEmpty()) {
                    yield "Usage: calc [expression]\nExample: calc 10+5*2";
                }
                try {
                    double result = evaluateExpression(args);
                    yield "📊 Result: " + args + " = " + result;
                } catch (Exception e) {
                    yield "❌ Error: Invalid expression. Use basic math (+, -, *, /)";
                }
            }
            
            case "memory" -> {
                Runtime runtime = Runtime.getRuntime();
                long maxMemory = runtime.maxMemory() / 1048576;
                long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1048576;
                long freeMemory = runtime.freeMemory() / 1048576;
                yield "💾 Memory Usage:\n" +
                      "  • Used: " + usedMemory + " MB\n" +
                      "  • Free: " + freeMemory + " MB\n" +
                      "  • Max: " + maxMemory + " MB";
            }
            
            case "cpu" -> {
                int processors = Runtime.getRuntime().availableProcessors();
                String osName = System.getProperty("os.name");
                String osVersion = System.getProperty("os.version");
                yield "⚙️ CPU Information:\n" +
                      "  • Processors: " + processors + "\n" +
                      "  • OS: " + osName + " " + osVersion;
            }
            
            case "uptime" -> {
                long uptime = java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime() / 1000;
                long hours = uptime / 3600;
                long minutes = (uptime % 3600) / 60;
                long seconds = uptime % 60;
                yield "⏱️ System Uptime: " + hours + "h " + minutes + "m " + seconds + "s";
            }
            
            case "info" -> "ℹ️ System Information:\n" +
                          "  • JavaDesk v1.0\n" +
                          "  • Java Version: " + System.getProperty("java.version") + "\n" +
                          "  • OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version") + "\n" +
                          "  • User: " + System.getProperty("user.name");
            
            case "list" -> {
                if (recentCommandsList.getItems().isEmpty()) {
                    yield "No recent commands yet.";
                }
                StringBuilder sb = new StringBuilder("📋 Recent Commands:\n");
                for (int i = 0; i < Math.min(5, recentCommandsList.getItems().size()); i++) {
                    sb.append("  ").append(i + 1).append(". ").append(recentCommandsList.getItems().get(i)).append("\n");
                }
                yield sb.toString();
            }
            
            case "custom" -> {
                if (customCommands.isEmpty()) {
                    yield "No custom commands created yet. Use 'New Command' button to create one!";
                } else {
                    StringBuilder sb = new StringBuilder("🔧 Custom Commands:\n");
                    customCommands.forEach((cmd1, desc) -> sb.append("  • ").append(cmd1).append(" - ").append(desc.split("\n")[0]).append("\n"));
                    yield sb.toString();
                }
            }
            
            case "clear" -> {
                outputArea.clear();
                yield "Console cleared.";
            }
            
            default -> {
                if (customCommands.containsKey(cmd)) {
                    yield "✓ Custom Command Output:\n" + customCommands.get(cmd);
                } else {
                    yield "❌ Unknown command: '" + cmd + "'. Type 'help' for available commands.";
                }
            }
        };
    }
    
    private double evaluateExpression(String expr) throws Exception {
        expr = expr.replaceAll("\\s+", "");
        
        // Simple expression evaluator (handles +, -, *, /)
        return evaluate(expr);
    }
    
    private double evaluate(String expr) throws Exception {
        // Handle addition and subtraction (lowest precedence)
        int lastPlus = Math.max(expr.lastIndexOf('+'), expr.lastIndexOf('-'));
        if (lastPlus > 0) {
            String left = expr.substring(0, lastPlus);
            String right = expr.substring(lastPlus + 1);
            char op = expr.charAt(lastPlus);
            
            if (op == '+') return evaluate(left) + evaluate(right);
            else return evaluate(left) - evaluate(right);
        }
        
        // Handle multiplication and division (higher precedence)
        int lastMulDiv = Math.max(expr.lastIndexOf('*'), expr.lastIndexOf('/'));
        if (lastMulDiv > 0) {
            String left = expr.substring(0, lastMulDiv);
            String right = expr.substring(lastMulDiv + 1);
            char op = expr.charAt(lastMulDiv);
            
            if (op == '*') return evaluate(left) * evaluate(right);
            else return evaluate(left) / evaluate(right);
        }
        
        // Parse number
        return Double.parseDouble(expr);
    }

    private String getCurrentTime() {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    private void startClockUpdate() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);
                    javafx.application.Platform.runLater(() -> 
                        timeLabel.setText(getCurrentTime())
                    );
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }

    private void updateStatus(String message) {
        statusLabel.setText("Status: " + message);
        PauseTransition pause = new PauseTransition(Duration.seconds(3));
        pause.setOnFinished(e -> statusLabel.setText("Status: Ready | System Active"));
        pause.play();
    }

    private Button createPurpleButton(String text, String accentColor) {
        Button btn = new Button(text);
        btn.setPrefWidth(120);
        btn.setStyle(
                "-fx-background-color: #2d5a8c;" +
                "-fx-text-fill: " + accentColor + ";" +
                "-fx-font-size: 11px;" +
                "-fx-font-family: 'Segoe UI';" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 4;" +
                "-fx-padding: 8 16 8 16;" +
                "-fx-border-color: " + accentColor + ";" +
                "-fx-border-width: 1;" +
                "-fx-cursor: hand;"
        );

        btn.setOnMouseEntered(e ->
                btn.setStyle(
                        "-fx-background-color: " + accentColor + ";" +
                        "-fx-text-fill: #0d1b2a;" +
                        "-fx-font-size: 11px;" +
                        "-fx-font-family: 'Segoe UI';" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 4;" +
                        "-fx-padding: 8 16 8 16;" +
                        "-fx-border-color: " + accentColor + ";" +
                        "-fx-border-width: 2;" +
                        "-fx-cursor: hand;"
                )
        );

        btn.setOnMouseExited(e ->
                btn.setStyle(
                        "-fx-background-color: #2d5a8c;" +
                        "-fx-text-fill: " + accentColor + ";" +
                        "-fx-font-size: 11px;" +
                        "-fx-font-family: 'Segoe UI';" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 4;" +
                        "-fx-padding: 8 16 8 16;" +
                        "-fx-border-color: " + accentColor + ";" +
                        "-fx-border-width: 1;" +
                        "-fx-cursor: hand;"
                )
        );

        return btn;
    }

    private String adjustColor(String color) {
        return color.equals("#C4F2A5") ? "#B3E894" : 
               color.equals("#D4A5F2") ? "#C394E1" :
               color.equals("#F2D4A5") ? "#E1C394" : color;
    }

    private void showCreateCommandDialog() {
        Stage dialogStage = new Stage();
        dialogStage.setTitle("CREATE COMMAND");
        
        VBox dialogContent = new VBox(15);
        dialogContent.setPadding(new Insets(20));
        dialogContent.setStyle(isDarkMode ? "-fx-background-color: #1a2f4a;" : "-fx-background-color: #FFFBF7;");
        
        Label cmdNameLabel = new Label("COMMAND NAME:");
        cmdNameLabel.setTextFill(isDarkMode ? Color.web("#a8d0f7") : Color.web("#6B5B95"));
        cmdNameLabel.setFont(Font.font("Segoe UI", javafx.scene.text.FontWeight.BOLD, 12));
        
        TextField cmdNameField = new TextField();
        cmdNameField.setPromptText("e.g., greet, welcome, status");
        cmdNameField.setStyle(isDarkMode ? 
            "-fx-background-color: #2d5a8c; -fx-text-fill: #cce5ff; -fx-prompt-text-fill: #a8d0f7; -fx-border-color: #4a90e2; -fx-border-width: 2; -fx-font-family: 'Segoe UI';" :
            "-fx-background-color: #F5F1E8; -fx-text-fill: #6B5B95; -fx-border-color: #E8D5F2; -fx-border-width: 1; -fx-font-family: 'Segoe UI';");
        
        Label cmdOutputLabel = new Label("COMMAND OUTPUT:");
        cmdOutputLabel.setTextFill(isDarkMode ? Color.web("#cce5ff") : Color.web("#6B5B95"));
        cmdOutputLabel.setFont(Font.font("Segoe UI", javafx.scene.text.FontWeight.BOLD, 12));
        
        TextArea cmdOutputArea = new TextArea();
        cmdOutputArea.setPromptText("What should this command display?");
        cmdOutputArea.setPrefRowCount(5);
        cmdOutputArea.setStyle(isDarkMode ?
            "-fx-control-inner-background: #2d5a8c; -fx-text-fill: #cce5ff; -fx-prompt-text-fill: #a8d0f7; -fx-border-color: #4a90e2; -fx-border-width: 2; -fx-font-family: 'Segoe UI';" :
            "-fx-control-inner-background: #F5F1E8; -fx-text-fill: #6B5B95; -fx-border-color: #E8D5F2; -fx-border-width: 1; -fx-font-family: 'Segoe UI';");
        
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        
        Button saveBtn = createPurpleButton("✓ SAVE", "#16A085");
        Button cancelBtn = createPurpleButton("✗ CANCEL", "#E74C3C");
        
        saveBtn.setOnAction(e -> {
            String cmdName = cmdNameField.getText().trim().toLowerCase();
            String cmdOutput = cmdOutputArea.getText().trim();
            
            if (cmdName.isEmpty() || cmdOutput.isEmpty()) {
                showAlert("ERROR", "Please fill in both fields!", isDarkMode);
                return;
            }
            
            if (cmdName.equals("help") || cmdName.equals("clear") || cmdName.equals("time") || 
                cmdName.equals("list") || cmdName.equals("info") || cmdName.equals("custom")) {
                showAlert("ERROR", "Cannot override built-in commands!", isDarkMode);
                return;
            }
            
            customCommands.put(cmdName, cmdOutput);
            outputArea.appendText("✓ COMMAND CREATED: '" + cmdName + "'\n\n");
            updateStatus("Custom command created: " + cmdName);
            dialogStage.close();
        });
        
        cancelBtn.setOnAction(e -> dialogStage.close());
        
        buttonBox.getChildren().addAll(saveBtn, cancelBtn);
        
        dialogContent.getChildren().addAll(cmdNameLabel, cmdNameField, cmdOutputLabel, cmdOutputArea, buttonBox);
        
        Scene dialogScene = new Scene(dialogContent, 450, 350);
        dialogStage.setScene(dialogScene);
        dialogStage.show();
    }
    
    private void showAlert(String title, String message, boolean isDark) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void toggleTheme() {
        isDarkMode = !isDarkMode;
        
        if (isDarkMode) {
            // Apply dark blue theme (now default)
            root.setStyle("-fx-background-color: #0d1b2a;");
            
            topBar.setStyle("-fx-background-color: #1a2f4a; -fx-border-color: #4a90e2; -fx-border-width: 0 0 2 0;");
            bottomBar.setStyle("-fx-background-color: #1a2f4a; -fx-border-color: #4a90e2; -fx-border-width: 2 0 0 0;");
            
            leftPanel.setStyle("-fx-background-color: #1a2f4a; -fx-border-color: #4a90e2; -fx-border-width: 0 2 0 0;");
            centerPanel.setStyle("-fx-background-color: #0d1b2a;");
            rightPanel.setStyle("-fx-background-color: #1a2f4a; -fx-border-color: #4a90e2; -fx-border-width: 0 0 0 2;");
            
            outputArea.setStyle(
                    "-fx-control-inner-background: #2d5a8c;" +
                    "-fx-text-fill: #cce5ff;" +
                    "-fx-font-size: 12px;" +
                    "-fx-font-family: 'Segoe UI';" +
                    "-fx-border-color: #4a90e2;" +
                    "-fx-border-width: 2;"
            );
            
            inputField.setStyle(
                    "-fx-background-color: #2d5a8c;" +
                    "-fx-text-fill: #cce5ff;" +
                    "-fx-prompt-text-fill: #a8d0f7;" +
                    "-fx-font-size: 12px;" +
                    "-fx-font-family: 'Segoe UI';" +
                    "-fx-border-color: #4a90e2;" +
                    "-fx-border-width: 2;" +
                    "-fx-padding: 10;"
            );
            
            recentCommandsList.setStyle("-fx-control-inner-background: #2d5a8c; -fx-text-fill: #cce5ff; -fx-font-size: 11px; -fx-border-color: #4a90e2;");
            
            // Update all labels for dark mode
            timeLabel.setTextFill(Color.web("#cce5ff"));
            statusLabel.setTextFill(Color.web("#cce5ff"));
            statusLabel.setText("STATUS: READY | SYSTEM ONLINE");
            
        } else {
            // Apply light theme
            root.setStyle("-fx-background-color: #F5F1E8;");
            
            topBar.setStyle("-fx-background-color: #E8D5F2; -fx-border-color: #D4C0E8; -fx-border-width: 0 0 2 0;");
            bottomBar.setStyle("-fx-background-color: #F0E6F6; -fx-border-color: #D4C0E8; -fx-border-width: 2 0 0 0;");
            
            leftPanel.setStyle("-fx-background-color: #F9F4F0; -fx-border-color: #E8D5F2; -fx-border-width: 0 2 0 0;");
            centerPanel.setStyle("-fx-background-color: #F5F1E8;");
            rightPanel.setStyle("-fx-background-color: #F9F4F0; -fx-border-color: #E8D5F2; -fx-border-width: 0 0 0 2;");
            
            outputArea.setStyle(
                    "-fx-control-inner-background: #FFFBF7;" +
                    "-fx-text-fill: #6B5B95;" +
                    "-fx-font-size: 12px;" +
                    "-fx-font-family: 'Segoe UI';" +
                    "-fx-border-color: #E8D5F2;" +
                    "-fx-border-width: 1;"
            );
            
            inputField.setStyle(
                    "-fx-background-color: #FFFBF7;" +
                    "-fx-text-fill: #6B5B95;" +
                    "-fx-prompt-text-fill: #B3A0C9;" +
                    "-fx-font-size: 12px;" +
                    "-fx-border-color: #E8D5F2;" +
                    "-fx-border-width: 1;" +
                    "-fx-padding: 10;"
            );
            
            recentCommandsList.setStyle("-fx-control-inner-background: #FFFBF7; -fx-text-fill: #6B5B95; -fx-font-size: 11px;");
            
            // Update all labels for light mode
            timeLabel.setTextFill(Color.web("#9B88B3"));
            statusLabel.setTextFill(Color.web("#6B5B95"));
            statusLabel.setText("Status: Ready | System Active");
        }
    }
    
    private void updateAllLabels(Color primaryColor, Color secondaryColor) {
        // Find and update all labels in the UI
        updateLabelColor(leftPanel, primaryColor, secondaryColor);
        updateLabelColor(centerPanel, primaryColor, secondaryColor);
        updateLabelColor(rightPanel, primaryColor, secondaryColor);
    }
    
    private void updateLabelColor(Parent parent, Color primaryColor, Color secondaryColor) {
        parent.getChildrenUnmodifiable().forEach(node -> {
            if (node instanceof Label label) {
                label.setTextFill(primaryColor);
            } else if (node instanceof Parent p) {
                updateLabelColor(p, primaryColor, secondaryColor);
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
