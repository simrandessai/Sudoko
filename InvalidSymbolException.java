/** Custom checked exception thrown when a cell is given something that is not a game symbol. */
public class InvalidSymbolException extends Exception {
    public InvalidSymbolException(String text) {
        super("Invalid symbol: " + text);
    }
}
