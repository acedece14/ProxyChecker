package by.katz;

import org.jsoup.Jsoup;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

public class Main {

    private static final int TIMEOUT_MILLIS = 10000;
    private static final int THREADS_TO_CHECK = 50;
    private static final int PROXY_TYPES_COUNT = 2;
    private static final File PROXY_LIST_FILE = new File("proxy_list.txt");
    private static final Proxy.Type[] PROXY_TYPES = new Proxy.Type[]{
          Proxy.Type.DIRECT,
          Proxy.Type.HTTP,
          Proxy.Type.SOCKS
    };

    public static void main(String[] args) throws IOException {
        var pattern = Pattern.compile("(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}):(\\d+)(?:,.*)?");
        var proxyList = Files.readAllLines(PROXY_LIST_FILE.toPath()).stream()
              .map(s -> {
                  try {
                      var matcher = pattern.matcher(s);
                      if (matcher.find()) {
                          var ipAddress = matcher.group(1);
                          var port = matcher.group(PROXY_TYPES_COUNT);
                          return new ProxyItem(ipAddress, Integer.parseInt(port));
                      }
                  } catch (Exception e) {e.printStackTrace();}
                  return null;
              }).filter(Objects::nonNull)
              .toList();

        var results = new ArrayList<Future<ProxyItem>>();
        var service = Executors.newFixedThreadPool(THREADS_TO_CHECK);
        var counter = new AtomicInteger(0);
        proxyList.forEach(pi -> Arrays.stream(PROXY_TYPES)
              .forEach(type -> {
                  var proxyItem = pi.setProxyType(type);
                  results.add(service.submit(() -> {
                      var address = new InetSocketAddress(proxyItem.path, proxyItem.port);
                      var proxy = new Proxy(proxyItem.proxyType, address);
                      try {
                          System.out.print("\rTry: [" + counter.incrementAndGet() + "/" + proxyList.size() * PROXY_TYPES.length + "]" + proxyItem);
                          var startTime = new Date().getTime();
                          var doc = Jsoup.connect("https://vk.com/")
                                .timeout(TIMEOUT_MILLIS)
                                .proxy(proxy)
                                .get();
                          var tmpElement = doc.selectFirst(".VkIdForm__header");
                          if (tmpElement == null)
                              return null;
                          return proxyItem.setProxyType(proxyItem.proxyType)
                                .setResponseTime(new Date().getTime() - startTime)
                                .setResponse(tmpElement.text());
                      } catch (IOException e) {return null;}
                  }));
              }));
        service.shutdown();
        try {
            var res = service.awaitTermination((long) TIMEOUT_MILLIS * results.size() * PROXY_TYPES_COUNT, TimeUnit.MILLISECONDS);
            if (!res) System.err.println("\nShutdown abnormally");
        } catch (InterruptedException e) {throw new RuntimeException(e);}
        System.out.println("\nReady proxies:");
        results.stream()
              .filter(Objects::nonNull)
              .map(r -> {
                  try {
                      return r.get();
                  } catch (InterruptedException | ExecutionException ignored) {}
                  return null;
              }).filter(Objects::nonNull)
              .sorted((o1, o2) -> Long.compare(o2.getResponseTime(), o1.getResponseTime()))
              .forEach(p -> System.out.println(">> " + p));
        service.shutdownNow();
    }

    @SuppressWarnings("UnusedReturnValue")
    private static class ProxyItem implements Cloneable {
        private final String path;
        private final int port;
        private Proxy.Type proxyType;
        private Long responseTime = 0L;
        private String response = null;

        public ProxyItem(String path, int port) {
            this.path = path;
            this.port = port;
        }

        public Long getResponseTime() {
            return responseTime;
        }

        public ProxyItem setResponseTime(long responseTime) {
            this.responseTime = responseTime;
            return this;
        }

        @Override public String toString() {
            return "ProxyItem{" +
                  "address='" + path + ":" + port +
                  ", proxyType=" + proxyType +
                  (responseTime > 0 ? ", responseTime=" + responseTime : "") +
                  (response != null ? ", response='" + response + '\'' : "") +
                  '}';
        }

        public ProxyItem setProxyType(Proxy.Type proxyType) {
            this.proxyType = proxyType;
            return this;
        }

        public ProxyItem setResponse(String response) {
            this.response = response;
            return this;
        }

        @Override public ProxyItem clone() {
            try {
                return (ProxyItem) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new AssertionError();
            }
        }
    }

}