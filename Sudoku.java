import java.util.*;

/** Sudoku rules and puzzle generation. Values are 1-9; 0 means empty. */
public class Sudoku extends BoardGame {
    private static final int BOX = 3;

    private int[][] solution; // private: only reachable through the getters below
    private int[][] puzzle;

    public Sudoku() {
        super(9);
        solution = new int[size][size];
        puzzle = new int[size][size];
    }

    /** Overload 1: the Level object decides how many cells to remove. */
    @Override
    public void newGame(Level level) {
        newGame(level.getHoles());
    }

    /** Overload 2: generates a full board, then blanks out 'holes' random cells. */
    public void newGame(int holes) {
        solution = new int[size][size];
        fill(0);

        puzzle = new int[size][size];
        for (int r = 0; r < size; r++) {
            puzzle[r] = solution[r].clone();
        }

        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i < size * size; i++) {
            positions.add(i);
        }
        Collections.shuffle(positions);
        for (int i = 0; i < holes; i++) {
            int pos = positions.get(i);
            puzzle[pos / size][pos % size] = 0;
        }
    }

    /** Fills the board cell by cell using randomized backtracking. */
    private boolean fill(int pos) {
        if (pos == size * size) {
            return true;
        }
        int r = pos / size;
        int c = pos % size;

        List<Integer> digits = new ArrayList<>();
        for (int n = 1; n <= size; n++) {
            digits.add(n);
        }
        Collections.shuffle(digits);

        for (int n : digits) {
            if (isSafe(solution, r, c, n)) {
                solution[r][c] = n;
                if (fill(pos + 1)) {
                    return true;
                }
                solution[r][c] = 0; // dead end: backtrack
            }
        }
        return false;
    }

    /** True if 'num' is not already in the row, column or block of (row, col). */
    private boolean isSafe(int[][] b, int row, int col, int num) {
        for (int i = 0; i < size; i++) {
            if (b[row][i] == num || b[i][col] == num) {
                return false;
            }
        }
        int br = row / BOX * BOX;
        int bc = col / BOX * BOX;
        for (int r = br; r < br + BOX; r++) {
            for (int c = bc; c < bc + BOX; c++) {
                if (b[r][c] == num) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Sudoku's own rule: marks every filled cell repeated in its row, column or
     * block.
     */
    @Override
    public boolean[][] findConflicts(int[][] b) {
        boolean[][] bad = new boolean[size][size];
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                int v = b[r][c];
                if (v != 0) {
                    b[r][c] = 0; // temporarily remove the cell itself
                    bad[r][c] = !isSafe(b, r, c, v);
                    b[r][c] = v;
                }
            }
        }
        return bad;
    }

    public int getPuzzle(int r, int c) {
        return puzzle[r][c];
    }

    public int getSolution(int r, int c) {
        return solution[r][c];
    }

    /** True if the cell was pre-filled by the puzzle. */
    public boolean isGiven(int r, int c) {
        return puzzle[r][c] != 0;
    }
}
