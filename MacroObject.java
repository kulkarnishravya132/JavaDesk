public class MacroObject {
    private String name;
    private String actions;

    public MacroObject(String name, String actions) {
        this.name = name;
        this.actions = actions;
    }

    public String getName() {
        return name;
    }

    public String getActions() {
        return actions;
    }
}