package com.codepoetics.protonpack.collectors;

import org.junit.Test;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class CompletableFuturesTest {

    private static final Random random = new Random();

    @Test
    public void collectsValuesFromCompletableFutures() throws ExecutionException, InterruptedException {
        ExecutorService threadPool = Executors.newFixedThreadPool(10);
        CompletableFuture<List<Integer>> integers = IntStream.range(0, 1000)
                .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                    try {
                        Thread.sleep(random.nextInt(100));
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    return i;
                }, threadPool))
                .collect(CompletableFutures.toFutureList());

        assertThat(
                integers.get(),
                equalTo(IntStream.range(0, 1000).mapToObj(Integer::valueOf).collect(toList())));
    }

    @Test
    public void failsFastOnAnyFutureFailure() throws ExecutionException, InterruptedException {
        ExecutorService threadPool = Executors.newFixedThreadPool(10);
        IllegalStateException expectedException = new IllegalStateException("19! Aaargh!");
        CompletableFuture<List<Integer>> integers = IntStream.range(0, 1000)
                .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                    try {
                        Thread.sleep(random.nextInt(100));
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    if (i == 19) {
                        throw expectedException;
                    }
                    return i;
                }, threadPool))
                .collect(CompletableFutures.toFutureList());

        AtomicReference<Throwable> exc = new AtomicReference<>();
        integers.handle((success, failure) -> { exc.set(failure.getCause()); return null; }).get();

        assertThat(exc.get(), equalTo(expectedException));
    }

    @Test
    public void reducesFutureInts() throws ExecutionException, InterruptedException {
        ExecutorService threadPool = Executors.newFixedThreadPool(10);

        final CompletableFuture<Optional<Integer>> collected = IntStream.range(1, 1000)
                .parallel()
                .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                    try {
                        Thread.sleep(random.nextInt(100));
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    return i;
                }, threadPool))
                .collect(CompletableFutures.reducing((l, r) -> l + r));

        assertThat(collected.get().get(), equalTo(499500));
    }
}
