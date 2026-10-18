/** Interface: the minimum any playable game must offer. */
public interface Playable {
    void newGame(Level level);

    boolean isSolved(int[][] board);
}
