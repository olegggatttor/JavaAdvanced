package ru.ifmo.rain.bobrov.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;

/**
 * Class provides public constructor and method to parallel given task
 *
 * Class implements {@link ParallelMapper} interface
 *
 * @author Bobrov Oleg
 * @version 1.0
 * @see ParallelMapper
 */
public class ParallelMapperImpl implements ParallelMapper {
    private final Queue<Task<?, ?>> taskQueue = new ArrayDeque<>();
    private final List<Thread> workers = new ArrayList<>();

    /**
     * Public contstructor creates {@code threads} amount of {@link Thread} that
     * get tasks from task queue and do them in parallel.
     * If {@code threads} is not positive than throws {@link IllegalArgumentException}
     *
     * @param threads amount of {@link Thread} that will be created
     */
    public ParallelMapperImpl(int threads) {
        if (threads <= 0) {
            throw new IllegalArgumentException("Number of threads must be positive.");
        }
        for (int curThread = 0; curThread < threads; curThread++) {
            workers.add(new Thread(() -> {
                while (!Thread.interrupted()) {
                    Task<?, ?> task;
                    synchronized (taskQueue) {
                        while (taskQueue.isEmpty()) {
                            try {
                                taskQueue.wait();
                            } catch (InterruptedException ex) {
                                Thread.currentThread().interrupt();
                                return;
                            }
                        }
                        task = taskQueue.poll();
                        taskQueue.notify();
                    }
                    task.run();
                }
            }));
            workers.get(curThread).start();
        }
    }

    /**
     * Default constructor
     */
    public ParallelMapperImpl() {

    }

    private class Task<T, R> {
        private Function<? super T, ? extends R> job;
        private T element;
        private R result;
        private boolean isFinished = false;

        Task(Function<? super T, ? extends R> job, T element) {
            this.element = element;
            this.job = job;
        }

        synchronized void run() {
            result = job.apply(element);
            isFinished = true;
            notifyAll();
        }

        synchronized R getResult() throws InterruptedException {
            while (!isFinished) {
                wait();
            }
            return result;
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        List<Task<T, R>> tasks = new ArrayList<>();
        for (int i = 0; i < args.size(); i++) {
            tasks.add(new Task<>(f, args.get(i)));
            taskQueue.add(tasks.get(i));
        }
        synchronized (taskQueue) {
            taskQueue.notifyAll();
        }
        List<R> results = new ArrayList<>();
        for (Task<T, R> task : tasks) {
            results.add(task.getResult());
        }
        return results;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        synchronized (taskQueue) {
            taskQueue.clear();
            workers.forEach(Thread::interrupt);
            workers.clear();
            taskQueue.notify();
        }
    }
}
