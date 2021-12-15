import java.math.BigInteger;
import java.util.function.BinaryOperator;

public class BigIntegerMultiplyAllocationTest {
    protected static final BinaryOperator<BigInteger> MULTIPLY = BigInteger::multiply;

    public static void main(String... args) {
        fibonacci(1000, MULTIPLY);
        fibonacci(1000, MULTIPLY);
        Benchmark bm = new Benchmark();
        for (int n = 1_000_000; n <= 1_000_000_000 ; n *= 10) {
            System.out.printf("fibonacci(%dm)%n", n / 1_000_000);
            bm.start();
            bm.stop();
            bm.start();
            fibonacci(n, MULTIPLY);
            bm.stop();
            System.out.println(bm);
        }
    }

    public static BigInteger fibonacci(int n, BinaryOperator<BigInteger> multiplyOperator) {
        if (n == 0) return BigInteger.ZERO;
        if (n == 1) return BigInteger.ONE;

        int half = (n + 1) / 2;
        BigInteger f0 = fibonacci(half - 1, multiplyOperator);
        BigInteger f1 = fibonacci(half, multiplyOperator);
        if (n % 2 == 1) {
            BigInteger b0 = multiplyOperator.apply(f0, f0);
            BigInteger b1 = multiplyOperator.apply(f1, f1);
            return b0.add(b1);
        } else {
            BigInteger b0 = f0.shiftLeft(1).add(f1);
            return multiplyOperator.apply(b0, f1);
        }
    }

}
