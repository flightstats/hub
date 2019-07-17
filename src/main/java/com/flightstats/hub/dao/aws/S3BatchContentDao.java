package com.flightstats.hub.dao.aws;

import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.config.properties.AppProperties;
import com.flightstats.hub.config.properties.S3Properties;
import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.dao.ContentMarshaller;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.metrics.StatsdReporter;
import com.flightstats.hub.metrics.Traces;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.ContentPath;
import com.flightstats.hub.model.ContentPathKeys;
import com.flightstats.hub.model.DirectionQuery;
import com.flightstats.hub.model.MinutePath;
import com.flightstats.hub.model.TimeQuery;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.io.ByteStreams;
import com.google.inject.Singleton;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import javax.inject.Inject;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Singleton
@Slf4j
public class S3BatchContentDao implements ContentDao {

    private static final String BATCH_INDEX = "Batch/index/";
    private static final String BATCH_ITEMS = "Batch/items/";
    private static final ObjectMapper mapper = new ObjectMapper();

    private final boolean useEncrypted;
    private final int s3MaxQueryItems;

    private final HubS3Client s3Client;
    private final String bucketName;
    private final StatsdReporter statsdReporter;
    private final S3Util s3Util;

    @Inject
    public S3BatchContentDao(HubS3Client s3Client,
                             StatsdReporter statsdReporter,
                             AppProperties appProperties,
                             S3Properties s3Properties,
                             S3Util s3Util) {
        this.statsdReporter = statsdReporter;
        this.s3Client = s3Client;

        this.useEncrypted = appProperties.isAppEncrypted();
        this.s3MaxQueryItems = s3Properties.getMaxQueryItems();
        this.bucketName = s3Properties.getBucketName();
        this.s3Util = s3Util;
    }


    @Override
    public ContentKey insert(String channelName, Content content) {
        throw new UnsupportedOperationException("single writes are not supported");
    }

    @Override
    public Content get(String channelName, ContentKey key) {
        try {
            return getS3Object(channelName, key);
        } catch (SocketTimeoutException | SocketException e) {
            log.warn("Socket Exception : unable to read " + channelName + " " + key + " " + e.getMessage() + " " + e.getClass());
            try {
                return getS3Object(channelName, key);
            } catch (Exception e2) {
                log.warn("unable to read second time " + channelName + " " + key + " " + e.getMessage(), e2);
                return null;
            }
        } catch (Exception e) {
            log.warn("unable to read " + channelName + " " + key, e);
            throw new RuntimeException(e);
        }
    }

    private Content getS3Object(String channel, ContentKey key) throws IOException {
        log.trace("S3BatchContentDao.getS3Object {} {}", channel, key);
        return readBatch(channel, key).get(key);
    }

    @Override
    public Map<ContentKey, Content> readBatch(String channelName, ContentKey key) throws IOException {
        MinutePath minutePath = new MinutePath(key.getTime());
        return mapMinute(channelName, minutePath);
    }

    private Map<ContentKey, Content> mapMinute(String channel, MinutePath minutePath) throws IOException {
        Map<ContentKey, Content> map = new HashMap<>();
        try (ZipInputStream zipStream = getZipInputStream(channel, minutePath)) {
            ZipEntry nextEntry = zipStream.getNextEntry();
            while (nextEntry != null) {
                log.trace("found zip entry {} in {}", nextEntry.getName(), minutePath);
                ContentKey contentKey = ContentKey.fromUrl(nextEntry.getName()).get();
                map.put(contentKey, getContent(contentKey, zipStream, nextEntry));
                nextEntry = zipStream.getNextEntry();
            }
        } catch (AmazonS3Exception e) {
            if (e.getStatusCode() != 404) {
                log.warn("AmazonS3Exception : unable to read " + channel + " " + minutePath, e);
            }
        } finally {
            ActiveTraces.getLocal().add("S3BatchContentDao.getS3Object completed");
        }
        return map;
    }

    private Content getContent(ContentKey key, ZipInputStream zipStream, ZipEntry nextEntry) throws IOException {
        Content.Builder builder = Content.builder()
                .withContentKey(key);
        byte[] bytes = ByteStreams.toByteArray(zipStream);
        log.trace("returning content {} bytes {}", key, bytes.length);
        String comment = new String(nextEntry.getExtra());
        ContentMarshaller.setMetaData(comment, builder);
        builder.withData(bytes);
        return builder.build();
    }

