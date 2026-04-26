import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:macros.db";

    // setup() → create DB + table
    public void setup() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            String sql = "CREATE TABLE IF NOT EXISTS macros (" +
                         "name TEXT PRIMARY KEY, " +
                         "actions TEXT NOT NULL" +
                         ");";

            stmt.execute(sql);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // save macro
    public void saveMacro(String name, String actions) {
        String sql = "INSERT OR REPLACE INTO macros(name, actions) VALUES(?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, name);
            pstmt.setString(2, actions);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // load all macros
    public List<MacroObject> loadAllMacros() {
        List<MacroObject> list = new ArrayList<>();
        String sql = "SELECT * FROM macros";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                list.add(new MacroObject(
                        rs.getString("name"),
                        rs.getString("actions")
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }
}