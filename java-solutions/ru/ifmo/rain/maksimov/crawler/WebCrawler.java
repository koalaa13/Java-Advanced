package ru.ifmo.rain.maksimov.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.*;

import static ru.ifmo.rain.maksimov.utils.ConcurrentUtils.closeExecutorService;
import static ru.ifmo.rain.maksimov.utils.Helper.log;

/**
 * Implementation of {@link Crawler}
 */
public class WebCrawler implements Crawler {
    private final Downloader downloader;
    private final int perHost;

    private final ExecutorService downloadersPool;
    private final ExecutorService extractorsPool;
    private final ConcurrentMap<String, HostData> hostsInfo;

    private class HostData {
        int cnt;
        final Queue<Runnable> waiting;

        HostData() {
            this.waiting = new ArrayDeque<>();
            cnt = 0;
        }

        synchronized void addTask(Runnable task) {
            if (cnt < perHost) {
                cnt++;
                downloadersPool.submit(task);
            } else {
                waiting.add(task);
            }
        }

        synchronized void nextTask() {
            if (waiting.isEmpty()) {
                cnt--;
            } else {
                downloadersPool.submit(waiting.poll());
            }
        }
    }

    private void addToDownload(final String url, final int depth, final Set<String> good,
                               final ConcurrentMap<String, IOException> bad,
                               final Phaser sync,
                               final Set<String> used) {
        try {
            final String host = URLUtils.getHost(url);
            final HostData data = hostsInfo.computeIfAbsent(host, hostData -> new HostData());

            sync.register();
            data.addTask(() -> {
                try {
                    final Document page = downloader.download(url);
                    good.add(url);
                    if (depth > 1) {
                        sync.register();
                        extractorsPool.submit(() -> extractLinks(page, depth - 1, good, bad, sync, used));
                    }
                } catch (IOException e) {
                    bad.put(url, e);
                } finally {
                    sync.arrive();
                    data.nextTask();
                }
            });
        } catch (MalformedURLException e) {
            bad.put(url, e);
        }
    }

    private void extractLinks(Document page, int depth, Set<String> good,
                              ConcurrentMap<String, IOException> bad,
                              Phaser sync,
                              Set<String> used) {
        try {
            page.extractLinks().stream()
                    .filter(used::add)
                    .forEach(link -> addToDownload(link, depth, good, bad, sync, used));
        } catch (IOException ignored) {
        } finally {
            sync.arrive();
        }
    }

    /**
     * @param downloader  downloader to use to download pages
     * @param downloaders count of downloaders that we can use
     * @param extractors  count of extractors that we can use
     * @param perHost     limit of how many pages we can download from one host
     */
    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        this.perHost = perHost;
        downloadersPool = Executors.newFixedThreadPool(downloaders);
        extractorsPool = Executors.newFixedThreadPool(extractors);
        hostsInfo = new ConcurrentHashMap<>();
    }

    /**
     * As same as {@link #WebCrawler(Downloader, int, int, int)} but uses {@link CachingDownloader}
     *
     * @param downloaders count of downloaders that we can use
     * @param extractors  count of extractors that we can use
     * @param perHost     limit of how many pages we can download from one host
     * @throws IOException error while creating {@link CachingDownloader} occurred
     */
    public WebCrawler(int downloaders, int extractors, int perHost) throws IOException {
        this(new CachingDownloader(), downloaders, extractors, perHost);
    }

    @Override
    public Result download(String url, int depth) {
        final Set<String> good = ConcurrentHashMap.newKeySet();
        final ConcurrentMap<String, IOException> bad = new ConcurrentHashMap<>();
        final Set<String> used = ConcurrentHashMap.newKeySet();
        final Phaser sync = new Phaser(1);
        used.add(url);
        addToDownload(url, depth, good, bad, sync, used);
        sync.arriveAndAwaitAdvance();
        return new Result(new ArrayList<>(good), bad);
    }

    @Override
    public void close() {
        closeExecutorService(downloadersPool);
        closeExecutorService(extractorsPool);
    }

    /**
     * Main method for {@link WebCrawler}
     *
     * @param args args for a crawler
     */
    public static void main(String[] args) {
        if (args == null || args.length != 5) {
            log("Usage: WebCrawler url [depth [downloads [extractors [perHost]]]]");
            return;
        }
        if (Arrays.stream(args).anyMatch(Objects::isNull)) {
            log("Arguments can not be null");
            return;
        }
        final int[] argsNumbers = new int[4];
        for (int i = 1; i < 5; ++i) {
            try {
                argsNumbers[i - 1] = Integer.parseInt(args[i]);
            } catch (final NumberFormatException e) {
                log(i + "th argument should be a number", e);
                return;
            }
        }
        if (Arrays.stream(argsNumbers).anyMatch(i -> i <= 0)) {
            log("All numbers should be > 0");
            return;
        }
        try (Crawler webCrawler = new WebCrawler(argsNumbers[1], argsNumbers[2], argsNumbers[3])) {
            webCrawler.download(args[0], argsNumbers[1]);
        } catch (IOException e) {
            log("Can not create CachingDownloader", e);
        }
    }
}