    @SneakyThrows
    private ZipInputStream getZipInputStream(String channel, ContentPathKeys minutePath) {
        ActiveTraces.getLocal().add("S3BatchContentDao.getZipInputStream");
        long start = System.currentTimeMillis();
        try {
            GetObjectRequest request = new GetObjectRequest(bucketName, getS3BatchItemsKey(channel, minutePath));
            try (S3Object object = s3Client.getObject(request)) {
                return new ZipInputStream(new BufferedInputStream(object.getObjectContent()));
            }
        } finally {
            statsdReporter.time(channel, "s3.get", start, "type:batch");
        }
    }

    @Override
    public boolean streamMinute(String channel, MinutePath minutePath, boolean descending, Consumer<Content> callback) {
        if (descending) {
            return descending(channel, minutePath, callback);
        }
        return ascending(channel, minutePath, callback);
    }

    private boolean descending(String channel, MinutePath minutePath, Consumer<Content> callback) {
        boolean found = false;
        try {
            Map<ContentKey, Content> map = mapMinute(channel, minutePath);
            NavigableSet<ContentKey> descendingSet = new TreeSet<>(map.keySet()).descendingSet();
            for (ContentKey contentKey : descendingSet) {
                if (minutePath.getKeys().contains(contentKey)) {
                    callback.accept(map.get(contentKey));
                    found = true;
                }
            }
        } catch (IOException e) {
            log.warn("unexpected IOException for " + channel + " " + minutePath, e);
        }
        return found;
    }

