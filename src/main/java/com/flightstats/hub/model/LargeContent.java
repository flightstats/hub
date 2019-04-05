package com.flightstats.hub.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubBindings;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class LargeContent {

    static final String CONTENT_TYPE = "application/hub";
    private static final ObjectMapper mapper = HubBindings.objectMapper();

    public static Content createIndex(Content largePayload) {
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

    public static Content fromIndex(Content content) {
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
