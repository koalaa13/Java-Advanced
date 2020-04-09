package ru.ifmo.rain.maksimov.concurrent;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Some util methods for {@link IterativeParallelism} and {@link ParallelMapperImpl}
 */
public class Utils {
    static <T, R> Thread getThread(int i,
                                   List<R> res,
                                   Stream<T> data,
                                   Function<? super Stream<T>, R> task) {
        return new Thread(() -> res.set(i, task.apply(data)));
    }

    static void joinThreads(final List<Thread> workers) throws InterruptedException {
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

    static void joinThreadsExceptionSafely(final List<Thread> workers) {
        for (Thread thread : workers) {
            try {
                thread.join();
            } catch (InterruptedException ignored) {
            }
        }
    }

    static void checkThreads(int threads) {
        if (threads <= 0) {
            throw new IllegalArgumentException("thread number should be >= 0");
        }
    }

    static void addAndStart(List<Thread> workers, final Thread thread) {
        workers.add(thread);
        thread.start();
    }
}
