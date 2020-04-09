package ru.ifmo.rain.maksimov.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;

import static ru.ifmo.rain.maksimov.concurrent.Utils.*;

/**
 * Implementation of {@link ParallelMapper}
 */
public class ParallelMapperImpl implements ParallelMapper {
    private final Queue<Runnable> tasks;
    private final List<Thread> workers;
    private final int MAX_SIZE = 1;

    private class ResultList<R> {
        private final List<R> data;
        private int cnt;

        ResultList(final int size) {
            cnt = 0;
            data = new ArrayList<>(Collections.nCopies(size, null));
        }

        synchronized void set(final int pos, R value) {
            data.set(pos, value);
            if (++cnt == data.size()) {
                notify();
            }
        }

        synchronized List<R> get() throws InterruptedException {
            while (cnt < data.size()) {
                wait();
            }
            return data;
        }
    }

    private void solveTask() throws InterruptedException {
        Runnable task;
        synchronized (tasks) {
            while (tasks.isEmpty()) {
                tasks.wait();
            }
            task = tasks.poll();
            tasks.notifyAll();
        }
        task.run();
    }

    private void addTask(final Runnable task) throws InterruptedException {
        synchronized (tasks) {
            while (tasks.size() == MAX_SIZE) {
                tasks.wait();
            }
            tasks.add(task);
            tasks.notifyAll();
        }
    }

    /**
     * Default constructor
     *
     * @param threads count of threads mapper can use to calculate function
     */
    public ParallelMapperImpl(final int threads) {
        checkThreads(threads);
        tasks = new ArrayDeque<>();
        workers = new ArrayList<>();
        for (int i = 0; i < threads; ++i) {
            addAndStart(workers, new Thread(() -> {
                try {
                    while (!Thread.interrupted()) {
                        solveTask();
                    }
                } catch (InterruptedException ignored) {
                } finally {
                    Thread.currentThread().interrupt();
                }
            }));
        }
    }

    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        ResultList<R> res = new ResultList<>(args.size());
        for (int i = 0; i < args.size(); i++) {
            final int ind = i;
            addTask(() -> res.set(ind, f.apply(args.get(ind))));
        }
        return res.get();
    }

    @Override
    public void close() {
        workers.forEach(Thread::interrupt);
        joinThreadsExceptionSafely(workers);
    }
}
