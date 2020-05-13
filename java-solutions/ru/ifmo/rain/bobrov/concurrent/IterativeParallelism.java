package ru.ifmo.rain.bobrov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ScalarIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;


/**
 * Class provides public methods to find maximum, minimum value or check
 * if all or any of elements of {@link List} match given predicate.
 *
 * Class implements {@link ScalarIP} interface.
 *
 * @author Bobrov Oleg
 * @version 1.0
 * @see ScalarIP
 */
public class IterativeParallelism implements ScalarIP {
    private final ParallelMapper mapper;
    /**
     * Default constructor.
     */
    public IterativeParallelism() {
        mapper = null;
    }

    public IterativeParallelism(ParallelMapper parallelMapper) {
        mapper = parallelMapper;
    }

    private <T, R> R createThreadsJob(int threads, List<? extends T> values,
                                      Function<? super Stream<? extends T>, ? extends R> partialJob,
                                      Function<? super Stream<? extends R>, ? extends R> finalJob) throws InterruptedException {
        if (threads <= 0) {
            throw new IllegalArgumentException("Number of threads must be positive.");
        }
        final int amountOfThreads = Math.max(1,Math.min(threads, values.size()));
        final int blockSize = values.size() / amountOfThreads;
        final int remainder = values.size() % amountOfThreads;
        ArrayList<Stream<? extends T>> streams = new ArrayList<>();
        final List<R> partialResults;
        int index = 0;
        for (int thread = 0; thread < amountOfThreads; thread++) {
            final int curBlockSize = blockSize + ((thread < remainder) ? 1 : 0);
            if (curBlockSize > 0) {
                streams.add(values.subList(index, index + curBlockSize).stream());
            }
            index += curBlockSize;
        }
        if(mapper == null) {
            partialResults = new ArrayList<>(Collections.nCopies(amountOfThreads, null));
            ArrayList<Thread> workers = new ArrayList<>();
            for (int curThread = 0; curThread < amountOfThreads; curThread++) {
                final int threadPos = curThread;
                workers.add(new Thread(() -> partialResults.set(threadPos, partialJob.apply(streams.get(threadPos)))));
                workers.get(threadPos).start();
            }
            InterruptedException exception = new InterruptedException();
            for (Thread thread : workers) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    exception.addSuppressed(e);
                }
            }
            if (exception.getSuppressed().length != 0) {
                throw exception;
            }
        } else {
            partialResults = mapper.map(partialJob, streams);
        }
        return finalJob.apply(partialResults.stream());
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        if (values.isEmpty()) {
            throw new NoSuchElementException();
        }
        return createThreadsJob(threads, values, stream -> stream.max(comparator).orElseThrow(), stream -> stream.max(comparator).orElseThrow());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        if (values.isEmpty()) {
            throw new NoSuchElementException();
        }
        return maximum(threads, values, Collections.reverseOrder(comparator));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return createThreadsJob(threads, values, stream -> stream.allMatch(predicate), stream -> stream.allMatch(Boolean::booleanValue));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return createThreadsJob(threads, values, stream -> stream.anyMatch(predicate), stream -> stream.anyMatch(Boolean::booleanValue));
    }
}
