import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class TaskDatabase {

    private static final String DB_URL = "jdbc:sqlite:" + findDatabaseFile();
    private Connection connection;

    private static String findDatabaseFile() {
        try {
            java.io.File location = new java.io.File(
                    TaskDatabase.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (location.isFile() && location.getName().endsWith(".jar")) {
                java.io.File srcFolder = new java.io.File(location.getParentFile(), "src");
                srcFolder.mkdirs();
                return new java.io.File(srcFolder, "task_scheduler.db").getAbsolutePath();
            }
        } catch (Exception e) {

        }
        return "task_scheduler.db";
    }

    public TaskDatabase() {
        try {
            connection = DriverManager.getConnection(DB_URL);
            createTables();
        } catch (SQLException e) {
            System.err.println("Database error (tasks will not be saved): " + e.getMessage());
            connection = null;
        }
    }

    public boolean isConnected() {
        return connection != null;
    }

    // ===== TABLE SETUP =====
    private void createTables() throws SQLException {
        try (Statement st = connection.createStatement()) {
            // One row per account. The password itself is never stored, only a salted hash.
            st.execute("CREATE TABLE IF NOT EXISTS users ("
                    + "username TEXT PRIMARY KEY COLLATE NOCASE, "
                    + "salt TEXT NOT NULL, "
                    + "password_hash TEXT NOT NULL, "
                    + "created_at TEXT NOT NULL)");

            if (!tableExists("tasks")) {
                createTasksTable(st);
            } else if (!columnExists("tasks", "owner")) {
                migrateOldTasksTable(st);
            }
        }
    }

    // Every task belongs to one user. Task IDs (#10 - #99) only need to be
    // unique per user, so the key is (owner, task_id).
    private void createTasksTable(Statement st) throws SQLException {
        st.execute("CREATE TABLE tasks ("
                + "owner TEXT NOT NULL COLLATE NOCASE, "
                + "task_id INTEGER NOT NULL, "
                + "add_order INTEGER NOT NULL, "
                + "task_name TEXT NOT NULL, "
                + "description TEXT, "
                + "deadline TEXT NOT NULL, "
                + "priority TEXT NOT NULL, "
                + "status TEXT NOT NULL, "
                + "PRIMARY KEY (owner, task_id))");
    }

    // Older versions of the app had no login. Their tasks are kept with an
    // empty owner and handed to the first account that gets created.
    private void migrateOldTasksTable(Statement st) throws SQLException {
        connection.setAutoCommit(false);
        try {
            st.execute("ALTER TABLE tasks RENAME TO tasks_old");
            createTasksTable(st);
            st.execute("INSERT INTO tasks (owner, task_id, add_order, task_name, description, deadline, priority, status) "
                    + "SELECT '', task_id, add_order, task_name, description, deadline, priority, status FROM tasks_old");
            st.execute("DROP TABLE tasks_old");
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    private boolean tableExists(String table) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?")) {
            ps.setString(1, table);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean columnExists(String table, String column) throws SQLException {
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                if (rs.getString("name").equalsIgnoreCase(column)) return true;
            }
        }
        return false;
    }

    // ===== USERS =====

    /** Returns {username as saved, salt, password hash}, or null if there is no such user. */
    public String[] findUser(String username) {
        if (connection == null) return null;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT username, salt, password_hash FROM users WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new String[]{rs.getString(1), rs.getString(2), rs.getString(3)};
                }
            }
        } catch (SQLException e) {
            System.err.println("Could not read user: " + e.getMessage());
        }
        return null;
    }

    public boolean createUser(String username, String salt, String passwordHash) {
        if (connection == null) return false;
        boolean firstUser = countUsers() == 0;
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO users (username, salt, password_hash, created_at) VALUES (?, ?, ?, datetime('now'))")) {
            ps.setString(1, username);
            ps.setString(2, salt);
            ps.setString(3, passwordHash);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Could not create user: " + e.getMessage());
            return false;
        }
        if (firstUser) {
            run("UPDATE tasks SET owner = ? WHERE owner = ''", username);
        }
        return true;
    }

    private int countUsers() {
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users")) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            return 0;
        }
    }

    // ===== LOAD =====
    public ArrayList<Task> loadTasks(String owner) {
        ArrayList<Task> list = new ArrayList<>();
        if (connection == null) return list;

        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM tasks WHERE owner = ? ORDER BY add_order")) {
            ps.setString(1, owner);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Task(
                            rs.getInt("task_id"),
                            rs.getInt("add_order"),
                            rs.getString("task_name"),
                            rs.getString("description"),
                            rs.getString("deadline"),
                            rs.getString("priority"),
                            rs.getString("status")));
                }
            }
        } catch (SQLException e) {
            System.err.println("Could not load tasks: " + e.getMessage());
        }
        return list;
    }

    // ===== ADD =====
    public void insertTask(String owner, Task t) {
        run("INSERT INTO tasks (owner, task_id, add_order, task_name, description, deadline, priority, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                owner, t.getTaskID(), t.getAddOrder(), t.getTaskName(), t.getDescription(),
                t.getDeadline(), t.getPriority(), t.getStatus());
    }

    // ===== EDIT / CHECKBOX =====
    public void updateTask(String owner, Task t) {
        run("UPDATE tasks SET task_name = ?, description = ?, deadline = ?, priority = ?, status = ? "
                + "WHERE owner = ? AND task_id = ?",
                t.getTaskName(), t.getDescription(), t.getDeadline(), t.getPriority(),
                t.getStatus(), owner, t.getTaskID());
    }

    // ===== DELETE =====
    public void deleteTask(String owner, int taskID) {
        run("DELETE FROM tasks WHERE owner = ? AND task_id = ?", owner, taskID);
    }

    // Runs one INSERT/UPDATE/DELETE with the given values.
    private void run(String sql, Object... values) {
        if (connection == null) return;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) {
                ps.setObject(i + 1, values[i]);
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
    }

    public void close() {
        try {
            if (connection != null) connection.close();
        } catch (SQLException e) {
            System.err.println("Could not close database: " + e.getMessage());
        }
    }
}
