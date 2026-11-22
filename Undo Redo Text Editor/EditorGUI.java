import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Minimalist Swing front-end for the Undo/Redo Text Editor.
 * All undo/redo logic lives in TextEditor (Stack + Linked List).
 */
public class EditorGUI extends JPanel {

    // ---- palette -------------------------------------------------------
    private static final Color BG        = new Color(0xF7F7F9);
    private static final Color CARD      = Color.WHITE;
    private static final Color BORDER    = new Color(0xE5E7EB);
    private static final Color TEXT      = new Color(0x1F2937);
    private static final Color MUTED     = new Color(0x9CA3AF);
    private static final Color ACCENT    = new Color(0x4F46E5);
    private static final Color ACCENT_BG = new Color(0xEEF2FF);

    private static final String FONT = "SansSerif";
    private static final int PAUSE_MS = 500;   // typing pause that counts as one undo step

    private final TextEditor editor = new TextEditor();

    private final JTextArea area = new JTextArea();
    private final RoundButton undoBtn = new RoundButton("Undo", true);
    private final RoundButton redoBtn = new RoundButton("Redo", true);
    private final RoundButton clearBtn = new RoundButton("Clear", false);
    private final JLabel status = new JLabel();
    private final JPanel undoList = new JPanel();
    private final JPanel redoList = new JPanel();

    private final Timer commitTimer;
    private boolean updating = false;          // true while we set the text ourselves

    public EditorGUI() {
        setBackground(BG);
        setLayout(new BorderLayout());

        commitTimer = new Timer(PAUSE_MS, e -> commit());
        commitTimer.setRepeats(false);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        wireActions();
        refresh();
    }

