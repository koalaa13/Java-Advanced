package ru.ifmo.rain.maksimov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.AdvancedIP;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation for {@link AdvancedIP} interface
 *
 * @author koalaa13 (github.com/koalaa13)
 */
public class IterativeParallelism implements AdvancedIP {
    /**
     * Default constructor
     */
    public IterativeParallelism() {
    }

    private static <T, R> Thread getThread(int i,
                                           List<R> res,
                                           Stream<T> data,
                                           Function<? super Stream<T>, R> task) {
        return new Thread(() -> res.set(i, task.apply(data)));
    }

    private static <T, R> R doJob(int threads,
                                  List<T> values,
                                  Function<? super Stream<T>, R> task,
                                  Function<? super Stream<R>, R> collector) throws InterruptedException {
        if (threads <= 0) {
            throw new IllegalArgumentException("thread number should be >= 0");
        }
        threads = Math.min(values.size(), threads);
        final List<Thread> jobs = new ArrayList<>(Collections.nCopies(threads, null));
        final List<R> result = new ArrayList<>(Collections.nCopies(threads, null));
        final int rest = values.size() % threads;
        int blockSize = values.size() / threads + 1;
        for (int i = 0, pos = 0; i < threads; ++i, pos += blockSize) {
            if (rest == i) {
                blockSize--;
            }
            jobs.set(i, getThread(i, result, values.subList(pos, pos + blockSize).stream(), task));
            jobs.get(i).start();
        }
        InterruptedException exception = null;
        for (Thread thread : jobs) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                if (exception == null) {
                    exception = new InterruptedException("Not all thread joined");
                }
                exception.addSuppressed(e);
                for (Thread toStop : jobs) {
                    if (toStop.isAlive()) {
                        toStop.interrupt();
                    }
                }
            }
        }
        if (exception != null) {
            throw exception;
        }
        return collector.apply(result.stream());
    }

    /**
     * Reduces values using {@link info.kgeorgiy.java.advanced.concurrent.AdvancedIP.Monoid}
     *
     * @param threads number of concurrent threads.
     * @param values  values to reduce.
     * @param monoid  monoid to use.
     * @param <T>     value type.
     * @return values reduced by provided monoid or {@link Monoid#getIdentity() identity} if no values specified.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T> T reduce(int threads, List<T> values, Monoid<T> monoid) throws InterruptedException {
        Function<Stream<T>, T> reducer =
                stream -> stream.reduce(monoid.getIdentity(), monoid.getOperator());
        return doJob(threads, values, reducer, reducer);
    }

    /**
     * Maps and reduces values using monoid.
     *
     * @param threads number of concurrent threads.
     * @param values  values to reduce.
     * @param lift    mapping function.
     * @param monoid  monoid to use.
     * @param <T>     value type of given values.
     * @param <R>     value type of result.
     * @return values mapped and reduced by provided monoid or {@link Monoid#getIdentity() identity} if no values specified.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T, R> R mapReduce(int threads, List<T> values, Function<T, R> lift, Monoid<R> monoid) throws InterruptedException {
        return doJob(threads, values,
                stream -> stream.map(lift).reduce(monoid.getIdentity(), monoid.getOperator()),
                stream -> stream.reduce(monoid.getIdentity(), monoid.getOperator()));
    }

    /**
     * Joins given value to {@link String string}.
     *
     * @param threads number of concurrent threads.
     * @param values  values to join.
     * @return {@link String} of joined {@link #toString() values}.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public String join(int threads, List<?> values) throws InterruptedException {
        return doJob(threads, values,
                stream -> stream.map(Object::toString).collect(Collectors.joining()),
                stream -> stream.collect(Collectors.joining()));
    }

    /**
     * Filters given values by {@link Predicate predicate}
     *
     * @param threads   number of concurrent threads.
     * @param values    values to filter.
     * @param predicate filter predicate.
     * @param <T>       value type.
     * @return {@link List} of filtered values.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T> List<T> filter(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return doJob(threads, values,
                stream -> stream.filter(predicate).collect(Collectors.toList()),
                stream -> stream.flatMap(Collection::stream).collect(Collectors.toList()));
    }

    /**
     * Maps given values by {@link Function mapper}
     *
     * @param threads number of concurrent threads.
     * @param values  values to filter.
     * @param f       mapper function.
     * @param <T>     value type of given values.
     * @param <U>     value type of result values.
     * @return {@link List} of mapped values.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T, U> List<U> map(int threads, List<? extends T> values, Function<? super T, ? extends U> f) throws InterruptedException {
        return doJob(threads, values,
                stream -> stream.map(f).collect(Collectors.toList()),
                stream -> stream.flatMap(Collection::stream).collect(Collectors.toList()));
    }

    /**
     * Finds maximum in {@link List values} by given {@link Comparator comparator}.
     *
     * @param threads    number or concurrent threads.
     * @param values     values to get maximum of.
     * @param comparator value comparator.
     * @param <T>        value type.
     * @return Maximum in given {@link List}.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return minimum(threads, values, Collections.reverseOrder(comparator));
    }

    /**
     * Finds minimum in {@link List values} by given {@link Comparator comparator}.
     *
     * @param threads    number or concurrent threads.
     * @param values     values to get minimum of.
     * @param comparator value comparator.
     * @param <T>        value type.
     * @return Minimum in given {@link List}.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("Values should not be null or empty");
        }
        Function<Stream<? extends T>, T> streamMax = stream -> stream.min(comparator).get();
        return doJob(threads, values, streamMax, streamMax);
    }

    /**
     * Returns {@link Boolean#TRUE} if all of {@link List values} satisfies {@link Predicate predicate}
     * or {@link Boolean#FALSE} else.
     *
     * @param threads   number or concurrent threads.
     * @param values    values to test.
     * @param predicate test predicate.
     * @param <T>       value type.
     * @return {@link Boolean#TRUE} if all of {@link List values} satisfies {@link Predicate predicate}
     * * or {@link Boolean#FALSE} else.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return !any(threads, values, predicate.negate());
    }

    /**
     * Returns {@link Boolean#TRUE} if any of {@link List values} satisfies {@link Predicate predicate}
     * or {@link Boolean#FALSE} else.
     *
     * @param threads   number or concurrent threads.
     * @param values    values to test.
     * @param predicate test predicate.
     * @param <T>       value type.
     * @return Returns {@link Boolean#TRUE} if any of {@link List values} satisfies {@link Predicate predicate}
     * or {@link Boolean#FALSE} else.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return doJob(threads, values,
                stream -> stream.anyMatch(predicate),
                stream -> stream.anyMatch(Boolean::booleanValue));
    }
}