    private boolean ascending(String channel, MinutePath minutePath, Consumer<Content> callback) {
        Map<String, ContentKey> keyMap = new HashMap<>();
        boolean found = false;
        for (ContentKey key : minutePath.getKeys()) {
            keyMap.put(key.toUrl(), key);
        }
        try (ZipInputStream zipStream = getZipInputStream(channel, minutePath)) {
            ZipEntry nextEntry = zipStream.getNextEntry();
            while (nextEntry != null) {
                log.trace("found zip entry {} in {}", nextEntry.getName(), minutePath);
                ContentKey key = keyMap.get(nextEntry.getName());
                if (key != null) {
                    callback.accept(getContent(key, zipStream, nextEntry));
                    found = true;
                }
                nextEntry = zipStream.getNextEntry();
            }
        } catch (AmazonS3Exception e) {
            if (e.getStatusCode() != 404) {
                log.warn("AmazonS3Exception : unable to read " + channel + " " + minutePath, e);
            }
        } catch (IOException e) {
            log.warn("unexpected IOException for " + channel + " " + minutePath, e);
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
                .withBucketName(bucketName)
                .withPrefix(channel + BATCH_INDEX + query.getUnit().format(rounded))
                .withMaxKeys(s3MaxQueryItems);
        SortedSet<MinutePath> minutePaths = listMinutePaths(channel, request, traces, true);
        for (MinutePath minutePath : minutePaths) {
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
        getKeysForMinute(
                channel,
                minutePath,
                traces,
                item -> keys.add(ContentKey.fromUrl(item.asText()).get()));
    }

    private void getKeysForMinute(String channel, MinutePath minutePath, Traces traces, Consumer<JsonNode> itemNodeConsumer) {
        long start = System.currentTimeMillis();
        GetObjectRequest request = new GetObjectRequest(bucketName, getS3BatchIndexKey(channel, minutePath));
        try (S3Object object = s3Client.getObject(request)) {
            byte[] bytes = ByteStreams.toByteArray(object.getObjectContent());
            JsonNode root = mapper.readTree(bytes);
            JsonNode items = root.get("items");
            for (JsonNode item : items) {
                itemNodeConsumer.accept(item);
            }
            traces.add("S3BatchContentDao.getKeysForMinute ", minutePath, items.size());
        } catch (AmazonS3Exception e) {
            if (e.getStatusCode() != 404) {
                log.warn("unable to get index " + channel, minutePath, e);
                traces.add("S3BatchContentDao.getKeysForMinute issue with getting keys", e);
            } else {
                traces.add("S3BatchContentDao.getKeysForMinute no keys ", minutePath);
            }
        } catch (IOException e) {
            log.warn("unable to get index " + channel, minutePath, e);
            traces.add("issue with getting keys", e);
        } finally {
            statsdReporter.time(channel, "s3.get", start, "type:batch");
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
                contentKeys = s3Util.queryPrevious(query, this);
            }
            traces.add("S3BatchContentDao.query completed", contentKeys);
        } catch (Exception e) {
            log.warn("query exception" + query, e);
            traces.add("S3BatchContentDao.query exception", e);
        }
        return contentKeys;
    }

    private SortedSet<ContentKey> handleNext(DirectionQuery query) {
        SortedSet<ContentKey> keys = new TreeSet<>();
        Traces traces = ActiveTraces.getLocal();
        DateTime endTime = query.getChannelStable();
        DateTime markerTime = query.getStartKey().getTime().minusMinutes(1);
        int queryItems = Math.min(s3MaxQueryItems, query.getCount());
        do {
            String channel = query.getChannelName();
            ListObjectsRequest request = new ListObjectsRequest()
                    .withBucketName(bucketName)
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
                    if (contentKey.compareTo(query.getStartKey()) > 0
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
        long start = System.currentTimeMillis();
        ObjectListing listing = s3Client.listObjects(request);
        statsdReporter.time(channel, "s3.list", start, "type:batch");
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
            s3Util.delete(channel + BATCH_ITEMS, limitKey, bucketName, s3Client);
            s3Util.delete(channel + BATCH_INDEX, limitKey, bucketName, s3Client);
            log.info("completed deleteBefore of " + channel);
        } catch (Exception e) {
            log.warn("unable to delete {} in {}", channel, bucketName, e);
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
        s3Client.initialize();
    }

    @Override
    public Optional<ContentKey> getLatest(String channel, ContentKey limitKey, Traces traces) {
        throw new UnsupportedOperationException("use query interface");
    }

    @Override
    public void writeBatch(String channel, ContentPath path, Collection<ContentKey> keys, byte[] bytes) {
        ActiveTraces.getLocal().add("S3BatchContentDao.writeBatch", channel, path);
        try {
            log.debug("writing {} batch {} keys {} bytes {}", channel, path, keys.size(), bytes.length);
            writeBatchItems(channel, path, bytes);
            writeBatchIndex(channel, path, keys);
        } catch (Exception e) {
            log.warn("unable to write batch to S3 " + channel + " " + path, e);
            throw e;
        } finally {
            ActiveTraces.getLocal().add("S3BatchContentDao.writeBatch completed", channel, path);
        }
    }

    private void writeBatchIndex(String channel, ContentPath path, Collection<ContentKey> keys) {
        String batchIndexKey = getS3BatchIndexKey(channel, path);
        ObjectNode root = mapper.createObjectNode();
        root.put("id", path.toUrl());
        ArrayNode items = root.putArray("items");
        for (ContentKey key : keys) {
            items.add(key.toUrl());
        }
        String index = root.toString();
        log.trace("index is {} {}", batchIndexKey, index);
        byte[] bytes = index.getBytes(StandardCharsets.UTF_8);
        putObject(channel, batchIndexKey, bytes);
    }

    private void writeBatchItems(String channel, ContentPath path, byte[] bytes) {
        String batchItemsKey = getS3BatchItemsKey(channel, path);
        putObject(channel, batchItemsKey, bytes);
    }

    private void putObject(String channel, String batchIndexKey, byte[] bytes) {
        long start = System.currentTimeMillis();
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            InputStream stream = new ByteArrayInputStream(bytes);
            metadata.setContentLength(bytes.length);
            if (useEncrypted) {
                metadata.setSSEAlgorithm(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION);
            }
            PutObjectRequest request = new PutObjectRequest(bucketName, batchIndexKey, stream, metadata);
            s3Client.putObject(request);
        } finally {
            statsdReporter.time(channel, "s3.put", start, bytes.length, "type:batch");
        }
    }

    private String getS3BatchItemsKey(String channel, ContentPath path) {
        return channel + BATCH_ITEMS + path.toUrl();
    }

    private String getS3BatchIndexKey(String channel, ContentPath path) {
        return channel + BATCH_INDEX + path.toUrl();
    }

}