    // ---- layout --------------------------------------------------------
    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(22, 28, 8, 28));

        JLabel title = new JLabel("Undo/Redo Editor");
        title.setFont(new Font(FONT, Font.BOLD, 22));
        title.setForeground(TEXT);
        JLabel sub = new JLabel("Stack + Linked List");
        sub.setFont(new Font(FONT, Font.PLAIN, 12));
        sub.setForeground(MUTED);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(title);
        left.add(Box.createVerticalStrut(2));
        left.add(sub);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(undoBtn);
        buttons.add(redoBtn);
        buttons.add(clearBtn);

        header.add(left, BorderLayout.WEST);
        header.add(buttons, BorderLayout.EAST);
        return header;
    }

    private JComponent buildCenter() {
        // editor card
        area.setFont(new Font(FONT, Font.PLAIN, 16));
        area.setForeground(TEXT);
        area.setCaretColor(ACCENT);
        area.setSelectionColor(ACCENT_BG);
        area.setSelectedTextColor(TEXT);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(new EmptyBorder(18, 20, 18, 20));
        area.setMargin(new Insets(0, 0, 0, 0));

        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(new LineBorder(BORDER, 1, true));
        scroll.getViewport().setBackground(CARD);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        // history sidebar
        JPanel side = new JPanel();
        side.setOpaque(false);
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setPreferredSize(new Dimension(230, 0));
        side.add(historyCard("Undo stack", undoList));
        side.add(Box.createVerticalStrut(12));
        side.add(historyCard("Redo stack", redoList));

        JPanel center = new JPanel(new BorderLayout(16, 0));
        center.setOpaque(false);
        center.setBorder(new EmptyBorder(8, 28, 8, 28));
        center.add(scroll, BorderLayout.CENTER);
        center.add(side, BorderLayout.EAST);
        return center;
    }

    private JComponent historyCard(String title, JPanel list) {
        JLabel label = new JLabel(title.toUpperCase());
        label.setFont(new Font(FONT, Font.BOLD, 11));
        label.setForeground(MUTED);
        label.setBorder(new EmptyBorder(0, 0, 8, 0));

        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));

        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1, true), new EmptyBorder(14, 14, 14, 14)));
        card.add(label, BorderLayout.NORTH);
        card.add(list, BorderLayout.CENTER);
        return card;
    }

    private JComponent buildFooter() {
        status.setFont(new Font(FONT, Font.PLAIN, 12));
        status.setForeground(MUTED);
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(6, 28, 16, 28));
        footer.add(status, BorderLayout.WEST);

        JLabel hint = new JLabel("Ctrl+Z undo   \u00B7   Ctrl+Y redo");
        hint.setFont(new Font(FONT, Font.PLAIN, 12));
        hint.setForeground(MUTED);
        footer.add(hint, BorderLayout.EAST);
        return footer;
    }

    // ---- behaviour -----------------------------------------------------
    private void wireActions() {
        area.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { onEdit(); }
            public void removeUpdate(DocumentEvent e)  { onEdit(); }
            public void changedUpdate(DocumentEvent e) { onEdit(); }
        });

        undoBtn.addActionListener(e -> undo());
        redoBtn.addActionListener(e -> redo());
        clearBtn.addActionListener(e -> clearAll());

        // keyboard shortcuts on the text area
        int mask;
        try {
            mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();   // Ctrl (Cmd on Mac)
        } catch (HeadlessException ex) {
            mask = java.awt.event.InputEvent.CTRL_DOWN_MASK;
        }
        bind("undo", KeyStroke.getKeyStroke(KeyEvent.VK_Z, mask), this::undo);
        bind("redo", KeyStroke.getKeyStroke(KeyEvent.VK_Y, mask), this::redo);
        bind("redo2", KeyStroke.getKeyStroke(KeyEvent.VK_Z, mask | InputEvent_SHIFT), this::redo);
    }

    private static final int InputEvent_SHIFT = java.awt.event.InputEvent.SHIFT_DOWN_MASK;

    private void bind(String name, KeyStroke key, Runnable action) {
        area.getInputMap().put(key, name);
        area.getActionMap().put(name, new AbstractAction() {
            public void actionPerformed(ActionEvent e) { action.run(); }
        });
    }

    /** Called on every keystroke; groups quick typing into one undo step. */
    private void onEdit() {
        if (updating) return;
        commitTimer.restart();
        refreshStatus();
    }

    /** Sends the text typed so far to the editor logic (one undo step). */
    private void commit() {
        commitTimer.stop();
        editor.type(area.getText());
        refresh();
    }

    private void undo() {
        commit();                              // flush anything still being typed
        if (editor.undo()) showEditorText();
    }

    private void redo() {
        commit();
        if (editor.redo()) showEditorText();
    }

    private void clearAll() {
        commitTimer.stop();
        editor.reset();
        showEditorText();
    }

    private void showEditorText() {
        updating = true;
        area.setText(editor.getText());
        area.setCaretPosition(area.getDocument().getLength());
        updating = false;
        refresh();
        area.requestFocusInWindow();
    }

    // ---- view updates --------------------------------------------------
    private void refresh() {
        undoBtn.setEnabled(editor.canUndo());
        redoBtn.setEnabled(editor.canRedo());
        fillHistory(undoList, editor.undoHistory());
        fillHistory(redoList, editor.redoHistory());
        refreshStatus();
    }

    private void refreshStatus() {
        int chars = area.getText().length();
        status.setText(chars + " characters   \u00B7   " + editor.undoCount()
                + " undo   \u00B7   " + editor.redoCount() + " redo");
    }

    private void fillHistory(JPanel list, String[] items) {
        list.removeAll();
        if (items.length == 0) {
            JLabel empty = new JLabel("Empty");
            empty.setFont(new Font(FONT, Font.ITALIC, 12));
            empty.setForeground(MUTED);
            list.add(empty);
        }
        int shown = Math.min(items.length, 5);        // top of stack first
        for (int i = 0; i < shown; i++) {
            list.add(chip(preview(items[i]), i == 0));
            list.add(Box.createVerticalStrut(6));
        }
        if (items.length > shown) {
            JLabel more = new JLabel("+ " + (items.length - shown) + " more");
            more.setFont(new Font(FONT, Font.PLAIN, 11));
            more.setForeground(MUTED);
            list.add(more);
        }
        list.revalidate();
        list.repaint();
    }

    private JComponent chip(String text, boolean top) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(FONT, Font.PLAIN, 12));
        l.setForeground(top ? ACCENT : TEXT);
        l.setOpaque(true);
        l.setBackground(top ? ACCENT_BG : BG);
        l.setBorder(new EmptyBorder(6, 10, 6, 10));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        return l;
    }

    private static String preview(String s) {
        String one = s.replace('\n', ' ').trim();
        if (one.isEmpty()) return "(empty)";
        return one.length() > 22 ? one.substring(0, 22) + "\u2026" : one;
    }

    // ---- rounded button ------------------------------------------------
    private static class RoundButton extends JButton {
        private final boolean primary;

        RoundButton(String label, boolean primary) {
            super(label);
            this.primary = primary;
            setFont(new Font(FONT, Font.BOLD, 13));
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(9, 20, 9, 20));
            setFocusable(false);
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            Color fill, fg;
            if (!isEnabled())                { fill = new Color(0xEEEEF2); fg = MUTED; }
            else if (primary)                { fill = getModel().isRollover() ? ACCENT.darker() : ACCENT; fg = Color.WHITE; }
            else                             { fill = getModel().isRollover() ? new Color(0xE5E7EB) : new Color(0xEEEEF2); fg = TEXT; }
            if (isEnabled() && getModel().isPressed()) fill = fill.darker();

            g2.setColor(fill);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 14, 14));

            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(getText())) / 2;
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.setColor(fg);
            g2.drawString(getText(), x, y);
            g2.dispose();
        }

        @Override public void setEnabled(boolean b) {
            super.setEnabled(b);
            setCursor(Cursor.getPredefinedCursor(b ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        }
    }

    // ---- entry point ---------------------------------------------------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Undo/Redo Text Editor");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(new EditorGUI());
            frame.setSize(980, 620);
            frame.setMinimumSize(new Dimension(760, 460));
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
