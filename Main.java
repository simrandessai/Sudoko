import javax.swing.*;
import javax.swing.border.MatteBorder;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * Swing GUI for Sudoku. Run this class to play.
 * The board uses the symbols "!@#$%^&*?" instead of digits 1-9.
 * Wrong or repeated symbols are highlighted red.
 */
public class Main extends JFrame {
    private static final String SYMBOLS = "!@#$%^&*?"; // symbol i+1 stands for value i+1
    private static final int MAX_MISTAKES = 3;         // the game ends on the next mistake
    private static final int MAX_HINTS = 3;

    private static final Color GIVEN_BG = new Color(225, 225, 225);
    private static final Color WRONG_BG = new Color(255, 170, 170);
    private static final Color USER_FG = new Color(0, 70, 190);

    private final Sudoku game = new Sudoku();
    private final JTextField[][] cells = new JTextField[9][9];
    private final boolean[][] hinted = new boolean[9][9];

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel screens = new JPanel(cardLayout);
    private final JComboBox<Level> level = new JComboBox<>(
            new Level[] { new EasyLevel(), new MediumLevel(), new HardLevel() });
    private final JLabel timerLabel = new JLabel();
    private final JLabel mistakesLabel = new JLabel();
    private final JLabel hintsLabel = new JLabel();
    private final JButton hintButton = new JButton("Hint");
    private int elapsedSeconds;
    private final Timer timer = new Timer(1000, e -> {
        elapsedSeconds++;
        timerLabel.setText("Time: " + formatTime());
    });

    private boolean loading;      // true while we set cells ourselves (ignore those changes)
    private boolean gameActive;
    private int selectedRow = -1; // cell that last had focus (target for hints)
    private int selectedColumn = -1;
    private int mistakes;
    private int hintsUsed;

    public Main() {
        setTitle("Sudoku");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        screens.add(createMenuScreen(), "menu");
        screens.add(createGameScreen(), "game");
        add(screens);
        setResizable(false);
        pack();
        setLocationRelativeTo(null);
    }

    // ---------- Screens ----------

    private JPanel createMenuScreen() {
        JPanel menu = new JPanel(new GridBagLayout());
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Sudoku");
        title.setFont(new Font("SansSerif", Font.BOLD, 30));
        JButton start = new JButton("Start Game");
        start.addActionListener(e -> startNewGame());

        for (JComponent c : new JComponent[] { title, new JLabel("Choose a level to begin"), level, start }) {
            c.setAlignmentX(Component.CENTER_ALIGNMENT);
            box.add(c);
            box.add(Box.createVerticalStrut(12));
        }
        menu.add(box);
        return menu;
    }

