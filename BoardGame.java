/** Abstract class: shared base for square board games. Subclasses supply the rules. */
public abstract class BoardGame implements Playable {
    protected final int size;

    protected BoardGame(int size) {
        this.size = size;
    }

    public int getSize() {
        return size;
    }

    /** Each game decides which cells break its rules. */
    public abstract boolean[][] findConflicts(int[][] board);

    /** Shared by all subclasses: solved = every cell filled and no conflicts. */
    @Override
    public boolean isSolved(int[][] board) {
        boolean[][] bad = findConflicts(board);
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (board[r][c] == 0 || bad[r][c]) {
                    return false;
                }
            }
        }
        return true;
    }
}
