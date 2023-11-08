package by.katz;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

public class Main {

    public static final String PROXY_LIST_PATTERN = "(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}):(\\d+)(?:,.*)?";
    static final int TIMEOUT_MILLIS = 10000;
    private static final int THREADS_TO_CHECK = 50;
    private static final File PROXY_LIST_FILE = new File("proxy_list.txt");
    private static final File RESULTS_FILE = new File("results.txt");
    private static final Proxy.Type[] PROXY_TYPES = new Proxy.Type[]{
          Proxy.Type.DIRECT,
          Proxy.Type.HTTP,
          Proxy.Type.SOCKS
    };

    public static void main(String[] args) throws IOException {
        if (!PROXY_LIST_FILE.exists()) {
            System.out.println("Create file: " + PROXY_LIST_FILE.getAbsolutePath() + " with proxies");
            return;
        }
        // 123.12.1.0:4321
        // 123.12.1.0:4321,BY
        var pattern = Pattern.compile(PROXY_LIST_PATTERN);
        var proxyList = Files.readAllLines(PROXY_LIST_FILE.toPath()).stream()
              .map(s -> Utils.parseListItem(pattern, s))
              .filter(Objects::nonNull)
              .toList();

        var results = new ArrayList<Future<ProxyItem>>();
        var service = Executors.newFixedThreadPool(THREADS_TO_CHECK);
        var counter = new AtomicInteger(0);
        proxyList.forEach(pi -> Arrays.stream(PROXY_TYPES)
              .forEach(proxyType -> results.add(service.submit(() ->
                    processProxyEntity(proxyList, counter, pi, proxyType)))));
        service.shutdown();
        try {
            var timeout = (long) TIMEOUT_MILLIS * results.size() * PROXY_TYPES.length;
            var res = service.awaitTermination(timeout, TimeUnit.MILLISECONDS);
            if (!res) System.err.println("\nShutdown abnormally");
        } catch (InterruptedException e) {throw new RuntimeException(e);}
        System.out.println("\nReady proxies:");
        try (var fw = new FileWriter(RESULTS_FILE)) {
            results.stream()
                  .filter(Objects::nonNull)
                  .map(Utils::getProxyItemFromFuture)
                  .filter(Objects::nonNull)
                  .sorted(Comparator.comparingLong(ProxyItem::getResponseTime))
                  .forEach(p -> {
                      Utils.writeToFile(fw, p);
                      System.out.println(">> " + p);
                  });
        }
        service.shutdownNow();
    }

    private static ProxyItem processProxyEntity(List<ProxyItem> proxyList, AtomicInteger counter, ProxyItem pi, Proxy.Type proxyType) {
        var proxyItem = pi.setProxyType(proxyType);
        var address = new InetSocketAddress(proxyItem.getPath(), proxyItem.getPort());
        var proxy = new Proxy(proxyItem.getProxyType(), address);
        try {
            System.out.printf("\rTry: [%d/%d]%s",
                  counter.incrementAndGet(),
                  proxyList.size() * PROXY_TYPES.length,
                  proxyItem);
            var startTime = new Date().getTime();
            var tmpElement = Utils.checkByVk(proxy);
            if (tmpElement == null)
                return null;
            return proxyItem.setProxyType(proxyItem.getProxyType())
                  .setResponseTime(new Date().getTime() - startTime)
                  .setResponse(tmpElement.text());
        } catch (IOException e) {return null;}
    }

}