package com.flightstats.hub.dao.aws;

import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.dao.ContentMarshaller;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.metrics.MetricsService;
import com.flightstats.hub.metrics.Traces;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.DirectionQuery;
import com.flightstats.hub.model.TimeQuery;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.base.Function;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

@Singleton
public class S3SingleContentDao implements ContentDao {

    private final static Logger logger = LoggerFactory.getLogger(S3SingleContentDao.class);
    private static final int MAX_ITEMS = 1000 * 1000;
    private final boolean useEncrypted = HubProperties.isAppEncrypted();
    private final int s3MaxQueryItems = HubProperties.getProperty("s3.maxQueryItems", 1000);

    @Inject
    private MetricsService metricsService;
    @Inject
    private HubS3Client s3Client;
    @Inject
    private S3BucketName s3BucketName;

    public void initialize() {
        s3Client.initialize();
    }

    @Override
    public Optional<ContentKey> getLatest(String channel, ContentKey limitKey, Traces traces) {
        throw new UnsupportedOperationException("use query interface");
    }

    public ContentKey insert(String channelName, Content content) {
        return insert(channelName, content, (metadata) -> {
            metadata.addUserMetadata("compressed", "true");
            return content.getData();
        });
    }

    //this is only needed for testing the non-compressed retrieval from S3.
    ContentKey insertOld(String channelName, Content content) {
        return insert(channelName, content, (metadata) -> content.getData());
    }

    private ContentKey insert(String channelName, Content content, Function<ObjectMetadata, byte[]> handler) {
        ContentKey key = content.getContentKey().get();
        ActiveTraces.getLocal().add("S3SingleContentDao.write", key);
        long start = System.currentTimeMillis();
        int length = 0;
        try {
            String s3Key = getS3ContentKey(channelName, key);
            ObjectMetadata metadata = S3SingleContentDao.createObjectMetadata(content, useEncrypted);
            byte[] bytes = handler.apply(metadata);
            length = bytes.length;
            logger.trace("insert {} {} {} {}", channelName, key, content.getSize(), length);
            InputStream stream = new ByteArrayInputStream(bytes);
            metadata.setContentLength(length);
            PutObjectRequest request = new PutObjectRequest(s3BucketName.getS3BucketName(), s3Key, stream, metadata);
            s3Client.putObject(request);
            return key;
        } catch (Exception e) {
            logger.warn("unable to write item to S3 " + channelName + " " + key, e);
            throw e;
        } finally {
            metricsService.time(channelName, "s3.put", start, length, "type:single");
            ActiveTraces.getLocal().add("S3SingleContentDao.write completed");
        }
    }

    @Override
    public void delete(String channelName, ContentKey key) {
        String s3ContentKey = getS3ContentKey(channelName, key);
        DeleteObjectRequest request = new DeleteObjectRequest(s3BucketName.getS3BucketName(), s3ContentKey);
        s3Client.deleteObject(request);
        ActiveTraces.getLocal().add("S3SingleContentDao.deleted", s3ContentKey);
    }

    public Content get(final String channelName, final ContentKey key) {
        ActiveTraces.getLocal().add("S3SingleContentDao.read", key);
        try {
            return getS3Object(channelName, key);
        } catch (SocketTimeoutException e) {
            logger.warn("SocketTimeoutException : unable to read " + channelName + " " + key);
            try {
                return getS3Object(channelName, key);
            } catch (Exception e2) {
                logger.warn("unable to read second time " + channelName + " " + key + " " + e.getMessage(), e2);
                throw new RuntimeException(e);
            }
        } catch (Exception e) {
            logger.warn("unable to read " + channelName + " " + key, e);
            throw new RuntimeException(e);
        } finally {
            ActiveTraces.getLocal().add("S3SingleContentDao.read completed");
        }
    }

    private Content getS3Object(String channelName, ContentKey key) throws IOException {
        long start = System.currentTimeMillis();
        GetObjectRequest request = new GetObjectRequest(s3BucketName.getS3BucketName(), getS3ContentKey(channelName, key));
        try (S3Object object = s3Client.getObject(request)) {
            byte[] bytes = ByteStreams.toByteArray(object.getObjectContent());
            ObjectMetadata metadata = object.getObjectMetadata();
            Map<String, String> userData = metadata.getUserMetadata();
            if (userData.containsKey("compressed")) {
                return ContentMarshaller.toContent(bytes, key);
            }
            Content.Builder builder = Content.builder();
            String type = userData.get("type");
            if (!type.equals("none")) {
                builder.withContentType(type);
            }
            builder.withContentKey(key);
            builder.withData(bytes);
            return builder.build();
        } catch (AmazonS3Exception e) {
            if (e.getStatusCode() != 404) {
                logger.warn("AmazonS3Exception : unable to read " + channelName + " " + key, e);
            }
            return null;
        } finally {
            metricsService.time(channelName, "s3.get", start, "type:single");
        }
    }

