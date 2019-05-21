package com.flightstats.hub.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

import static com.flightstats.hub.constant.ContentConstant.CONTENT_TYPE;

@Slf4j
public class LargeContentUtils {

    private final ObjectMapper mapper;

    @Inject
    public LargeContentUtils(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public Content createIndex(Content largePayload) {
        try {
            Content.Builder builder = Content.builder();
            builder.withContentType(CONTENT_TYPE);
            ObjectNode data = mapper.createObjectNode();
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
            Content content = builder.build();
            content.packageStream();
            return content;
        } catch (IOException e) {
            log.warn("unable to create index");
            throw new RuntimeException(e);
        }
    }

    public Content fromIndex(Content content) {
        try {
            String data = new String(content.getData());
            JsonNode jsonNode = mapper.readTree(data);
            Content.Builder builder = Content.builder();
            builder.withContentKey(ContentKey.fromUrl(jsonNode.get("key").asText()).get());
            builder.withContentType(jsonNode.get("content-type").asText());
            builder.withSize(jsonNode.get("size").asLong());
            builder.withLarge(true);
            return builder.build();
        } catch (IOException e) {
            log.info("trying to read " + content.getContentKey(), e);
            throw new RuntimeException(e);
        }
    }
}
