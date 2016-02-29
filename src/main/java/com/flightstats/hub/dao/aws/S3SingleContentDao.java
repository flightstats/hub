package com.flightstats.hub.dao.aws;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.metrics.MetricsSender;
import com.flightstats.hub.metrics.Traces;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.DirectionQuery;
import com.flightstats.hub.model.TimeQuery;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.base.Optional;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.util.*;

public class S3SingleContentDao implements ContentDao {

    private final static Logger logger = LoggerFactory.getLogger(S3SingleContentDao.class);
    public static final int MAX_ITEMS = 1000 * 1000;

    private final AmazonS3 s3Client;
    private final MetricsSender sender;
    private final boolean useEncrypted;
    private final int s3MaxQueryItems;
    private final String s3BucketName;

    @Inject
    public S3SingleContentDao(AmazonS3 s3Client, S3BucketName s3BucketName, MetricsSender sender) {
        this.s3Client = s3Client;
        this.sender = sender;
        this.useEncrypted = HubProperties.getProperty("app.encrypted", false);
        this.s3MaxQueryItems = HubProperties.getProperty("s3.maxQueryItems", 1000);
        this.s3BucketName = s3BucketName.getS3BucketName();
    }

    public void initialize() {
        S3Util.initialize(s3BucketName, s3Client);
    }

    @Override
    public Optional<ContentKey> getLatest(String channel, ContentKey limitKey, Traces traces) {
        throw new UnsupportedOperationException("use query interface");
    }

    public ContentKey write(String channelName, Content content) {
        ContentKey key = content.getContentKey().get();
        ActiveTraces.getLocal().add("S3SingleContentDao.write", key);
        try {
            String s3Key = getS3ContentKey(channelName, key);
            InputStream stream = new ByteArrayInputStream(content.getData());
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(content.getData().length);
            if (content.getContentType().isPresent()) {
                metadata.setContentType(content.getContentType().get());
                metadata.addUserMetadata("type", content.getContentType().get());
            } else {
                metadata.addUserMetadata("type", "none");
            }
            if (content.getContentLanguage().isPresent()) {
                metadata.addUserMetadata("language", content.getContentLanguage().get());
            } else {
                metadata.addUserMetadata("language", "none");
            }
            if (useEncrypted) {
                metadata.setSSEAlgorithm(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION);
            }
            PutObjectRequest request = new PutObjectRequest(s3BucketName, s3Key, stream, metadata);
            sender.send("channel." + channelName + ".s3.put", 1);
            sender.send("channel." + channelName + ".s3.bytes", content.getData().length);
            s3Client.putObject(request);
            return key;
        } catch (Exception e) {
            logger.warn("unable to write item to S3 " + channelName + " " + key, e);
            throw e;
        } finally {
            ActiveTraces.getLocal().add("S3SingleContentDao.write completed");
        }
    }

