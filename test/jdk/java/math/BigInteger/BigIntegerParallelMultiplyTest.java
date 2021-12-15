/*
 * Copyright (c) 1998, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * @test
 * @run main BigIntegerParallelMultiplyTest
 * @summary tests parallelMultiply() method in BigInteger
 * @author Heinz Kabutz heinz@javaspecialists.eu
 */

import java.math.BigInteger;
import java.util.function.BinaryOperator;

/**
 * This is a simple test class created to ensure that the results
 * of multiply() are the same as multiplyParallel(). We calculate
 * the Fibonacci numbers using Dijkstra's sum of squares to get
 * very large numbers (hundreds of thousands of bits).
 *
 * @author Heinz Kabutz, heinz@javaspecialists.eu
 */
public class BigIntegerParallelMultiplyTest {
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

    public static void main(String[] args) throws Exception {
        for (int n = 0; n <= 10; n++) {
            BigInteger fib = fibonacci(n, BigInteger::multiply);
            System.out.printf("fibonacci(%d) = %d%n", n, fib);
        }

        compare(1000, 324);
        compare(10_000, 3473);
        compare(100_000, 34883);
        compare(1_000_000, 347084);
        for (int n = 1_000; n <= 100_000_000; n *= 10) {
            System.out.println();
            BigInteger parallelResult;
            long time = System.nanoTime();
            try {
                parallelResult = fibonacci(n, BigInteger::parallelMultiply);
                System.out.printf("parallel fibonacci(%,d) %d", n, parallelResult.bitLength());
            } finally {
                time = System.nanoTime() - time;
                System.out.printf(" took %dms%n", (time / 1_000_000));
            }

            BigInteger sequentialResult;
            time = System.nanoTime();
            try {
                sequentialResult = fibonacci(n, BigInteger::multiply);
                System.out.printf("sequential fibonacci(%,d) %d", n, sequentialResult.bitLength());
            } finally {
                time = System.nanoTime() - time;
                System.out.printf(" took %dms%n", (time / 1_000_000));
            }
            if (!parallelResult.equals(sequentialResult))
                throw new AssertionError("Mismatch of results");
        }
    }

    private static void compare(int n, int expectedBitCount) {
        BigInteger multiplyResult = fibonacci(n, BigInteger::multiply);
        BigInteger parallelMultiplyResult = fibonacci(n, BigInteger::parallelMultiply);
        checkBitCount(n, expectedBitCount, multiplyResult);
        checkBitCount(n, expectedBitCount, parallelMultiplyResult);
        if (!multiplyResult.equals(parallelMultiplyResult))
            throw new AssertionError("multiply() and parallelMultiply() give different results");
    }

    private static void checkBitCount(int n, int expectedBitCount, BigInteger number) {
        if (number.bitCount() != expectedBitCount)
            throw new AssertionError(
                    "bitCount of fibonacci(" + n + ") was expected to be " + expectedBitCount
                            + " but was " + number.bitCount());
    }
}
