import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Arrays;
import java.util.function.Consumer;

/**
 * Sign-in / create-account window shown before the task scheduler.
 * Each account only ever sees its own tasks.
 */
public class LoginFrame extends JFrame {

    private final AuthManager auth;
    private final Consumer<String> onSuccess;
    private boolean registerMode = false;

    private final Theme.HintTextField usernameField = new Theme.HintTextField("Enter your username");
    private final Theme.HintPasswordField passwordField = new Theme.HintPasswordField("Enter your password");
    private final Theme.HintPasswordField confirmField = new Theme.HintPasswordField("Re-enter your password");

    private final JLabel heading = Theme.label("", Font.BOLD, 24, Theme.TEXT);
    private final JLabel subheading = Theme.label("", Font.PLAIN, 13, Theme.MUTED);
    private final JLabel errorLabel = Theme.label(" ", Font.PLAIN, 12, Theme.DANGER);
    private final Theme.RoundButton submitButton =
            new Theme.RoundButton("Sign in", Theme.PRIMARY, Theme.RoundButton.Style.FILLED);
    private final JLabel switchPrompt = Theme.label("", Font.PLAIN, 13, Theme.MUTED);
    private final Theme.RoundButton switchButton =
            new Theme.RoundButton("", Theme.ACCENT, Theme.RoundButton.Style.GHOST);
    private final JPanel confirmRow = new JPanel(new BorderLayout(0, 6));
    private final JCheckBox showPassword = new JCheckBox("Show password");

    public LoginFrame(AuthManager auth, Consumer<String> onSuccess) {
        this.auth = auth;
        this.onSuccess = onSuccess;

        setTitle("Task Scheduler - Sign in");
        setIconImage(Theme.windowIcon());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        JPanel root = new JPanel(new GridLayout(1, 2));
        root.add(createBrandPanel());
        root.add(createFormPanel());
        setContentPane(root);

        setSize(880, 560);
        setLocationRelativeTo(null);
        applyMode();

        if (!auth.isDatabaseReady()) {
            showError("Database not available. Check that sqlite-jdbc is on the classpath.");
            submitButton.setEnabled(false);
        }
    }

