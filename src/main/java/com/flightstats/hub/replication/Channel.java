package com.flightstats.hub.replication;

public class Channel {
    private final String name;
    private final String url;

    public Channel(String name, String url) {
        this.name = name;
        if (!url.endsWith("/")) {
            url += "/";
        }
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Channel channel = (Channel) o;

        if (!name.equals(channel.name)) return false;
        if (!url.equals(channel.url)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + url.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Channel{" +
                "name='" + name + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}