    private JPanel createGameScreen() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));

        JPanel status = new JPanel(new FlowLayout(FlowLayout.CENTER, 18, 8));
        status.add(timerLabel);
        status.add(mistakesLabel);
        status.add(hintsLabel);
        panel.add(status, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(9, 9));
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                cells[r][c] = createCell(r, c);
                grid.add(cells[r][c]);
            }
        }
        panel.add(grid, BorderLayout.CENTER);

        JButton menuButton = new JButton("Main Menu");
        hintButton.addActionListener(e -> useHint());
        menuButton.addActionListener(e -> returnToMenu());
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));
        controls.add(hintButton);
        controls.add(menuButton);
        panel.add(controls, BorderLayout.SOUTH);
        return panel;
    }

    /** Builds one grid cell: thick borders around 3x3 blocks, symbol filter, key handling. */
    private JTextField createCell(int r, int c) {
        JTextField tf = new JTextField();
        tf.setHorizontalAlignment(JTextField.CENTER);
        tf.setFont(new Font("SansSerif", Font.BOLD, 24));
        tf.setPreferredSize(new Dimension(50, 50));
        tf.setBorder(new MatteBorder(r % 3 == 0 ? 3 : 1, c % 3 == 0 ? 3 : 1,
                r == 8 ? 3 : 0, c == 8 ? 3 : 0, Color.BLACK));
        ((AbstractDocument) tf.getDocument()).setDocumentFilter(new SymbolFilter(r, c));

        tf.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                selectedRow = r;
                selectedColumn = c;
            }
        });

        // Arrow keys move between cells
        addArrowKey(tf, r, c, "UP", -1, 0);
        addArrowKey(tf, r, c, "DOWN", 1, 0);
        addArrowKey(tf, r, c, "LEFT", 0, -1);
        addArrowKey(tf, r, c, "RIGHT", 0, 1);
        return tf;
    }

    private void addArrowKey(JTextField cell, int row, int col, String key, int dr, int dc) {
        cell.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(key), key);
        cell.getActionMap().put(key, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int r = row + dr;
                int c = col + dc;
                if (r >= 0 && r < 9 && c >= 0 && c < 9) {
                    cells[r][c].requestFocusInWindow();
                }
            }
        });
    }

    // ---------- Game flow ----------

    private void startNewGame() {
        game.newGame((Level) level.getSelectedItem()); // each Level subclass decides the holes

        mistakes = 0;
        hintsUsed = 0;
        elapsedSeconds = 0;
        gameActive = true;

        loading = true; // don't treat our own setText calls as player input
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                boolean given = game.isGiven(r, c);
                hinted[r][c] = false;
                cells[r][c].setText(symbolFor(game.getPuzzle(r, c)));
                cells[r][c].setEditable(!given);
                cells[r][c].setForeground(given ? Color.BLACK : USER_FG);
            }
        }
        loading = false;

        timerLabel.setText("Time: 00:00");
        updateStatus();
        refreshColors();
        cardLayout.show(screens, "game");
        timer.restart();
    }

    private void returnToMenu() {
        gameActive = false;
        timer.stop();
        cardLayout.show(screens, "menu");
    }

    private void finishGame(String title, String message) {
        gameActive = false;
        timer.stop();
        for (JTextField[] row : cells) {
            for (JTextField cell : row) {
                cell.setEditable(false);
            }
        }
        updateStatus(); // disables the hint button
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);
        returnToMenu(); // go back to the main menu once the dialog is closed
    }

    private void checkForWin() {
        if (gameActive && game.isSolved(readBoard())) {
            finishGame("You Win!", "Congratulations! You solved it in " + formatTime() + ".");
        }
    }

    // ---------- Handling player input ----------

    /** Called by the cell filter after the player has changed a cell. */
    private void cellChanged(int row, int col) {
        if (loading || !gameActive) {
            return;
        }
        String value = cells[row][col].getText();
        boolean wrong = !value.isEmpty() && valueFor(value.charAt(0)) != game.getSolution(row, col);
        refreshColors();
        if (wrong) {
            mistakes++;
            updateStatus();
            if (mistakes > MAX_MISTAKES) {
                finishGame("Game Over", "You made " + mistakes + " mistakes. Better luck next time!");
            }
        } else {
            checkForWin();
        }
    }

    private void useHint() {
        if (!gameActive || hintsUsed >= MAX_HINTS) {
            return;
        }
        if (selectedRow < 0 || !cells[selectedRow][selectedColumn].isEditable()
                || !cells[selectedRow][selectedColumn].getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Select an empty cell to receive a hint.");
            return;
        }

        // Fill in the correct symbol and lock the cell
        JTextField cell = cells[selectedRow][selectedColumn];
        loading = true;
        cell.setText(symbolFor(game.getSolution(selectedRow, selectedColumn)));
        loading = false;
        cell.setEditable(false);
        cell.setForeground(Color.BLACK);
        hinted[selectedRow][selectedColumn] = true;

        hintsUsed++;
        updateStatus();
        refreshColors();
        checkForWin();
    }

    // ---------- Display helpers ----------

    /** Reads the symbols on screen as values 1-9 (0 = empty). */
    private int[][] readBoard() {
        int[][] board = new int[9][9];
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                String text = cells[r][c].getText();
                board[r][c] = text.isEmpty() ? 0 : valueFor(text.charAt(0));
            }
        }
        return board;
    }

    private static String symbolFor(int value) {
        return value == 0 ? "" : String.valueOf(SYMBOLS.charAt(value - 1));
    }

    private static int valueFor(char symbol) {
        return SYMBOLS.indexOf(symbol) + 1;
    }

    /** Throws the custom exception unless the text is exactly one game symbol. */
    private static void checkSymbol(String text) throws InvalidSymbolException {
        if (text.length() != 1 || !SYMBOLS.contains(text)) {
            throw new InvalidSymbolException(text);
        }
    }

    /** Red for conflicting or incorrect cells, grey for given/hinted cells, white otherwise. */
    private void refreshColors() {
        boolean[][] conflicts = game.findConflicts(readBoard());
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                String text = cells[r][c].getText();
                boolean incorrect = !text.isEmpty() && valueFor(text.charAt(0)) != game.getSolution(r, c);
                Color bg = Color.WHITE;
                if (conflicts[r][c] || incorrect) {
                    bg = WRONG_BG;
                } else if (game.isGiven(r, c) || hinted[r][c]) {
                    bg = GIVEN_BG;
                }
                cells[r][c].setBackground(bg);
            }
        }
    }

    private void updateStatus() {
        mistakesLabel.setText("Mistakes: " + mistakes + " / " + MAX_MISTAKES);
        hintsLabel.setText("Hints: " + hintsUsed + " / " + MAX_HINTS);
        hintButton.setEnabled(gameActive && hintsUsed < MAX_HINTS);
    }

    private String formatTime() {
        return String.format("%02d:%02d", elapsedSeconds / 60, elapsedSeconds % 60);
    }

    /**
     * Allows only one of the nine game symbols per cell (a new symbol replaces the
     * old one) and tells the game once the text has actually changed.
     */
    private class SymbolFilter extends DocumentFilter {
        private final int row;
        private final int col;

        SymbolFilter(int row, int col) {
            this.row = row;
            this.col = col;
        }

        @Override
        public void insertString(FilterBypass fb, int offset, String text, AttributeSet attrs)
                throws BadLocationException {
            replace(fb, offset, 0, text, attrs);
        }

        @Override
        public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
            super.remove(fb, offset, length);
            cellChanged(row, col);
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException {
            Document doc = fb.getDocument();
            if (text == null || text.isEmpty()) {
                super.replace(fb, offset, length, text, attrs);
            } else {
                try {
                    checkSymbol(text);
                } catch (InvalidSymbolException e) {
                    Toolkit.getDefaultToolkit().beep(); // reject anything that is not a game symbol
                    return;
                }
                if (text.equals(doc.getText(0, doc.getLength()))) {
                    return; // same symbol typed again: nothing changed
                }
                super.replace(fb, 0, doc.getLength(), text, attrs); // overwrite cell
            }
            cellChanged(row, col);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main().setVisible(true));
    }
}
