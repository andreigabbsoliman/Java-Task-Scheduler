import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * Shared colors, fonts and small custom-painted components used by the
 * login screen and the main window, so the whole app looks consistent.
 */
public final class Theme {

    private Theme() { }

    // ===== COLORS =====
    public static final Color BG           = new Color(243, 245, 249);
    public static final Color SURFACE      = Color.WHITE;
    public static final Color BORDER       = new Color(222, 227, 235);
    public static final Color PRIMARY      = new Color(40, 70, 120);
    public static final Color PRIMARY_DARK = new Color(24, 44, 82);
    public static final Color ACCENT       = new Color(59, 110, 200);
    public static final Color TEXT         = new Color(28, 34, 46);
    public static final Color MUTED        = new Color(108, 117, 134);
    public static final Color SUCCESS      = new Color(38, 135, 82);
    public static final Color DANGER       = new Color(196, 56, 56);
    public static final Color WARNING      = new Color(196, 132, 12);
    public static final Color SELECTION    = new Color(226, 235, 251);
    public static final Color HEADER_BG    = new Color(248, 249, 252);

    // ===== FONTS =====
    private static final String FAMILY = pickFontFamily();

    private static String pickFontFamily() {
        String[] preferred = {"Segoe UI", "Inter", "SF Pro Text", "Helvetica Neue", "Roboto", "Arial"};
        String[] available = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        for (String wanted : preferred) {
            for (String have : available) {
                if (have.equalsIgnoreCase(wanted)) return have;
            }
        }
        return Font.SANS_SERIF;
    }

    public static Font font(int style, float size) {
        return new Font(FAMILY, style, Math.round(size));
    }

