package com.flightstats.hub.dao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.google.common.io.ByteStreams;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ContentMarshaller {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static byte[] toBytes(Content content) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zipOut = new ZipOutputStream(baos);
        zipOut.setLevel(Deflater.BEST_COMPRESSION);
        zipOut.putNextEntry(new ZipEntry("meta"));
        String meta = getMetaData(content);
        zipOut.write(meta.getBytes());
        zipOut.putNextEntry(new ZipEntry("payload"));
        long bytesCopied = ByteStreams.copy(content.getStream(), zipOut);
        content.setSize(bytesCopied);
        zipOut.setComment("" + bytesCopied);
        zipOut.close();
        return baos.toByteArray();
    }

    public static String getMetaData(Content content) {
        ObjectNode objectNode = mapper.createObjectNode();
        if (content.getContentType().isPresent()) {
            objectNode.put("contentType", content.getContentType().get());
        }
        return objectNode.toString();
    }

    public static Content toContent(byte[] read, ContentKey key) throws IOException {
        ZipInputStream zipStream = new ZipInputStream(new ByteArrayInputStream(read));
        zipStream.getNextEntry();
        byte[] bytes = ByteStreams.toByteArray(zipStream);
        Content.Builder builder = Content.builder().withContentKey(key);
        setMetaData(new String(bytes), builder);
        zipStream.getNextEntry();
        String comment = ZipComment.getZipCommentFromBuffer(read);
        if (comment != null) {
            builder.withSize(Long.parseLong(comment));
        }
        return builder.withStream(zipStream).build();
    }

    public static void setMetaData(String metaData, Content.Builder builder) throws IOException {
        JsonNode jsonNode = mapper.readTree(metaData);
        if (jsonNode.has("contentType")) {
            builder.withContentType(jsonNode.get("contentType").asText());
        }
    }
}
