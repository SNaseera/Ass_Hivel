import java.math.BigInteger;

/**
 * Represents a single decoded root (x, y) of the polynomial.
 * Using BigInteger for both coordinates is crucial for large number handling.
 */
public class Root {
    public final BigInteger x;
    public final BigInteger y;

    public Root(BigInteger x, BigInteger y) {
        this.x = x;
        this.y = y;
    }
}