    public static JLabel label(String text, int style, float size, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font(style, size));
        label.setForeground(color);
        return label;
    }

    // ===== ROUNDED PANEL (cards, input boxes) =====
    public static class RoundedPanel extends JPanel {
        private final int radius;
        private Color fill;
        private Color outline;

        public RoundedPanel(LayoutManager layout, int radius, Color fill, Color outline) {
            super(layout);
            this.radius = radius;
            this.fill = fill;
            this.outline = outline;
            setOpaque(false);
        }

        public void setOutline(Color outline) {
            this.outline = outline;
            repaint();
        }

        public void setFill(Color fill) {
            this.fill = fill;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (fill != null) {
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            }
            if (outline != null) {
                g2.setColor(outline);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** A white card with a light border and inner padding. */
    public static RoundedPanel card(LayoutManager layout, int padding) {
        RoundedPanel card = new RoundedPanel(layout, 16, SURFACE, BORDER);
        card.setBorder(new EmptyBorder(padding, padding, padding, padding));
        return card;
    }

    /**
     * Wraps a text component in a rounded white box whose outline turns blue
     * while the field has focus. An optional trailing component (like a
     * calendar button) sits on the right side.
     */
    public static RoundedPanel inputBox(JComponent field, JComponent trailing) {
        RoundedPanel box = new RoundedPanel(new BorderLayout(6, 0), 10, SURFACE, BORDER);
        box.setBorder(new EmptyBorder(2, 10, 2, trailing == null ? 10 : 4));
        field.setBorder(new EmptyBorder(8, 0, 8, 0));
        field.setOpaque(false);
        field.setFont(font(Font.PLAIN, 14));
        field.setForeground(TEXT);
        if (field instanceof javax.swing.text.JTextComponent) {
            ((javax.swing.text.JTextComponent) field).setCaretColor(TEXT);
        }
        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { box.setOutline(ACCENT); }
            @Override public void focusLost(FocusEvent e)   { box.setOutline(BORDER); }
        });
        box.add(field, BorderLayout.CENTER);
        if (trailing != null) box.add(trailing, BorderLayout.EAST);
        return box;
    }

    // ===== TEXT FIELD WITH PLACEHOLDER =====
    public static class HintTextField extends JTextField {
        private final String hint;

        public HintTextField(String hint) {
            this.hint = hint;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty() && hint != null) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(new Color(160, 167, 180));
                g2.setFont(getFont());
                Insets in = getInsets();
                FontMetrics fm = g2.getFontMetrics();
                int y = in.top + (getHeight() - in.top - in.bottom - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(hint, in.left, y);
                g2.dispose();
            }
        }
    }

    public static class HintPasswordField extends JPasswordField {
        private final String hint;

        public HintPasswordField(String hint) {
            this.hint = hint;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getPassword().length == 0 && hint != null) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(new Color(160, 167, 180));
                g2.setFont(getFont());
                Insets in = getInsets();
                FontMetrics fm = g2.getFontMetrics();
                int y = in.top + (getHeight() - in.top - in.bottom - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(hint, in.left, y);
                g2.dispose();
            }
        }
    }

    // ===== ROUNDED BUTTON =====
    public static class RoundButton extends JButton {
        public enum Style { FILLED, OUTLINE, GHOST }

        private Color base;
        private final Style style;

        public RoundButton(String text, Color base, Style style) {
            super(text);
            this.base = base;
            this.style = style;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setFont(font(Font.BOLD, 13));
            setForeground(style == Style.FILLED ? Color.WHITE : base);
            setBorder(new EmptyBorder(9, 18, 9, 18));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        public void setBaseColor(Color color) {
            this.base = color;
            if (style != Style.FILLED) setForeground(color);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            ButtonModel m = getModel();
            int w = getWidth(), h = getHeight();
            if (!isEnabled()) {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f));
            }
            if (style == Style.FILLED) {
                Color c = base;
                if (m.isPressed()) c = mix(base, Color.BLACK, 0.18);
                else if (m.isRollover()) c = mix(base, Color.WHITE, 0.12);
                g2.setColor(c);
                g2.fillRoundRect(0, 0, w, h, 10, 10);
            } else {
                if (m.isRollover() || m.isPressed()) {
                    g2.setColor(withAlpha(base, m.isPressed() ? 40 : 22));
                    g2.fillRoundRect(0, 0, w, h, 10, 10);
                }
                if (style == Style.OUTLINE) {
                    g2.setColor(withAlpha(base, 120));
                    g2.drawRoundRect(0, 0, w - 1, h - 1, 10, 10);
                }
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ===== COLOR HELPERS =====
    public static Color mix(Color a, Color b, double amount) {
        int r = (int) Math.round(a.getRed()   + (b.getRed()   - a.getRed())   * amount);
        int g = (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * amount);
        int bl = (int) Math.round(a.getBlue() + (b.getBlue()  - a.getBlue())  * amount);
        return new Color(r, g, bl);
    }

    public static Color withAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    /** Draws the app's checkmark logo. Used for the window icon and headers. */
    public static void paintLogo(Graphics2D g2, int x, int y, int size, Color bg, Color fg) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(bg);
        g2.fillRoundRect(x, y, size, size, size / 3, size / 3);
        g2.setColor(fg);
        g2.setStroke(new BasicStroke(size / 10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(x + size * 26 / 100, y + size * 53 / 100, x + size * 43 / 100, y + size * 70 / 100);
        g2.drawLine(x + size * 43 / 100, y + size * 70 / 100, x + size * 76 / 100, y + size * 32 / 100);
    }

    public static Image windowIcon() {
        java.awt.image.BufferedImage image =
                new java.awt.image.BufferedImage(64, 64, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        paintLogo(g2, 2, 2, 60, PRIMARY, Color.WHITE);
        g2.dispose();
        return image;
    }

    /** A small square component that just paints the logo. */
    public static JComponent logo(int size, Color bg, Color fg) {
        JComponent c = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                paintLogo(g2, 0, 0, size, bg, fg);
                g2.dispose();
            }
        };
        c.setPreferredSize(new Dimension(size, size));
        c.setMinimumSize(new Dimension(size, size));
        c.setMaximumSize(new Dimension(size, size));
        return c;
    }
}
