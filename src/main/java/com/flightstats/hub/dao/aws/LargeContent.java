package com.flightstats.hub.dao.aws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

class LargeContent {

    private static final Logger logger = LoggerFactory.getLogger(LargeContent.class);
    private static final ObjectMapper mapper = HubProvider.getInstance(ObjectMapper.class);

    static Content createIndex(Content largePayload) {
        Content.Builder builder = Content.builder();
        builder.withContentType(S3LargeContentDao.CONTENT_TYPE);
        ObjectNode data = mapper.createObjectNode();
        data.put("key", largePayload.getContentKey().get().toUrl());
        data.put("size", largePayload.getSize());
        if (largePayload.getContentType().isPresent()) {
            data.put("content-type", largePayload.getContentType().get());
        }
        builder.withData(data.toString().getBytes());
        builder.withContentKey(new ContentKey());
        return builder.build();
    }

    static Content fromIndex(Content content) {
        try {
            String data = new String(content.getData());
            JsonNode jsonNode = mapper.readTree(data);
            Content.Builder builder = Content.builder();
            builder.withContentKey(ContentKey.fromUrl(jsonNode.get("key").asText()).get());
            builder.withContentType(jsonNode.get("content-type").asText());
            return builder.build();
        } catch (IOException e) {
            logger.info("trying to read " + content.getContentKey(), e);
            throw new RuntimeException(e);
        }
    }
}
