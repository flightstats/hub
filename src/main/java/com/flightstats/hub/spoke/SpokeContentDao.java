package com.flightstats.hub.spoke;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.model.ChannelConfiguration;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.InsertedContentKey;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * This is the entry point in the Hub's storage system, Spoke.
 * <p>
 * It is called in-process on the originating Hub server, and this class will
 * call the other Spoke servers in the cluster.
 * <p>
 * Eventually, it may make sense to pull this out as a separate system.
 */
public class SpokeContentDao implements ContentDao {

    private static final ObjectMapper mapper = new ObjectMapper();
    private final static Logger logger = LoggerFactory.getLogger(SpokeContentDao.class);
    private static final DateTimeFormatter pathFormatter = DateTimeFormat.forPattern("yyyy/MM/dd/HH/mm/ssSSS").withZoneUTC();
    private final SpokeFileStore spokeFileStore;

    @Inject
    public SpokeContentDao(SpokeFileStore spokeFileStore) {
        this.spokeFileStore = spokeFileStore;
    }

    @Override
    public InsertedContentKey write(String channelName, Content content, long ttlDays) {
        if (content.isNewContent()) {
            content.setContentKey(new ContentKey());
        } else {
            //todo - gfm - 10/31/14 - how should replication be handled?
        }
        try {
            //todo - gfm - 11/12/14 - clean this up
            ContentKey key = content.getContentKey().get();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zipOut = new ZipOutputStream(baos);
            zipOut.putNextEntry(new ZipEntry("meta"));
            ObjectNode objectNode = mapper.createObjectNode();
            //todo - gfm - 11/12/14 - make headers a map
            objectNode.put("millis", key.getMillis());
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
            ByteStreams.copy(new ByteArrayInputStream(content.getData()), zipOut);
            zipOut.close();
            spokeFileStore.write(getPath(channelName, key), baos.toByteArray());

            //todo - gfm - 11/11/14 - send async to other stores, wait for success
            return new InsertedContentKey(key, new DateTime(key.getMillis()).toDate());
        } catch (IOException e) {
            logger.warn("what's up?", e);
            return null;
        }
    }

    private String getPath(String channelName, ContentKey key) {
        return channelName + "/" + key.toString(pathFormatter);
    }

    @Override
    public Content read(String channelName, ContentKey key) {
        String path = getPath(channelName, key);
        try {
            byte[] read = spokeFileStore.read(path);
            ZipInputStream zipStream = new ZipInputStream(new ByteArrayInputStream(read));
            zipStream.getNextEntry();
            byte[] bytes = ByteStreams.toByteArray(zipStream);
            JsonNode jsonNode = mapper.readTree(bytes);
            Content.Builder builder = Content.builder()
                    .withMillis(jsonNode.get("millis").asLong())
                    .withContentKey(key)
                    .withContentLanguage(jsonNode.get("contentLanguage").asText())
                    .withContentType(jsonNode.get("contentType").asText())
                    .withUser(jsonNode.get("user").asText());
            zipStream.getNextEntry();
            bytes = ByteStreams.toByteArray(zipStream);
            //todo - gfm - 11/11/14 - try next store
            //todo - gfm - 11/11/14 - try 3rd store
            return builder
                    .withData(bytes)
                    .build();
        } catch (IOException e) {
            logger.warn("unable to get data" + path, e);
            return null;
        }
    }

    @Override
    public void initializeChannel(ChannelConfiguration configuration) {
        //todo - gfm - 11/11/14 - do anything?
    }

    @Override
    public Collection<ContentKey> getKeys(String channelName, DateTime startTime, DateTime endTime) {

        return null;
    }

    @Override
    public Collection<ContentKey> getKeys(String channelName, ContentKey contentKey, int count) {
        return null;
    }

    @Override
    public void delete(String channelName) {

    }
}
