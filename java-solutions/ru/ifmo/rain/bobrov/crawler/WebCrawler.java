package ru.ifmo.rain.bobrov.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;


/**
 * Class provides interface for crawling websites.
 *
 * Implements {@link Crawler} interface.
 */
public class WebCrawler implements Crawler {
    private final ExecutorService downloadersService;
    private final ExecutorService extractorsService;
    private final Downloader loader;

    private final static int DEFAULT_VALUE = 1;

    /**
     * WebCrawler constructor
     *
     * @param downloader {@link Downloader} that used for loading pages
     * @param downloaders amount of downloaders used for loading pages
     * @param extractors amount of extractors used for extracting links from loaded pages
     * @param perHost
     */
    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        downloadersService = Executors.newFixedThreadPool(downloaders);
        extractorsService = Executors.newFixedThreadPool(extractors);
        loader = downloader;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Result download(String url, int depth) {
        final Set<String> used = ConcurrentHashMap.newKeySet();
        final Map<String, IOException> exceptions = new ConcurrentHashMap<>();
        final Phaser counter = new Phaser(1);
        used.add(url);
        startDownloading(url, depth, used, exceptions, counter);
        counter.arriveAndAwaitAdvance();
        used.removeAll(exceptions.keySet());
        return new Result(List.copyOf(used), exceptions);
    }

    private void startDownloading(final String url, final int depth, final Set<String> used,
                                  final Map<String, IOException> exceptions, final Phaser counter) {
        counter.register();
        Runnable pageDownloadTask = () -> {
            try {
                final Document page = loader.download(url);
                if (depth > 1) {
                    counter.register();
                    Runnable extractLinksTask = () -> {
                        try {
                            for (String link : page.extractLinks()) {
                                if (used.add(link)) {
                                    startDownloading(link, depth - 1, used, exceptions, counter);
                                }
                            }
                        } catch (IOException exception) {
                            exceptions.put(url, exception);
                        } finally {
                            counter.arrive();
                        }
                    };
                    extractorsService.submit(extractLinksTask);
                }
            } catch (IOException exception) {
                exceptions.put(url, exception);
            } finally {
                counter.arrive();
            }
        };
        downloadersService.submit(pageDownloadTask);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        downloadersService.shutdown();
        extractorsService.shutdown();
        try {
            if (!downloadersService.awaitTermination(1L, TimeUnit.SECONDS)) {
                downloadersService.shutdownNow();
            }
        } catch (InterruptedException e) {
            downloadersService.shutdownNow();
        }
        try {
            if (!extractorsService.awaitTermination(1L, TimeUnit.SECONDS)) {
                extractorsService.shutdownNow();
            }
        } catch (InterruptedException e) {
            extractorsService.shutdownNow();
        }
    }

    private static int getArgs(int pos, String[] args) {
        if (pos < args.length) {
            try {
                return Integer.parseInt(args[pos]);
            } catch (NumberFormatException ex) {
                return DEFAULT_VALUE;
            }
        } else {
            return DEFAULT_VALUE;
        }
    }

    public static void main(String[] args) {
        if (args == null || args.length < 1 || args.length > 5 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Wrong arguments");
            return;
        }
        try (Crawler crawler = new WebCrawler(new CachingDownloader(), getArgs(2, args), getArgs(3, args), getArgs(4, args))) {
            crawler.download(args[0], getArgs(1, args));
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }
}
