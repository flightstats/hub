package com.flightstats.hub.spoke;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.exception.ContentTooLargeException;
import com.google.common.io.ByteStreams;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class SpokeMarshaller {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final int maxBytes = HubProperties.getProperty("app.maxPayloadSizeMB", 10) * 1024 * 1024;

    public static byte[] toBytes(Content content) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zipOut = new ZipOutputStream(baos);
        zipOut.setLevel(Deflater.BEST_SPEED);
        zipOut.putNextEntry(new ZipEntry("meta"));
        ObjectNode objectNode = mapper.createObjectNode();
        //todo - gfm - 11/12/14 - make headers a map
        if (content.getUser().isPresent()) {
            objectNode.put("user", content.getUser().get());
        }
        if (content.getContentLanguage().isPresent()) {
            objectNode.put("contentLanguage", content.getContentLanguage().get());
        }
        if (content.getContentType().isPresent()) {
            objectNode.put("contentType", content.getContentType().get());
        }
        String meta = objectNode.toString();
        zipOut.write(meta.getBytes());
        zipOut.putNextEntry(new ZipEntry("payload"));
        long copy = ByteStreams.copy(content.getStream(), zipOut);
        if (copy > maxBytes) {
            throw new ContentTooLargeException("max payload size is " + maxBytes + " bytes");
        }
        zipOut.close();
        return baos.toByteArray();
    }

    public static Content toContent(byte[] read, ContentKey key) throws IOException {
        ZipInputStream zipStream = new ZipInputStream(new ByteArrayInputStream(read));
        zipStream.getNextEntry();
        byte[] bytes = ByteStreams.toByteArray(zipStream);
        JsonNode jsonNode = mapper.readTree(bytes);
        Content.Builder builder = Content.builder().withContentKey(key);
        if (jsonNode.has("contentLanguage")) {
            builder.withContentLanguage(jsonNode.get("contentLanguage").asText());
        }
        if (jsonNode.has("contentType")) {
            builder.withContentType(jsonNode.get("contentType").asText());
        }
        if (jsonNode.has("user")) {
            builder.withUser(jsonNode.get("user").asText());
        }

        zipStream.getNextEntry();
        return builder.withStream(zipStream).build();
    }
}
