package automation;

import org.testng.IExecutionListener;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicSliderUI;
import java.awt.*;
import java.util.Arrays;
import java.util.Locale;

/**
 * Provides a desktop control panel for changing test speed and result display time while tests run. Restores the original settings and closes when testing finishes.
 */
public final class PresentationController implements IExecutionListener {

    private static final String SLOW_MO_PROPERTY = "browser.slowMo.delayMs";
    private static final String RESULTS_PROPERTY = "browser.showResultsMs";

    // Security Assessment SPA color palette.
    private static final Color NAVY = Color.decode("#111d33");
    private static final Color BACKGROUND = Color.decode("#f5f7fb");
    private static final Color SURFACE = Color.decode("#ffffff");
    private static final Color TEXT = Color.decode("#17243d");
    private static final Color MUTED = Color.decode("#7a879c");
    private static final Color BORDER = Color.decode("#e2e7f0");
    private static final Color ACCENT = Color.decode("#4d7fc2");
    private static final Color ACCENT_DARK = Color.decode("#2a4a7d");
    private static final Color ACCENT_LIGHT = Color.decode("#91bbff");
    private static final Color SUCCESS = Color.decode("#48c29d");
    private static final Color SUCCESS_TEXT = Color.decode("#427f5d");
    private static final Color TRACK = Color.decode("#edf1f6");

    private static final int SLOW_MO_MAX = 3000;
    private static final int RESULTS_MAX = 15000;
    private static final int[] SPEED_PRESETS = {0, 250, 500, 1000};

    private static final String FONT_FAMILY = resolveFontFamily();

    private JFrame frame;
    private String originalSlowMoOverride;
    private String originalResultsOverride;

    /**
     * Opens the control panel when visible slow-motion mode is enabled. Uses Swing's interface thread to create the window safely.
     */
    @Override
    public void onExecutionStart() {
        if (AppConfig.isHeadless()
                || !AppConfig.isSlowMoEnabled()
                || GraphicsEnvironment.isHeadless()) return;

        SwingUtilities.invokeLater(this::createWindow);
    }

    /**
     * Restores the original settings and closes the control panel when testing finishes.
     */
    @Override
    public void onExecutionFinish() {
        SwingUtilities.invokeLater(() -> {
            if (frame == null) return;

            restoreProperty(SLOW_MO_PROPERTY, originalSlowMoOverride);
            restoreProperty(RESULTS_PROPERTY, originalResultsOverride);

            frame.dispose();
            frame = null;
        });
    }

    /**
     * Builds and opens the control panel using the current delay settings.
     */
    private void createWindow() {
        if (frame != null) return;

        originalSlowMoOverride = System.getProperty(SLOW_MO_PROPERTY);
        originalResultsOverride = System.getProperty(RESULTS_PROPERTY);

        int defaultSlowMo = AppConfig.slowMoDelayMs();
        int defaultResults = AppConfig.showResultsMs();

        frame = new JFrame("Presentation Controller");
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.setAlwaysOnTop(true);
        frame.setResizable(false);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BACKGROUND);
        root.add(createHeader(), BorderLayout.NORTH);

        JPanel content = createVerticalPanel(BACKGROUND);
        content.setBorder(new EmptyBorder(22, 22, 18, 22));
        content.setPreferredSize(new Dimension(480, 495));

        content.add(createLabel(
                "RUNTIME CONFIGURATION", ACCENT, 11, Font.BOLD));

        content.add(Box.createVerticalStrut(8));

        content.add(createLabel(
                "Presentation settings", TEXT, 24, Font.BOLD));

        content.add(Box.createVerticalStrut(6));

        content.add(createLabel(
                "Adjust execution speed without restarting your tests.",
                MUTED, 12, Font.PLAIN));

        content.add(Box.createVerticalStrut(20));

        JSlider slowMo = createDelayCard(
                content,
                "INTERACTION DELAY",
                SLOW_MO_PROPERTY,
                defaultSlowMo,
                SLOW_MO_MAX,
                false
        );

        content.add(Box.createVerticalStrut(14));

        JSlider results = createDelayCard(
                content,
                "RESULT DISPLAY DURATION",
                RESULTS_PROPERTY,
                defaultResults,
                RESULTS_MAX,
                true
        );

        content.add(Box.createVerticalStrut(16));

        content.add(createControls(
                slowMo, results, defaultSlowMo, defaultResults));

        content.add(Box.createVerticalGlue());
        content.add(createFooter());

        root.add(content, BorderLayout.CENTER);