    // ===== LEFT SIDE: branding =====
    private JPanel createBrandPanel() {
        JPanel brand = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, Theme.PRIMARY, getWidth(), getHeight(), Theme.PRIMARY_DARK));
                g2.fillRect(0, 0, getWidth(), getHeight());
                // soft decorative circles
                g2.setColor(new Color(255, 255, 255, 18));
                g2.fillOval(getWidth() - 220, -90, 320, 320);
                g2.fillOval(-120, getHeight() - 180, 280, 280);
                g2.dispose();
            }
        };
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        brand.setBorder(new EmptyBorder(56, 48, 48, 48));

        JComponent logo = Theme.logo(56, Color.WHITE, Theme.PRIMARY);
        logo.setAlignmentX(LEFT_ALIGNMENT);

        JLabel name = Theme.label("Task Scheduler", Font.BOLD, 30, Color.WHITE);
        name.setAlignmentX(LEFT_ALIGNMENT);
        JLabel team = Theme.label("GEMS 3", Font.BOLD, 13, new Color(190, 205, 235));
        team.setAlignmentX(LEFT_ALIGNMENT);

        JLabel tagline = new JLabel("<html><div style='width:250px'>Plan your deadlines, keep track of what's "
                + "pending, and check things off as you go.</div></html>");
        tagline.setFont(Theme.font(Font.PLAIN, 15));
        tagline.setForeground(new Color(225, 232, 245));
        tagline.setAlignmentX(LEFT_ALIGNMENT);

        brand.add(logo);
        brand.add(Box.createVerticalStrut(28));
        brand.add(team);
        brand.add(Box.createVerticalStrut(4));
        brand.add(name);
        brand.add(Box.createVerticalStrut(16));
        brand.add(tagline);
        brand.add(Box.createVerticalGlue());
        brand.add(feature("Your own private task list"));
        brand.add(Box.createVerticalStrut(10));
        brand.add(feature("Sort by status, deadline or priority"));
        brand.add(Box.createVerticalStrut(10));
        brand.add(feature("Deadlines can't be set in the past"));
        return brand;
    }

    private JComponent feature(String text) {
        JLabel l = Theme.label("✓   " + text, Font.PLAIN, 14, Color.WHITE);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    // ===== RIGHT SIDE: form =====
    private JPanel createFormPanel() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(Theme.SURFACE);

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setPreferredSize(new Dimension(330, 440));

        heading.setAlignmentX(LEFT_ALIGNMENT);
        subheading.setAlignmentX(LEFT_ALIGNMENT);

        JPanel userRow = fieldRow("Username", Theme.inputBox(usernameField, null));
        JPanel passRow = fieldRow("Password", Theme.inputBox(passwordField, null));

        confirmRow.setOpaque(false);
        confirmRow.add(Theme.label("Confirm password", Font.BOLD, 12, Theme.TEXT), BorderLayout.NORTH);
        confirmRow.add(Theme.inputBox(confirmField, null), BorderLayout.CENTER);
        confirmRow.setAlignmentX(LEFT_ALIGNMENT);
        confirmRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        showPassword.setOpaque(false);
        showPassword.setFont(Theme.font(Font.PLAIN, 12));
        showPassword.setForeground(Theme.MUTED);
        showPassword.setFocusPainted(false);
        showPassword.setAlignmentX(LEFT_ALIGNMENT);
        showPassword.addActionListener(e -> {
            char echo = showPassword.isSelected() ? (char) 0 : '•';
            passwordField.setEchoChar(echo);
            confirmField.setEchoChar(echo);
        });
        passwordField.setEchoChar('•');
        confirmField.setEchoChar('•');

        errorLabel.setAlignmentX(LEFT_ALIGNMENT);
        errorLabel.setVerticalAlignment(SwingConstants.TOP);
        errorLabel.setPreferredSize(new Dimension(330, 34));
        errorLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        submitButton.setFont(Theme.font(Font.BOLD, 14));
        submitButton.setAlignmentX(LEFT_ALIGNMENT);
        submitButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        submitButton.addActionListener(e -> submit());

        JPanel switchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        switchRow.setOpaque(false);
        switchRow.setAlignmentX(LEFT_ALIGNMENT);
        switchButton.setBorder(new EmptyBorder(4, 6, 4, 6));
        switchButton.addActionListener(e -> { registerMode = !registerMode; applyMode(); });
        switchRow.add(switchPrompt);
        switchRow.add(switchButton);

        form.add(heading);
        form.add(Box.createVerticalStrut(6));
        form.add(subheading);
        form.add(Box.createVerticalStrut(28));
        form.add(userRow);
        form.add(Box.createVerticalStrut(14));
        form.add(passRow);
        form.add(Box.createVerticalStrut(14));
        form.add(confirmRow);
        form.add(Box.createVerticalStrut(6));
        form.add(showPassword);
        form.add(Box.createVerticalStrut(6));
        form.add(errorLabel);
        form.add(Box.createVerticalStrut(10));
        form.add(submitButton);
        form.add(Box.createVerticalStrut(14));
        form.add(switchRow);
        form.add(Box.createVerticalGlue());

        // Enter key submits from any field
        usernameField.addActionListener(e -> passwordField.requestFocusInWindow());
        passwordField.addActionListener(e -> { if (registerMode) confirmField.requestFocusInWindow(); else submit(); });
        confirmField.addActionListener(e -> submit());

        outer.add(form);
        return outer;
    }

    private JPanel fieldRow(String label, JComponent input) {
        JPanel row = new JPanel(new BorderLayout(0, 6));
        row.setOpaque(false);
        row.add(Theme.label(label, Font.BOLD, 12, Theme.TEXT), BorderLayout.NORTH);
        row.add(input, BorderLayout.CENTER);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        return row;
    }

    private void applyMode() {
        if (registerMode) {
            heading.setText("Create your account");
            subheading.setText("Pick a username and a password (6+ characters).");
            submitButton.setText("Create account");
            switchPrompt.setText("Already have an account?");
            switchButton.setText("Sign in");
        } else {
            heading.setText("Welcome back");
            subheading.setText("Sign in to see your tasks.");
            submitButton.setText("Sign in");
            switchPrompt.setText("New here?");
            switchButton.setText("Create an account");
        }
        confirmRow.setVisible(registerMode);
        confirmField.setText("");
        errorLabel.setText(" ");
        revalidate();
        repaint();
        SwingUtilities.invokeLater(usernameField::requestFocusInWindow);
    }

    private void submit() {
        String username = usernameField.getText();
        char[] password = passwordField.getPassword();
        char[] confirm = confirmField.getPassword();
        try {
            if (registerMode) {
                String problem = auth.register(username, password, confirm);
                if (problem != null) {
                    showError(problem);
                    return;
                }
                finish(username.trim());
            } else {
                String user = auth.login(username, password);
                if (user == null) {
                    showError("Incorrect username or password.");
                    passwordField.setText("");
                    passwordField.requestFocusInWindow();
                    return;
                }
                finish(user);
            }
        } finally {
            Arrays.fill(password, '\0');
            Arrays.fill(confirm, '\0');
        }
    }

    private void finish(String username) {
        dispose();
        onSuccess.accept(username);
    }

    private void showError(String message) {
        errorLabel.setText("<html><div style='width:270px'>" + message + "</div></html>");
    }
}
