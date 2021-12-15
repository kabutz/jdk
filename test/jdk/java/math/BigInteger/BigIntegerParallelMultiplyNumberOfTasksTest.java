import java.math.BigInteger;
import java.util.Locale;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.function.BinaryOperator;

public class BigIntegerParallelMultiplyNumberOfTasksTest {
    private static LongAccumulator maximumNumberOfRecursiveTasks =
            new LongAccumulator(Long::max, 0);
    private static LongAccumulator maximumNumberOfForkedTasks =
            new LongAccumulator(Long::max, 0);

    public static void main(String... args) {
        for (int n = 1000; n <= 1_000_000_000; n *= 10) {
            System.out.printf(Locale.US, "n=%,d", n);
            int bitLength;
            long time = System.nanoTime();
            try {
                BigInteger fb = fibonacci(n, BigInteger::parallelMultiply);
                bitLength = fb.bitLength();
            } finally {
                time = System.nanoTime() - time;
            }
            System.out.printf(Locale.US, "\tbits=%,d\ttasks=%,d\tforks=%,d\ttime=%,dms%n%n",
                    bitLength, maximumNumberOfRecursiveTasks.longValue(),
                    maximumNumberOfForkedTasks.longValue(),
                    time / 1_000_000);
            maximumNumberOfRecursiveTasks.reset();
            maximumNumberOfForkedTasks.reset();
        }
    }

    public static BigInteger fibonacci(int n, BinaryOperator<BigInteger> multiplyOperator) {
        if (n == 0) return BigInteger.ZERO;
        if (n == 1) return BigInteger.ONE;

        int half = (n + 1) / 2;
        BigInteger f0 = fibonacci(half - 1, multiplyOperator);
        BigInteger f1 = fibonacci(half, multiplyOperator);
        if (n % 2 == 1) {
            BigInteger b0 = multiply(multiplyOperator, f0, f0);
            BigInteger b1 = multiply(multiplyOperator, f1, f1);
            return b0.add(b1);
        } else {
            BigInteger b0 = f0.shiftLeft(1).add(f1);
            return multiply(multiplyOperator, b0, f1);
        }
    }

    private static BigInteger multiply(BinaryOperator<BigInteger> multiplyOperator, BigInteger a, BigInteger b) {
        BigInteger.resetCounters();
        BigInteger result = multiplyOperator.apply(a, b);
        maximumNumberOfForkedTasks.accumulate(BigInteger.getNumberOfForks());
        maximumNumberOfRecursiveTasks.accumulate(BigInteger.getNumberOfRecursiveOpTasks());
        return result;
    }
}
