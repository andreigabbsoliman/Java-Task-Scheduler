import javax.swing.*;

public class Main {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Unable to apply UI: " + e.getMessage());
        }

        SwingUtilities.invokeLater(() -> {
            TaskDatabase database = new TaskDatabase();
            Runtime.getRuntime().addShutdownHook(new Thread(database::close));
            showLogin(database);
        });
    }

    static void showLogin(TaskDatabase database) {
        AuthManager auth = new AuthManager(database);
        LoginFrame login = new LoginFrame(auth, username -> {
            TaskManager manager = new TaskManager(database, username);
            TaskSchedulerGUI gui = new TaskSchedulerGUI(manager, () -> showLogin(database));
            gui.setVisible(true);
        });
        login.setVisible(true);
    }
}