        frame.setContentPane(root);
        frame.pack();
        frame.setLocationByPlatform(true);
        frame.setVisible(true);
    }

    /**
     * Creates the navy header using the SPA branding colors.
     *
     * @return styled application header
     */
    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setBackground(NAVY);
        header.setBorder(new EmptyBorder(20, 22, 20, 22));

        RoundedPanel brand = new RoundedPanel(
                Color.decode("#243f70"),
                Color.decode("#365485"),
                12
        );

        brand.setLayout(new GridBagLayout());
        brand.setPreferredSize(new Dimension(44, 44));

        brand.add(createLabel(
                "S", ACCENT_LIGHT, 24, Font.BOLD));

        JPanel titles = createVerticalPanel(NAVY);

        titles.add(createLabel(
                "OWASP SECURITY AUTOMATION",
                SURFACE, 13, Font.BOLD));

        titles.add(Box.createVerticalStrut(5));

        titles.add(createLabel(
                "Presentation Controller",
                Color.decode("#8798b5"), 12, Font.PLAIN));

        JPanel status = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        status.setOpaque(false);

        status.add(createStatusDot());
        status.add(createLabel(
                "Live", Color.decode("#d7e1f3"), 12, Font.PLAIN));

        header.add(brand, BorderLayout.WEST);
        header.add(titles, BorderLayout.CENTER);
        header.add(status, BorderLayout.EAST);

        return header;
    }

    /**
     * Creates a settings card with a slider and live value. Changes take effect immediately, and the interaction delay card includes preset speeds.
     *
     * @param parent parent container
     * @param title card heading
     * @param property runtime setting controlled by the slider
     * @param initial initial configured delay
     * @param maximum suggested maximum delay
     * @param seconds whether to display the value in seconds
     * @return configured delay slider
     */
    private JSlider createDelayCard(
            JPanel parent,
            String title,
            String property,
            int initial,
            int maximum,
            boolean seconds) {

        RoundedPanel card = new RoundedPanel(SURFACE, BORDER, 12);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(16, 16, 16, 16));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel heading = createLabel(
                title, MUTED, 11, Font.BOLD);

        JLabel value = createLabel(
                formatDelay(initial, seconds), TEXT, 29, Font.BOLD);

        JSlider slider = new JSlider(
                0, Math.max(maximum, initial), initial);

        slider.setUI(new PresentationSliderUI(slider));
        slider.setOpaque(false);
        slider.setAlignmentX(Component.LEFT_ALIGNMENT);
        slider.setPreferredSize(new Dimension(400, 30));
        slider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        slider.addChangeListener(e -> {
            int delay = slider.getValue();

            System.setProperty(property, Integer.toString(delay));
            value.setText(formatDelay(delay, seconds));
        });

        card.add(heading);
        card.add(Box.createVerticalStrut(6));
        card.add(value);
        card.add(Box.createVerticalStrut(8));
        card.add(slider);

        JPanel range = new JPanel(new BorderLayout());
        range.setOpaque(false);
        range.setAlignmentX(Component.LEFT_ALIGNMENT);

        range.add(createLabel(
                seconds ? "Immediate" : "Fast",
                MUTED, 11, Font.PLAIN), BorderLayout.WEST);

        range.add(createLabel(
                formatDelay(slider.getMaximum(), seconds),
                MUTED, 11, Font.PLAIN), BorderLayout.EAST);

        card.add(range);
        card.add(Box.createVerticalStrut(12));

        if (seconds) {
            card.add(createLabel(
                    "Time available to visually verify test results.",
                    MUTED, 11, Font.PLAIN));
        } else {
            card.add(createPresets(slider));
        }

        card.setMaximumSize(new Dimension(
                Integer.MAX_VALUE, card.getPreferredSize().height));

        parent.add(card);

        return slider;
    }

    /**
     * Builds one row of interaction-delay preset buttons and updates each selected state whenever the slider changes.
     *
     * @param slider interaction delay slider
     * @return preset button container
     */
    private JPanel createPresets(JSlider slider) {
        JPanel presets = new JPanel(
                new GridLayout(1, SPEED_PRESETS.length, 8, 0));

        presets.setOpaque(false);
        presets.setAlignmentX(Component.LEFT_ALIGNMENT);

        for (int delay : SPEED_PRESETS) {
            StyledButton button = new StyledButton(
                    delay == 0 ? "Fast" : delay + " ms");

            button.setSelected(slider.getValue() == delay);

            button.addActionListener(e -> slider.setValue(delay));

            slider.addChangeListener(e ->
                    button.setSelected(slider.getValue() == delay));

            presets.add(button);
        }

        return presets;
    }

    /**
     * Builds the always-on-top toggle and reset button, restoring the starting slider values when reset is selected.
     *
     * @param slowMo interaction delay slider
     * @param results result duration slider
     * @param defaultSlowMo initial interaction delay
     * @param defaultResults initial result duration
     * @return presentation control row
     */
    private JPanel createControls(
            JSlider slowMo,
            JSlider results,
            int defaultSlowMo,
            int defaultResults) {

        JPanel controls = new JPanel(new BorderLayout(12, 0));
        controls.setOpaque(false);
        controls.setAlignmentX(Component.LEFT_ALIGNMENT);

        JCheckBox alwaysOnTop = new JCheckBox("Always on top", true);
        alwaysOnTop.setOpaque(false);
        alwaysOnTop.setForeground(TEXT);
        alwaysOnTop.setFont(createFont(12, Font.PLAIN));

        alwaysOnTop.addActionListener(e ->
                frame.setAlwaysOnTop(alwaysOnTop.isSelected()));

        StyledButton reset = new StyledButton("Reset defaults");

        reset.addActionListener(e -> {
            slowMo.setValue(defaultSlowMo);
            results.setValue(defaultResults);
        });

        controls.add(alwaysOnTop, BorderLayout.WEST);
        controls.add(reset, BorderLayout.EAST);

        controls.setMaximumSize(new Dimension(
                Integer.MAX_VALUE, controls.getPreferredSize().height));

        return controls;
    }

    /**
     * Builds the status and session-only labels displayed below the presentation controls.
     *
     * @return styled status footer
     */
    private JPanel createFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setAlignmentX(Component.LEFT_ALIGNMENT);

        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER),
                new EmptyBorder(12, 0, 0, 0)
        ));

        JPanel status = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 6, 0));

        status.setOpaque(false);
        status.add(createStatusDot());

        status.add(createLabel(
                "Changes applied in real time",
                SUCCESS_TEXT, 11, Font.PLAIN));

        footer.add(status, BorderLayout.WEST);

        footer.add(createLabel(
                        "Session only", MUTED, 11, Font.PLAIN),
                BorderLayout.EAST);

        footer.setMaximumSize(new Dimension(
                Integer.MAX_VALUE, footer.getPreferredSize().height));

        return footer;
    }

    /**
     * Creates a green status indicator matching the SPA.
     *
     * @return circular status indicator
     */
    private JComponent createStatusDot() {
        JComponent dot = new JComponent() {

            /**
             * Paints the circular status indicator.
             *
             * @param graphics component graphics context
             */
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = prepareGraphics(graphics);

                g.setColor(SUCCESS);
                g.fillOval(1, 1, 8, 8);

                g.dispose();
            }
        };

        dot.setPreferredSize(new Dimension(10, 10));

        return dot;
    }

    /**
     * Creates a vertically arranged panel with the specified background.
     *
     * @param background panel background color
     * @return configured panel
     */
    private JPanel createVerticalPanel(Color background) {
        JPanel panel = new JPanel();

        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(background);

        return panel;
    }

    /**
     * Creates a consistently styled text label.
     *
     * @param text label content
     * @param color foreground color
     * @param size font size
     * @param style font style
     * @return configured label
     */
    private JLabel createLabel(
            String text, Color color, int size, int style) {

        JLabel label = new JLabel(text);

        label.setForeground(color);
        label.setFont(createFont(size, style));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        return label;
    }

    /**
     * Creates a font using the selected application font family.
     *
     * @param size font size
     * @param style font style
     * @return configured font
     */
    private static Font createFont(int size, int style) {
        return new Font(FONT_FAMILY, style, size);
    }

    /**
     * Chooses an available font that matches the dashboard, or uses the system's default sans-serif font.
     *
     * @return available font
     */
    private static String resolveFontFamily() {
        String[] available = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames();

        for (String preferred : new String[]{"Inter", "Segoe UI", "Arial"}) {
            if (Arrays.stream(available)
                    .anyMatch(preferred::equalsIgnoreCase)) {
                return preferred;
            }
        }

        return Font.SANS_SERIF;
    }

    /**
     * Formats a presentation delay for display.
     *
     * @param delay duration in milliseconds
     * @param seconds whether to display seconds
     * @return formatted duration
     */
    private String formatDelay(int delay, boolean seconds) {
        return seconds
                ? String.format(Locale.ROOT, "%.1f seconds", delay / 1000.0)
                : String.format(Locale.US, "%,d ms", delay);
    }

    /**
     * Prepares drawing settings for smooth component edges.
     *
     * @param graphics original graphics context
     * @return independent smooth-edged graphics context
     */
    private static Graphics2D prepareGraphics(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();

        g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        return g;
    }

    /**
     * Restores a runtime setting or removes an existing override.
     *
     * @param property runtime setting name
     * @param value original property value
     */
    private void restoreProperty(String property, String value) {
        if (value == null) {
            System.clearProperty(property);
        } else {
            System.setProperty(property, value);
        }
    }

    /**
     * A reusable panel that gives the presentation cards their rounded background and border.
     */
    private static final class RoundedPanel extends JPanel {

        private final Color surface;
        private final Color border;
        private final int radius;

        /**
         * Sets up a rounded panel.
         *
         * @param surface panel background color
         * @param border panel outline color
         * @param radius corner radius
         */
        private RoundedPanel(
                Color surface, Color border, int radius) {

            this.surface = surface;
            this.border = border;
            this.radius = radius;

            setOpaque(false);
        }

        /**
         * Paints the rounded background and outline.
         *
         * @param graphics component graphics context
         */
        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);

            Graphics2D g = prepareGraphics(graphics);

            g.setColor(surface);
            g.fillRoundRect(
                    0, 0, getWidth() - 1, getHeight() - 1,
                    radius, radius);

            g.setColor(border);
            g.drawRoundRect(
                    0, 0, getWidth() - 1, getHeight() - 1,
                    radius, radius);

            g.dispose();
        }
    }

    /**
     * A reusable button for presets and reset controls. Changes appearance when selected, hovered over, pressed or focused.
     */
    private static final class StyledButton extends JButton {

        /**
         * Sets up a styled button.
         *
         * @param text button display text
         */
        private StyledButton(String text) {
            super(text);

            setFont(createFont(12, Font.BOLD));
            setForeground(TEXT);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            setBorder(new EmptyBorder(10, 12, 10, 12));
        }

        /**
         * Paints the button using its current interaction state.
         *
         * @param graphics component graphics context
         */
        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = prepareGraphics(graphics);

            boolean selected = isSelected();
            boolean hovered = getModel().isRollover();
            boolean pressed = getModel().isPressed();

            Color background = selected || pressed
                    ? ACCENT_DARK
                    : hovered ? BACKGROUND : SURFACE;

            g.setColor(background);
            g.fillRoundRect(
                    0, 0, getWidth() - 1, getHeight() - 1, 8, 8);

            g.setColor(selected || hovered || pressed
                    ? ACCENT : BORDER);

            g.drawRoundRect(
                    0, 0, getWidth() - 1, getHeight() - 1, 8, 8);

            if (hasFocus()) {
                g.setColor(ACCENT);
                g.drawRoundRect(
                        2, 2, getWidth() - 5, getHeight() - 5, 6, 6);
            }

            g.setFont(getFont());
            g.setColor(selected || pressed ? SURFACE : TEXT);

            FontMetrics metrics = g.getFontMetrics();

            int x = (getWidth() - metrics.stringWidth(getText())) / 2;
            int y = (getHeight() - metrics.getHeight()) / 2
                    + metrics.getAscent();

            g.drawString(getText(), x, y);
            g.dispose();
        }
    }

    /**
     * Gives the delay sliders their custom appearance while keeping normal slider behavior.
     */
    private static final class PresentationSliderUI
            extends BasicSliderUI {

        /**
         * Sets up the custom slider display component.
         *
         * @param slider slider being styled
         */
        private PresentationSliderUI(JSlider slider) {
            super(slider);
        }

        /**
         * Defines the dimensions of the slider thumb.
         *
         * @return slider thumb dimensions
         */
        @Override
        protected Dimension getThumbSize() {
            return new Dimension(18, 18);
        }

        /**
         * Paints the inactive track and active progress indicator.
         *
         * @param graphics component graphics context
         */
        @Override
        public void paintTrack(Graphics graphics) {
            Graphics2D g = prepareGraphics(graphics);

            int height = 8;
            int y = trackRect.y + (trackRect.height - height) / 2;

            g.setColor(TRACK);
            g.fillRoundRect(
                    trackRect.x, y, trackRect.width, height,
                    height, height);

            int progress = Math.max(0,
                    thumbRect.x + thumbRect.width / 2 - trackRect.x);

            g.setColor(ACCENT);
            g.fillRoundRect(
                    trackRect.x, y,
                    Math.min(progress, trackRect.width),
                    height, height, height);

            g.dispose();
        }

        /**
         * Paints the circular slider thumb and its border.
         *
         * @param graphics component graphics context
         */
        @Override
        public void paintThumb(Graphics graphics) {
            Graphics2D g = prepareGraphics(graphics);

            g.setColor(SURFACE);
            g.fillOval(
                    thumbRect.x, thumbRect.y,
                    thumbRect.width - 1, thumbRect.height - 1);

            g.setColor(ACCENT);
            g.setStroke(new BasicStroke(2f));

            g.drawOval(
                    thumbRect.x + 1, thumbRect.y + 1,
                    thumbRect.width - 3, thumbRect.height - 3);

            g.dispose();
        }

        /**
         * Paints a keyboard focus indicator around the slider.
         *
         * @param graphics component graphics context
         */
        @Override
        public void paintFocus(Graphics graphics) {
            Graphics2D g = prepareGraphics(graphics);

            g.setColor(ACCENT);
            g.drawRoundRect(
                    focusRect.x, focusRect.y,
                    Math.max(0, focusRect.width - 1),
                    Math.max(0, focusRect.height - 1), 8, 8);

            g.dispose();
        }
    }
}