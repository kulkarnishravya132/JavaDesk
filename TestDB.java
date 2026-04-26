public class TestDB {
    public static void main(String[] args) {

        DatabaseManager db = new DatabaseManager();

        db.setup();

        db.saveMacro("TestMacro", "click;type;enter");

        var list = db.loadAllMacros();

        for (MacroObject m : list) {
            System.out.println(m.getName() + " -> " + m.getActions());
        }
    }
}