    @Override
    public SortedSet<ContentKey> queryByTime(TimeQuery query) {
        logger.debug("queryByTime {} ", query);
        Traces traces = ActiveTraces.getLocal();
        traces.add("S3SingleContentDao.queryByTime", query);
        String timePath = query.getUnit().format(query.getStartTime());
        ListObjectsRequest request = new ListObjectsRequest()
                .withBucketName(s3BucketName.getS3BucketName())
                .withMaxKeys(s3MaxQueryItems);
        ContentKey limitKey = query.getLimitKey();
        if (limitKey == null) {
            request.withPrefix(query.getChannelName() + "/" + timePath);
            limitKey = ContentKey.lastKey(query.getStartTime().plus(query.getUnit().getDuration()));
        } else {
            request.withPrefix(query.getChannelName() + "/");
            request.withMarker(query.getChannelName() + "/" + timePath);
        }
        SortedSet<ContentKey> keys = iterateListObjects(query.getChannelName(), request, MAX_ITEMS, query.getCount(), limitKey);
        traces.add("S3SingleContentDao.queryByTime completed", keys);
        return keys;
    }

    private SortedSet<ContentKey> iterateListObjects(String channel, ListObjectsRequest request,
                                                     int maxItems, int count, ContentKey limitKey) {
        Traces traces = ActiveTraces.getLocal();
        SortedSet<ContentKey> keys = new TreeSet<>();
        if (limitKey != null) {
            keys = new ContentKeySet(count, limitKey);
        }
        logger.trace("list {} {} {}", channel, request.getPrefix(), request.getMarker());
        traces.add("S3SingleContentDao.iterateListObjects prefix:", request.getPrefix(), request.getMarker());
        ObjectListing listing = getObjectListing(request, channel);
        ContentKey marker = addKeys(channel, listing, keys);
        while (shouldContinue(maxItems, limitKey, keys, listing, marker)) {
            request.withMarker(channel + "/" + marker.toUrl());
            logger.trace("list {} {}", channel, request.getMarker());
            traces.add("S3SingleContentDao.iterateListObjects marker:", request.getMarker());
            listing = getObjectListing(request, channel);
            marker = addKeys(channel, listing, keys);
        }
        return keys;
    }

    private ObjectListing getObjectListing(ListObjectsRequest request, String channel) {
        long start = System.currentTimeMillis();
        ObjectListing objects = s3Client.listObjects(request);
        metricsService.time(channel, "s3.list", start, "type:single");
        return objects;
    }

    private boolean shouldContinue(int maxItems, ContentKey limitKey, SortedSet<ContentKey> keys, ObjectListing listing, ContentKey marker) {
        if (marker == null) {
            return false;
        }
        return listing.isTruncated()
                && keys.size() < maxItems
                && marker.getTime().isBefore(limitKey.getTime());
    }

    private ContentKey addKeys(String channelName, ObjectListing listing, Set<ContentKey> keys) {
        Optional<ContentKey> contentKey = Optional.empty();
        List<S3ObjectSummary> summaries = listing.getObjectSummaries();
        for (S3ObjectSummary summary : summaries) {
            contentKey = ContentKey.fromUrl(StringUtils.substringAfter(summary.getKey(), channelName + "/"));
            if (contentKey.isPresent()) {
                if (!keys.add(contentKey.get())) {
                    return contentKey.get();
                }
            }
        }
        return contentKey.orElse(null);
    }

    @Override
    public SortedSet<ContentKey> query(DirectionQuery query) {
        logger.trace("query {}", query);
        Traces traces = ActiveTraces.getLocal();
        traces.add("S3SingleContentDao.query", query);
        SortedSet<ContentKey> contentKeys;
        if (query.isNext()) {
            contentKeys = next(query);
        } else {
            contentKeys = S3Util.queryPrevious(query, this);
        }
        traces.add("S3SingleContentDao.query completed", contentKeys);
        return contentKeys;
    }

    private SortedSet<ContentKey> next(DirectionQuery query) {
        ListObjectsRequest request = new ListObjectsRequest()
                .withBucketName(s3BucketName.getS3BucketName())
                .withPrefix(query.getChannelName() + "/")
                .withMarker(query.getChannelName() + "/" + query.getStartKey().toUrl())
                .withMaxKeys(query.getCount());
        return iterateListObjects(query.getChannelName(), request, query.getCount(), query.getCount(),
                ContentKey.lastKey(TimeUtil.time(query.isStable())));
    }

    private String getS3ContentKey(String channelName, ContentKey key) {
        return channelName + "/" + key.toUrl();
    }

    @Override
    public void deleteBefore(String channel, ContentKey limitKey) {
        try {
            S3Util.delete(channel + "/", limitKey, s3BucketName.getS3BucketName(), s3Client);
            logger.info("completed deletion of " + channel);
        } catch (Exception e) {
            logger.warn("unable to delete " + channel + " in " + s3BucketName.getS3BucketName(), e);
        }
    }

    @Override
    public ContentKey insertHistorical(String channelName, Content content) throws Exception {
        return insert(channelName, content);
    }

    public void delete(String channel) {
        Traces traces = ActiveTraces.getLocal();
        new Thread(() -> {
            try {
                ContentKey limitKey = new ContentKey(TimeUtil.now(), "ZZZZZZ");
                ActiveTraces.start("S3SingleContentDao.delete", traces, limitKey);
                deleteBefore(channel, limitKey);
            } finally {
                ActiveTraces.end();
            }
        }).start();
    }

    static ObjectMetadata createObjectMetadata(Content content, boolean useEncrypted) {
        ObjectMetadata metadata = new ObjectMetadata();
        if (content.getContentType().isPresent()) {
            metadata.setContentType(content.getContentType().get());
            metadata.addUserMetadata("type", content.getContentType().get());
        } else {
            metadata.addUserMetadata("type", "none");
        }
        if (useEncrypted) {
            metadata.setSSEAlgorithm(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION);
        }
        return metadata;
    }
}
