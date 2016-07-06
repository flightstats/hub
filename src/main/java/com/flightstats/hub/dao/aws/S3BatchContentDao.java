package com.flightstats.hub.dao.aws;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.dao.ContentMarshaller;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.metrics.DataDog;
import com.flightstats.hub.metrics.MetricsSender;
import com.flightstats.hub.metrics.Traces;
import com.flightstats.hub.model.*;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.base.Optional;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import com.timgroup.statsd.StatsDClient;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class S3BatchContentDao implements ContentDao {

    private final static Logger logger = LoggerFactory.getLogger(S3BatchContentDao.class);
    private static final String BATCH_INDEX = "Batch/index/";
    private static final String BATCH_ITEMS = "Batch/items/";
    private static final ObjectMapper mapper = new ObjectMapper();
    private final AmazonS3 s3Client;
    private final MetricsSender sender;
    private final boolean useEncrypted = HubProperties.getProperty("app.encrypted", false);
    private final int s3MaxQueryItems = HubProperties.getProperty("s3.maxQueryItems", 1000);
    private final String s3BucketName;
    private final static StatsDClient statsd = DataDog.statsd;

    @Inject
    public S3BatchContentDao(AmazonS3 s3Client, S3BucketName s3BucketName, MetricsSender sender) {
        this.s3Client = s3Client;
        this.sender = sender;
        this.s3BucketName = s3BucketName.getS3BucketName();
    }

    @Override
    public ContentKey insert(String channelName, Content content) throws Exception {
        throw new UnsupportedOperationException("single writes are not supported");
    }

    @Override
    public Content get(String channelName, ContentKey key) {
        try {
            return getS3Object(channelName, key);
        } catch (SocketTimeoutException | SocketException e) {
            logger.warn("Socket Exception : unable to read " + channelName + " " + key + " " + e.getMessage() + " " + e.getClass());
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

    private Content getS3Object(String channel, ContentKey key) throws IOException {
        logger.trace("S3BatchContentDao.getS3Object {} {}", channel, key);
        MinutePath minutePath = new MinutePath(key.getTime());
        Content content = null;
        long start = System.currentTimeMillis();
        try (ZipInputStream zipStream = getZipInputStream(channel, minutePath)) {
            ZipEntry nextEntry = zipStream.getNextEntry();
            while (nextEntry != null) {
                logger.trace("found zip entry {} in {}", nextEntry.getName(), minutePath);
                if (nextEntry.getName().equals(key.toUrl())) {
                    content = getContent(key, zipStream, nextEntry);
                }
                nextEntry = zipStream.getNextEntry();
            }
        } catch (AmazonS3Exception e) {
            if (e.getStatusCode() != 404) {
                logger.warn("AmazonS3Exception : unable to read " + channel + " " + key, e);
            }
        } finally {
            ActiveTraces.getLocal().add("S3BatchContentDao.getS3Object completed");
            long time = System.currentTimeMillis() - start;
            statsd.recordExecutionTime("s3.get", time, "channel:" + channel, "type:batch");
        }

        return content;
    }

    private Content getContent(ContentKey key, ZipInputStream zipStream, ZipEntry nextEntry) throws IOException {
        Content.Builder builder = Content.builder()
                .withContentKey(key);
        byte[] bytes = ByteStreams.toByteArray(zipStream);
        logger.trace("returning content {} bytes {}", key, bytes.length);
        String comment = new String(nextEntry.getExtra());
        ContentMarshaller.setMetaData(comment, builder);
        builder.withData(bytes);
        return builder.build();
    }

    private ZipInputStream getZipInputStream(String channel, ContentPathKeys minutePath) {
        ActiveTraces.getLocal().add("S3BatchContentDao.getZipInputStream");
        statsd.increment("s3.get", "type:batch", "channel:" + channel);
        sender.send("channel." + channel + ".s3Batch.get", 1);

        S3Object object = s3Client.getObject(s3BucketName, getS3BatchItemsKey(channel, minutePath));
        return new ZipInputStream(new BufferedInputStream(object.getObjectContent()));
    }

    @Override
    public boolean streamMinute(String channel, ContentPathKeys minutePath, Consumer<Content> callback) {
        Map<String, ContentKey> keyMap = new HashMap<>();
        boolean found = false;
        for (ContentKey key : minutePath.getKeys()) {
            keyMap.put(key.toUrl(), key);
        }
        try (ZipInputStream zipStream = getZipInputStream(channel, minutePath)) {
            ZipEntry nextEntry = zipStream.getNextEntry();
            while (nextEntry != null) {
                logger.trace("found zip entry {} in {}", nextEntry.getName(), minutePath);
                ContentKey key = keyMap.get(nextEntry.getName());
                if (key != null) {
                    callback.accept(getContent(key, zipStream, nextEntry));
                    found = true;
                }
                nextEntry = zipStream.getNextEntry();
            }
        } catch (AmazonS3Exception e) {
            if (e.getStatusCode() != 404) {
                logger.warn("AmazonS3Exception : unable to read " + channel + " " + minutePath, e);
            }
        } catch (IOException e) {
            logger.warn("unexpected IOException for " + channel + " " + minutePath, e);
        } finally {
            ActiveTraces.getLocal().add("S3BatchContentDao.streamMinute completed");
        }
        return found;
    }

    @Override
    public SortedSet<ContentKey> queryByTime(TimeQuery query) {
        if (query.getUnit().lessThanOrEqual(TimeUtil.Unit.MINUTES)) {
            return queryMinute(query.getChannelName(), query.getStartTime(), query.getUnit());
        } else {
            return queryHourPlus(query);
        }
    }

    private SortedSet<ContentKey> queryHourPlus(TimeQuery query) {
        Traces traces = ActiveTraces.getLocal();
        SortedSet<ContentKey> keys = new TreeSet<>();
        if (query.getCount() > 0) {
            keys = new ContentKeySet(query.getCount(), query.getLimitKey());
        }

        DateTime rounded = query.getUnit().round(query.getStartTime());
        String channel = query.getChannelName();
        traces.add("S3BatchContentDao.queryHourPlus starting ", channel, rounded, query.getUnit());
        ListObjectsRequest request = new ListObjectsRequest()
                .withBucketName(s3BucketName)
                .withPrefix(channel + BATCH_INDEX + query.getUnit().format(rounded))
                .withMaxKeys(s3MaxQueryItems);
        SortedSet<MinutePath> minutePaths = listMinutePaths(channel, request, traces, true);
        for (MinutePath minutePath : minutePaths) {
            //todo - gfm - 11/5/15 - this could be in parallel, needs to handle throttling by S3
            getKeysForMinute(channel, minutePath, keys, traces);
        }
        traces.add("S3BatchContentDao.queryHourPlus found keys", keys);
        return keys;
    }

    private SortedSet<ContentKey> queryMinute(String channel, DateTime startTime, TimeUtil.Unit unit) {
        Traces traces = ActiveTraces.getLocal();
        SortedSet<ContentKey> keys = new TreeSet<>();
        DateTime rounded = unit.round(startTime);
        traces.add("S3BatchContentDao.queryMinute ", channel, rounded, unit);
        getKeysForMinute(channel, new MinutePath(rounded), keys, traces);
        if (unit.equals(TimeUtil.Unit.SECONDS)) {
            DateTime start = rounded.minusMillis(1);
            DateTime endTime = rounded.plus(unit.getDuration());
            keys = keys.stream()
                    .filter(key -> key.getTime().isAfter(start))
                    .filter(key -> key.getTime().isBefore(endTime))
                    .collect(Collectors.toCollection(TreeSet::new));
        }
        traces.add("S3BatchContentDao.queryMinute completed", keys);
        return keys;
    }

    private void getKeysForMinute(String channel, MinutePath minutePath, SortedSet<ContentKey> keys, Traces traces) {
        getKeysForMinute(channel, minutePath, traces, item -> {
            keys.add(ContentKey.fromUrl(item.asText()).get());
        });
    }

    private void getKeysForMinute(String channel, MinutePath minutePath, Traces traces, Consumer<JsonNode> itemNodeConsumer) {
        try (S3Object object = s3Client.getObject(s3BucketName, getS3BatchIndexKey(channel, minutePath))) {
            statsd.increment("s3.get", "type:batchIndex", "channel:" + channel);
            sender.send("channel." + channel + ".s3Batch.get", 1);
            byte[] bytes = ByteStreams.toByteArray(object.getObjectContent());
            JsonNode root = mapper.readTree(bytes);
            JsonNode items = root.get("items");
            for (JsonNode item : items) {
                itemNodeConsumer.accept(item);
            }
            traces.add("S3BatchContentDao.getKeysForMinute ", minutePath, items.size());
        } catch (AmazonS3Exception e) {
            if (e.getStatusCode() != 404) {
                logger.warn("unable to get index " + channel, minutePath, e);
                traces.add("S3BatchContentDao.getKeysForMinute issue with getting keys", e);
            } else {
                traces.add("S3BatchContentDao.getKeysForMinute no keys ", minutePath);
            }
        } catch (IOException e) {
            logger.warn("unable to get index " + channel, minutePath, e);
            traces.add("issue with getting keys", e);
        }
    }

    @Override
    public SortedSet<ContentKey> query(DirectionQuery query) {
        Traces traces = ActiveTraces.getLocal();
        SortedSet<ContentKey> contentKeys = Collections.emptySortedSet();
        try {
            traces.add("S3BatchContentDao.query", query);
            if (query.isNext()) {
                contentKeys = handleNext(query);
            } else {
                contentKeys = S3Util.queryPrevious(query, this);
            }
            traces.add("S3BatchContentDao.query completed", contentKeys);
        } catch (Exception e) {
            logger.warn("query exception" + query, e);
            traces.add("S3BatchContentDao.query exception", e);
        }
        return contentKeys;
    }

    private SortedSet<ContentKey> handleNext(DirectionQuery query) {
        SortedSet<ContentKey> keys = new TreeSet<>();
        Traces traces = ActiveTraces.getLocal();
        DateTime endTime = TimeUtil.time(query.isStable());
        DateTime markerTime = query.getContentKey().getTime().minusMinutes(1);
        int queryItems = Math.min(s3MaxQueryItems, query.getCount());
        do {
            String channel = query.getChannelName();
            ListObjectsRequest request = new ListObjectsRequest()
                    .withBucketName(s3BucketName)
                    .withPrefix(channel + BATCH_INDEX)
                    .withMarker(channel + BATCH_INDEX + TimeUtil.Unit.MINUTES.format(markerTime))
                    .withMaxKeys(queryItems);
            SortedSet<MinutePath> paths = listMinutePaths(channel, request, traces, false);

            if (paths.isEmpty()) {
                return keys;
            }
            for (MinutePath path : paths) {
                getKeysForMinute(channel, path, traces, item -> {
                    ContentKey contentKey = ContentKey.fromUrl(item.asText()).get();
                    if (contentKey.compareTo(query.getContentKey()) > 0
                            && keys.size() < query.getCount()) {
                        keys.add(contentKey);
                    }
                });
                if (keys.size() >= query.getCount()) {
                    break;
                }
                markerTime = path.getTime();
            }
        } while (keys.size() < query.getCount() && markerTime.isBefore(endTime));
        return keys;
    }

    private SortedSet<MinutePath> listMinutePaths(String channel, ListObjectsRequest request, Traces traces, boolean iterate) {
        SortedSet<MinutePath> paths = new TreeSet<>();
        traces.add("S3BatchContentDao.listMinutePaths ", request.getPrefix(), request.getMarker(), iterate);
        sender.send("channel." + channel + ".s3Batch.list", 1);
        ObjectListing listing = s3Client.listObjects(request);
        List<S3ObjectSummary> summaries = listing.getObjectSummaries();
        for (S3ObjectSummary summary : summaries) {
            String key = summary.getKey();
            Optional<MinutePath> pathOptional = MinutePath.fromUrl(StringUtils.substringAfter(key, channel + BATCH_INDEX));
            if (pathOptional.isPresent()) {
                MinutePath path = pathOptional.get();
                paths.add(path);
            }
        }
        if (iterate && listing.isTruncated()) {
            request.withMarker(channel + BATCH_INDEX + TimeUtil.Unit.MINUTES.format(paths.last().getTime()));
            paths.addAll(listMinutePaths(channel, request, traces, iterate));
        }
        traces.add("S3BatchContentDao.listMinutePaths ", paths);
        return paths;
    }

    @Override
    public void deleteBefore(String channel, ContentKey limitKey) {
        try {
            S3Util.delete(channel + BATCH_ITEMS, limitKey, s3BucketName, s3Client);
            S3Util.delete(channel + BATCH_INDEX, limitKey, s3BucketName, s3Client);
            logger.info("completed deleteBefore of " + channel);
        } catch (Exception e) {
            logger.warn("unable to delete " + channel + " in " + s3BucketName, e);
        }
    }

    @Override
    public void delete(String channel) {
        Traces traces = ActiveTraces.getLocal();
        new Thread(() -> {
            ContentKey limitKey = new ContentKey(TimeUtil.now().plusHours(1), "ZZZZZZ");
            ActiveTraces.start("S3BatchContentDao.delete", traces, limitKey);
            deleteBefore(channel, limitKey);
            ActiveTraces.end();
        }).start();
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
    public void writeBatch(String channel, ContentPath path, Collection<ContentKey> keys, byte[] bytes) {
        ActiveTraces.getLocal().add("S3BatchContentDao.writeBatch", channel, path);
        try {
            logger.debug("writing {} batch {} keys {} bytes {}", channel, path, keys.size(), bytes.length);
            writeBatchItems(channel, path, bytes);
            long indexSize = writeBatchIndex(channel, path, keys);
            statsd.increment("s3.put", "type:batch", "channel:" + channel);
            statsd.count("s3.put.bytes", bytes.length + indexSize, "channel:" + channel, "type:batch");
            sender.send("channel." + channel + ".s3Batch.put", 1);
            sender.send("channel." + channel + ".s3Batch.bytes", bytes.length + indexSize);
        } catch (Exception e) {
            logger.warn("unable to write batch to S3 " + channel + " " + path, e);
            throw e;
        } finally {
            ActiveTraces.getLocal().add("S3BatchContentDao.writeBatch completed", channel, path);
        }
    }

    private long writeBatchIndex(String channel, ContentPath path, Collection<ContentKey> keys) {
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

    private void writeBatchItems(String channel, ContentPath path, byte[] bytes) {
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

    private String getS3BatchItemsKey(String channel, ContentPath path) {
        return channel + BATCH_ITEMS + path.toUrl();
    }

    private String getS3BatchIndexKey(String channel, ContentPath path) {
        return channel + BATCH_INDEX + path.toUrl();
    }
}
