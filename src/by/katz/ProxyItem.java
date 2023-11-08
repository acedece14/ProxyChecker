package by.katz;

import java.net.Proxy;

class ProxyItem implements Cloneable {
    private final String path;
    private final int port;
    private Proxy.Type proxyType = null;
    private Long responseTime = 0L;
    private String response = null;

    public ProxyItem(String path, int port) {
        this.path = path;
        this.port = port;
    }

    public String getPath() {return path;}

    public int getPort() {return port;}

    public Proxy.Type getProxyType() {return proxyType;}

    public Long getResponseTime() {
        return responseTime;
    }

    public ProxyItem setProxyType(Proxy.Type proxyType) {
        this.proxyType = proxyType;
        return this;
    }

    public ProxyItem setResponseTime(long responseTime) {
        this.responseTime = responseTime;
        return this;
    }

    public ProxyItem setResponse(String response) {
        this.response = response;
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

    @Override public ProxyItem clone() {
        try {
            return (ProxyItem) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