    public Content read(final String channelName, final ContentKey key) {
        ActiveTraces.getLocal().add("S3SingleContentDao.read", key);
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
        } finally {
            ActiveTraces.getLocal().add("S3SingleContentDao.read completed");
        }
    }

    private Content getS3Object(String channelName, ContentKey key) throws IOException {
        try (S3Object object = s3Client.getObject(s3BucketName, getS3ContentKey(channelName, key))) {
            sender.send("channel." + channelName + ".s3.get", 1);
            byte[] bytes = ByteStreams.toByteArray(object.getObjectContent());
            ObjectMetadata metadata = object.getObjectMetadata();
            Map<String, String> userData = metadata.getUserMetadata();
            Content.Builder builder = Content.builder();
            String type = userData.get("type");
            if (!type.equals("none")) {
                builder.withContentType(type);
            }
            String language = userData.get("language");
            if (!language.equals("none")) {
                builder.withContentLanguage(language);
            }
            builder.withContentKey(key);
            builder.withData(bytes);
            return builder.build();
        } catch (AmazonS3Exception e) {
            if (e.getStatusCode() != 404) {
                logger.warn("AmazonS3Exception : unable to read " + channelName + " " + key, e);
            }
            return null;
        }
    }

    @Override
    public SortedSet<ContentKey> queryByTime(TimeQuery query) {
        logger.debug("queryByTime {} ", query);
        Traces traces = ActiveTraces.getLocal();
        traces.add("S3SingleContentDao.queryByTime", query);
        String timePath = query.getUnit().format(query.getStartTime());
        ListObjectsRequest request = new ListObjectsRequest()
                .withBucketName(s3BucketName)
                .withMaxKeys(s3MaxQueryItems);
        DateTime endTime = query.getEndTime();
        if (endTime == null) {
            request.withPrefix(query.getChannelName() + "/" + timePath);
            endTime = query.getStartTime().plus(query.getUnit().getDuration());
        } else {
            request.withMarker(query.getChannelName() + "/" + timePath);
        }
        SortedSet<ContentKey> keys = iterateListObjects(query.getChannelName(), request, MAX_ITEMS, endTime, query.getCount(), query.getLimitKey());
        traces.add("S3SingleContentDao.queryByTime completed", keys);
        return keys;
    }

    private SortedSet<ContentKey> iterateListObjects(String channelName, ListObjectsRequest request,
                                                     int maxItems, DateTime endTime, int count, ContentKey limitKey) {
        Traces traces = ActiveTraces.getLocal();
        SortedSet<ContentKey> keys = new TreeSet<>();
        if (count > 0 && limitKey != null) {
            keys = new ContentKeySet(count, limitKey);
        }
        sender.send("channel." + channelName + ".s3.list", 1);
        logger.trace("list {} {} {}", channelName, request.getPrefix(), request.getMarker());
        traces.add("S3SingleContentDao.iterateListObjects prefix:", request.getPrefix(), request.getMarker());
        ObjectListing listing = s3Client.listObjects(request);
        ContentKey marker = addKeys(channelName, listing, keys, endTime);
        while (shouldContinue(maxItems, endTime, keys, listing, marker)) {
            request.withMarker(channelName + "/" + marker.toUrl());
            sender.send("channel." + channelName + ".s3.list", 1);
            logger.trace("list {} {}", channelName, request.getMarker());
            traces.add("S3SingleContentDao.iterateListObjects marker:", request.getMarker());
            listing = s3Client.listObjects(request);
            marker = addKeys(channelName, listing, keys, endTime);
        }
        return keys;
    }

    private boolean shouldContinue(int maxItems, DateTime endTime, SortedSet<ContentKey> keys, ObjectListing listing, ContentKey marker) {
        if (marker == null) {
            return false;
        }
        return listing.isTruncated()
                && keys.size() < maxItems
                && marker.getTime().isBefore(endTime);
    }

    private ContentKey addKeys(String channelName, ObjectListing listing, Set<ContentKey> keys, DateTime endTime) {
        Optional<ContentKey> contentKey = Optional.absent();
        List<S3ObjectSummary> summaries = listing.getObjectSummaries();
        for (S3ObjectSummary summary : summaries) {
            contentKey = ContentKey.fromUrl(StringUtils.substringAfter(summary.getKey(), channelName + "/"));
            if (contentKey.isPresent()) {
                if (contentKey.get().getTime().isBefore(endTime)) {
                    keys.add(contentKey.get());
                } else {
                    return contentKey.get();
                }
            }
        }
        return contentKey.orNull();
    }

    @Override
    public SortedSet<ContentKey> query(DirectionQuery query) {
        logger.trace("query {}", query);
        Traces traces = ActiveTraces.getLocal();
        traces.add("S3SingleContentDao.query", query);
        SortedSet<ContentKey> contentKeys = Collections.emptySortedSet();
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
                .withBucketName(s3BucketName)
                .withPrefix(query.getChannelName() + "/")
                .withMarker(query.getChannelName() + "/" + query.getContentKey().toUrl())
                .withMaxKeys(query.getCount());
        return iterateListObjects(query.getChannelName(), request, query.getCount(), TimeUtil.now(), query.getCount(), null);
    }

    private String getS3ContentKey(String channelName, ContentKey key) {
        return channelName + "/" + key.toUrl();
    }

    @Override
    public void deleteBefore(String channel, ContentKey limitKey) {
        try {
            S3Util.delete(channel + "/", limitKey, s3BucketName, s3Client);
            logger.info("completed deletion of " + channel);
        } catch (Exception e) {
            logger.warn("unable to delete " + channel + " in " + s3BucketName, e);
        }
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

}
