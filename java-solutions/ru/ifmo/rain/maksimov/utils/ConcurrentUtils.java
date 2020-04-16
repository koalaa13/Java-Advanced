package ru.ifmo.rain.maksimov.utils;

import ru.ifmo.rain.maksimov.concurrent.IterativeParallelism;
import ru.ifmo.rain.maksimov.concurrent.ParallelMapperImpl;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Some util methods for {@link IterativeParallelism} and {@link ParallelMapperImpl}
 */
public class ConcurrentUtils {
    /**
     * Returns a thread that doing next.
     * Invoke {@link Function#apply} using {@link Stream data} as argument.
     * Then set at index i in {@link List res} result of applying function.
     *
     * @param i index where to set result of applying function
     * @param res {@link List list} where to set
     * @param data argument for function
     * @param task function to apply
     * @param <T> typename for data
     * @param <R> typename for result
     * @return Watch description
     */
    public static <T, R> Thread getThread(int i,
                                   List<R> res,
                                   Stream<T> data,
                                   Function<? super Stream<T>, R> task) {
        return new Thread(() -> res.set(i, task.apply(data)));
    }

    /**
     * Doing the same as {@link #joinThreadsExceptionSafely(List)} but if exception happened
     * while joining threads stopping all threads and throw {@link InterruptedException exception}
     *
     * @param workers {@link List list} of {@link Thread threads} to join
     * @throws InterruptedException when {@link Thread#join()} throws exception
     */
    public static void joinThreads(final List<Thread> workers) throws InterruptedException {
        InterruptedException exception = null;
        for (Thread thread : workers) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                if (exception == null) {
                    exception = new InterruptedException("Not all thread joined");
                }
                exception.addSuppressed(e);
                for (Thread toStop : workers) {
                    if (toStop.isAlive()) {
                        toStop.interrupt();
                    }
                }
            }
        }
        if (exception != null) {
            throw exception;
        }
    }

    /**
     * Invoke {@link Thread#join()} on all threads in given {@link List list}
     *
     * @param workers {@link List list} of {@link Thread threads} to join
     */
    public static void joinThreadsExceptionSafely(final List<Thread> workers) {
        for (Thread thread : workers) {
            try {
                thread.join();
            } catch (InterruptedException ignored) {
            }
        }
    }

    /**
     * Check is given count of threads greater then 0 or else throw a {@link IllegalArgumentException}
     *
     * @param threads count of threads to check
     */
    public static void checkThreads(int threads) {
        if (threads <= 0) {
            throw new IllegalArgumentException("thread number should be >= 0");
        }
    }

    /**
     * Add {@link Thread thread} to {@link List list} of workers and it
     *
     * @param workers {@link List list} where to add thread
     * @param thread {@link Thread thread} to add and start
     */
    public static void addAndStart(List<Thread> workers, final Thread thread) {
        workers.add(thread);
        thread.start();
    }
}
