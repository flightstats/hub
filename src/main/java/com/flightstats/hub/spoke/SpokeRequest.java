package com.flightstats.hub.spoke;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@EqualsAndHashCode(of = "key")
/**
 * Note: this class has a natural ordering that is inconsistent with equals.
 */
public class SpokeRequest implements Comparable<SpokeRequest> {

    private static final ObjectMapper mapper = new ObjectMapper();

    private final long start = System.currentTimeMillis();
    private final String key = UUID.randomUUID().toString();
    private final String path;
    private final String method;

    private long end = 0;
    private long bytes = 0;

    public SpokeRequest(String path, String method) {
        this.path = path;
        this.method = method;
    }

    public void complete() {
        setEnd(System.currentTimeMillis());
        SpokeTracer.completed(this);
    }

    public void complete(long bytes) {
        setBytes(bytes);
        complete();
    }

    @Override
    public String toString() {
        return getTime() + ", " + bytes + ", " + method + ", " + path;
    }

    public ObjectNode toNode() {
        ObjectNode root = mapper.createObjectNode();
        root.put("time", getTime());
        root.put("bytes", getBytes());
        root.put("method", getMethod());
        root.put("path", getPath());
        return root;
    }

    private long getTime() {
        long time = end - start;
        if (end == 0) {
            time = System.currentTimeMillis() - start;
        }
        return time;
    }

    @Override
    public int compareTo(SpokeRequest other) {
        int value = (int) (other.getTime() - this.getTime());
        if (value != 0) {
            return value;
        }
        return key.compareTo(other.getKey());
    }
}
