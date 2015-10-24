package com.flightstats.hub.dao;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.dao.aws.S3BucketName;
import com.flightstats.hub.dao.aws.S3Util;
import com.flightstats.hub.metrics.MetricsSender;
import com.flightstats.hub.model.*;
import com.flightstats.hub.spoke.SpokeMarshaller;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.base.Optional;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class S3BatchContentDao implements ContentDao {

    private final static Logger logger = LoggerFactory.getLogger(S3BatchContentDao.class);

    private final AmazonS3 s3Client;
    private final MetricsSender sender;
    private final boolean useEncrypted;
    private final int s3MaxQueryItems;
    private final String s3BucketName;

    private static final ObjectMapper mapper = new ObjectMapper();

    @Inject
    public S3BatchContentDao(AmazonS3 s3Client, S3BucketName s3BucketName, MetricsSender sender) {
        this.s3Client = s3Client;
        this.sender = sender;
        this.useEncrypted = HubProperties.getProperty("app.encrypted", false);
        this.s3MaxQueryItems = HubProperties.getProperty("s3.maxQueryItems", 1000);
        this.s3BucketName = s3BucketName.getS3BucketName();
    }

    @Override
    public ContentKey write(String channelName, Content content) throws Exception {
        throw new UnsupportedOperationException("single writes are not supported");
    }

    @Override
    public Content read(String channelName, ContentKey key) {
        try {
            return getS3Object(channelName, key);
        } catch (SocketTimeoutException e) {
            logger.warn("SocketTimeoutException : unable to read " + channelName + " " + key);
            try {
                return getS3Object(channelName, key);
            } catch (Exception e2) {
                logger.warn("unable to read second time " + channelName + " " + key + " " + e.getMessage(), e2);
                return null;
            }
        } catch (Exception e) {
            logger.warn("unable to read " + channelName + " " + key, e);
            return null;
        }
    }

    private Content getS3Object(String channelName, ContentKey key) throws IOException {
        try {
            sender.send("channel." + channelName + ".s3Batch.get", 1);
            MinutePath minutePath = new MinutePath(key.getTime());
            S3Object object = s3Client.getObject(s3BucketName, getS3BatchItemsKey(channelName, minutePath));
            ZipInputStream zipStream = new ZipInputStream(object.getObjectContent());

            ZipEntry nextEntry = zipStream.getNextEntry();
            while (nextEntry != null) {
                logger.trace("found zip entry {} in {}", nextEntry.getName(), minutePath);
                if (nextEntry.getName().equals(key.toUrl())) {
                    Content.Builder builder = Content.builder()
                            .withContentKey(key);
                    byte[] bytes = ByteStreams.toByteArray(zipStream);
                    logger.trace("returning content {} bytes {}", key, bytes.length);
                    String comment = new String(nextEntry.getExtra());
                    SpokeMarshaller.setMetaData(comment, builder);
                    builder.withData(bytes);
                    return builder.build();
                }
                nextEntry = zipStream.getNextEntry();
            }
        } catch (AmazonS3Exception e) {
            if (e.getStatusCode() != 404) {
                logger.warn("AmazonS3Exception : unable to read " + channelName + " " + key, e);
            }
        }
        return null;
    }

    //todo - gfm - 10/23/15 - add batch read

    @Override
    public SortedSet<ContentKey> queryByTime(String channelName, DateTime startTime, TimeUtil.Unit unit, Traces traces) {
        SortedSet<ContentKey> keys = new TreeSet<>();
        DateTime rounded = unit.round(startTime);
        MinutePath minutePath = new MinutePath(rounded);
        MinutePath endMinute = new MinutePath(rounded.plus(unit.getDuration()));
        traces.add("queryByTime ", channelName, rounded, unit);
        while (minutePath.getTime().isBefore(endMinute.getTime())) {
            addKeys(channelName, keys, minutePath, traces);
            minutePath = new MinutePath(minutePath.getTime().plusMinutes(1));
        }
        traces.add("found keys", keys);
        return keys;
    }

    private void addKeys(String channelName, SortedSet<ContentKey> keys, MinutePath minutePath, Traces traces) {
        try {
            S3Object object = s3Client.getObject(s3BucketName, getS3BatchIndexKey(channelName, minutePath));
            byte[] bytes = ByteStreams.toByteArray(object.getObjectContent());
            JsonNode root = mapper.readTree(bytes);
            JsonNode items = root.get("items");
            for (JsonNode item : items) {
                keys.add(ContentKey.fromUrl(item.asText()).get());
            }
            traces.add("addKeys ", minutePath, items.size());
        } catch (AmazonS3Exception e) {
            if (e.getStatusCode() != 404) {
                logger.warn("unable to get index " + channelName, minutePath, e);
                traces.add("issue with getting keys", e);
            } else {
                traces.add("no keys ", minutePath);
            }
        } catch (IOException e) {
            logger.warn("unable to get index " + channelName, minutePath, e);
            traces.add("issue with getting keys", e);
        }
    }

    @Override
    public SortedSet<ContentKey> query(DirectionQuery query) {
        //todo - gfm - 10/19/15 - in S3ContentDao, this is based on queryByTime
        return null;
    }

    @Override
    public void delete(String channelName) {
        //todo - gfm - 10/19/15 - look at S3ContentDao
    }

    @Override
    public void initialize() {
        S3Util.initialize(s3BucketName, s3Client);
    }

    @Override
    public Optional<ContentKey> getLatest(String channel, ContentKey limitKey, Traces traces) {
        throw new UnsupportedOperationException("use query interface");
    }

    @Override
    public void deleteBefore(String channelName, ContentKey limitKey) {
        //todo - gfm - 10/19/15 - look at S3ContentDao
    }

    @Override
    public void writeBatch(String channel, MinutePath path, List<ContentKey> keys, byte[] bytes) {
        try {
            logger.debug("writing batch {} keys {} bytes {}", path, keys.size(), bytes.length);
            writeBatchItems(channel, path, bytes);
            long size = writeBatchIndex(channel, path, keys);
            sender.send("channel." + channel + ".s3Batch.put", 2);
            sender.send("channel." + channel + ".s3Batch.bytes", bytes.length + size);
        } catch (Exception e) {
            logger.warn("unable to write batch to S3 " + channel + " " + path, e);
            throw e;
        }
    }

    private long writeBatchIndex(String channel, MinutePath path, List<ContentKey> keys) {
        String batchIndexKey = getS3BatchIndexKey(channel, path);
        ObjectNode root = mapper.createObjectNode();
        root.put("id", path.toUrl());
        ArrayNode items = root.putArray("items");
        for (ContentKey key : keys) {
            items.add(key.toUrl());
        }
        String index = root.toString();
        logger.trace("index is {} {}", batchIndexKey, index);
        byte[] bytes = index.getBytes(StandardCharsets.UTF_8);
        putObject(batchIndexKey, bytes);
        return bytes.length;
    }

    private void writeBatchItems(String channel, MinutePath path, byte[] bytes) {
        String batchItemsKey = getS3BatchItemsKey(channel, path);
        putObject(batchItemsKey, bytes);
    }

    private void putObject(String batchIndexKey, byte[] bytes) {
        ObjectMetadata metadata = new ObjectMetadata();
        InputStream stream = new ByteArrayInputStream(bytes);
        metadata.setContentLength(bytes.length);
        if (useEncrypted) {
            metadata.setSSEAlgorithm(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION);
        }
        PutObjectRequest request = new PutObjectRequest(s3BucketName, batchIndexKey, stream, metadata);
        s3Client.putObject(request);
    }

    private String getS3BatchItemsKey(String channelName, MinutePath path) {
        return channelName + "/batch/items/" + path.toUrl();
    }

    private String getS3BatchIndexKey(String channelName, MinutePath path) {
        return channelName + "/batch/index/" + path.toUrl();
    }
}
