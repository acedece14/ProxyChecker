package by.katz;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.Proxy;
import java.util.regex.Pattern;

import static by.katz.Main.TIMEOUT_MILLIS;

public class Utils {
    static ProxyItem parseListItem(Pattern pattern, String s) {
        try {
            var matcher = pattern.matcher(s);
            if (matcher.find()) {
                var ipAddress = matcher.group(1);
                var port = matcher.group(2);
                return new ProxyItem(ipAddress, Integer.parseInt(port));
            }
        } catch (Exception e) {e.printStackTrace();}
        return null;
    }

    static Element checkByVk(Proxy proxy) throws IOException {
        var doc = Jsoup.connect("https://vk.com/")
              .timeout(TIMEOUT_MILLIS)
              .proxy(proxy)
              .get();
        return doc.selectFirst(".VkIdForm__header");
    }
}
