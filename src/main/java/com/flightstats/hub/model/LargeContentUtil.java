package com.flightstats.hub.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

public class LargeContentUtil {

    private static final Logger logger = LoggerFactory.getLogger(LargeContentUtil.class);
    public static final String CONTENT_TYPE = "application/hub";
    private static AtomicReference<ObjectMapper> mapper;

    @Inject
    public void initialize(ObjectMapper mapper) {
        LargeContentUtil.mapper.set(mapper);
    }

    public static Content createIndex(Content largePayload) {
        Content.Builder builder = Content.builder();
        builder.withContentType(CONTENT_TYPE);
        ObjectNode data = mapper.get().createObjectNode();
        ContentKey largeKey = largePayload.getContentKey().get();
        data.put("key", largeKey.toUrl());
        data.put("size", largePayload.getSize());
        if (largePayload.getContentType().isPresent()) {
            data.put("content-type", largePayload.getContentType().get());
        }
        builder.withData(data.toString().getBytes());
        if (largePayload.isReplicated()) {
            builder.withContentKey(largeKey);
        } else {
            builder.withContentKey(new ContentKey());
        }
        return builder.withLarge(true).build();
    }

    public static Content fromIndex(Content content) {
        try {
            String data = new String(content.getData());
            JsonNode jsonNode = mapper.get().readTree(data);
            Content.Builder builder = Content.builder();
            builder.withContentKey(ContentKey.fromUrl(jsonNode.get("key").asText()).get());
            builder.withContentType(jsonNode.get("content-type").asText());
            builder.withSize(jsonNode.get("size").asLong());
            builder.withLarge(true);
            return builder.build();
        } catch (IOException e) {
            logger.info("trying to read " + content.getContentKey(), e);
            throw new RuntimeException(e);
        }
    }
}
