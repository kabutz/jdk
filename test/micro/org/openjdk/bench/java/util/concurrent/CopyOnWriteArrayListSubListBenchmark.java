/*
 * Copyright (c) 2026 - Dr Heinz M. Kabutz. All Rights Reserved.
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
package org.openjdk.bench.java.util.concurrent;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * In this benchmark, we investigate different ways of
 * creating a subList from a CopyOnWriteArrayList, where the
 * sublist should not be affected by changes to the original
 * list. This is based on findings in The Java Specialists'
 * Newsletter issue 336 -
 * https://www.javaspecialists.eu/archive/Issue336.html
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(value = 3)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
public class CopyOnWriteArrayListSubListBenchmark {
    @Param({"2", "10", "100", "1000", "1000000"})
    private int size;
    private CopyOnWriteArrayList<Integer> cow;

    @Setup(Level.Trial)
    public void setup() {
        cow = new CopyOnWriteArrayList<>(
                IntStream.range(0, size).boxed().toList());
    }

    /**
     * Create a subList(), then wrap that in an ArrayList.
     * Can cause {@link ConcurrentModificationException}
     * if outer COWAL is modified whilst we are using the
     * sublist. O(n) cost to the size of the sublist.
     */
    @Benchmark
    public int _0_subListThenWrapInArrayList(Blackhole bh) {
        return process(bh, new ArrayList<>(cow.subList(0, 2)));
    }

    /**
     * Wrap in an ArrayList, then create a subList(). Won't
     * cause {@link ConcurrentModificationException} if the
     * outer COWAL is modified. O(n) cost to the size of
     * the original list.
     */
    @Benchmark
    public int _1_wrapInArrayListThenSubList(Blackhole bh) {
        return process(bh, new ArrayList<>(cow).subList(0, 2));
    }

    /**
     * Wrap with List.copyOf(), then create a subList().
     * O(n) cost to the size of the original list in the
     * original implementation of List.copyOf(); O(1) cost
     * in our proposed change.
     */
    @Benchmark
    public int _2_wrapInListCopyOfThenSubList(Blackhole bh) {
        return process(bh, List.copyOf(cow).subList(0, 2));
    }

    /**
     * Wrap in a CopyOnWriteArrayList, then create a
     * subList(). The new COWAL will share the same array
     * as the original COWAL. Won't cause {@link
     * ConcurrentModificationException} if the original
     * COWAL is modified. O(1) cost to the size of the
     * original list. Access cost of the array field is
     * affected by the volatile modifier.
     */
    @Benchmark
    public int _3_wrapInCOWArrayListThenSubList(Blackhole bh) {
        return process(bh, new CopyOnWriteArrayList<>(cow).subList(0, 2));
    }

    /**
     * Clone the original list, then create a subList().
     * The new COWAL will share the same array
     * as the original COWAL. Won't cause {@link
     * ConcurrentModificationException} if the original
     * COWAL is modified. O(1) cost to the size of the
     * original list. However, slow due to cost of dynamic
     * reflection.
     */
    @Benchmark
    @SuppressWarnings("unchecked")
    public int _4_cloneThenSubList(Blackhole bh) {
        return process(bh, ((List<Integer>) cow.clone()).subList(0, 2));
    }

    /**
     * The proposed snapshot() method, which creates an
     * immutable snapshot of the original
     * CopyOnWriteArrayList, sharing the same underlying
     * array. Since the reference to the array is no longer
     * volatile, access is potentially faster. O(1) cost to
     * the size of the original list.
     */
    @Benchmark
    public int _5_snapshotThenSubList(Blackhole bh) {
        return process(bh, cow.snapshot().subList(0, 2));
    }

    /**
     * Ensure that the list is not eliminated as dead code.
     */
    private int process(Blackhole bh, List<Integer> subList) {
        bh.consume(subList);
        return subList.get(0) + subList.get(1);
    }
}