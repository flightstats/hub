package com.flightstats.hub.dao.aws;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.dao.ContentKeyUtil;
import com.flightstats.hub.metrics.MetricsSender;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.DirectionQuery;
import com.flightstats.hub.model.Traces;
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

public class S3ContentDao implements ContentDao {

    private final static Logger logger = LoggerFactory.getLogger(S3ContentDao.class);
    private static final int MAX_ITEMS = 1000 * 1000;

    private final AmazonS3 s3Client;
    private final MetricsSender sender;
    private final boolean useEncrypted;
    private final int s3MaxQueryItems;
    private final String s3BucketName;

    @Inject
    public S3ContentDao(AmazonS3 s3Client, S3BucketName s3BucketName, MetricsSender sender) {
        this.s3Client = s3Client;
        this.sender = sender;
        this.useEncrypted = HubProperties.getProperty("app.encrypted", false);
        this.s3MaxQueryItems = HubProperties.getProperty("s3.maxQueryItems", 1000);
        this.s3BucketName = s3BucketName.getS3BucketName();
    }

    public void initialize() {
        logger.info("checking if bucket exists " + s3BucketName);
        if (s3Client.doesBucketExist(s3BucketName)) {
            logger.info("bucket exists " + s3BucketName);
            return;
        }
        logger.error("EXITING! unable to find bucket " + s3BucketName);
        throw new RuntimeException("unable to find bucket " + s3BucketName);
    }

    @Override
    public Optional<ContentKey> getLatest(String channel, ContentKey limitKey, Traces traces) {
        throw new UnsupportedOperationException("use query interface");
    }

    @Override
    public void deleteBefore(String channelName, ContentKey limitKey) {
        callInternalDelete(channelName, limitKey);
    }

    public ContentKey write(String channelName, Content content) {
        long start = System.currentTimeMillis();
        ContentKey key = content.getContentKey().get();
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
        long time = System.currentTimeMillis() - start;
        sender.send("channel." + channelName + ".s3", time);
        sender.send("channel.ALL.s3", time);
        sender.send("channel." + channelName + ".s3.requestA", 1);
        s3Client.putObject(request);
        return key;
    }

    public Content read(final String channelName, final ContentKey key) {
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
            sender.send("channel." + channelName + ".s3.requestB", 1);
            S3Object object = s3Client.getObject(s3BucketName, getS3ContentKey(channelName, key));
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
            if (userData.containsKey("user")) {
                builder.withUser(userData.get("user"));
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
    public SortedSet<ContentKey> queryByTime(String channelName, DateTime startTime, TimeUtil.Unit unit, Traces traces) {
        traces.add("s3 query by time", channelName, startTime, unit);
        String timePath = unit.format(startTime);
        ListObjectsRequest request = new ListObjectsRequest()
                .withBucketName(s3BucketName)
                .withPrefix(channelName + "/" + timePath)
                .withMaxKeys(s3MaxQueryItems);
        SortedSet<ContentKey> keys = iterateListObjects(channelName, request, MAX_ITEMS);
        traces.add("s3 returning ", keys);
        return keys;
    }

    private SortedSet<ContentKey> iterateListObjects(String channelName, ListObjectsRequest request, int maxItems) {
        SortedSet<ContentKey> keys = new TreeSet<>();
        sender.send("channel." + channelName + ".s3.requestA", 1);
        ObjectListing listing = s3Client.listObjects(request);
        String marker = addKeys(channelName, listing, keys);
        while (listing.isTruncated() && keys.size() < maxItems) {
            request.withMarker(marker);
            sender.send("channel." + channelName + ".s3.requestA", 1);
            listing = s3Client.listObjects(request);
            marker = addKeys(channelName, listing, keys);
        }
        return keys;
    }

    private String addKeys(String channelName, ObjectListing listing, Set<ContentKey> keys) {
        String key = null;
        List<S3ObjectSummary> summaries = listing.getObjectSummaries();
        for (S3ObjectSummary summary : summaries) {
            key = summary.getKey();
            Optional<ContentKey> contentKey = ContentKey.fromUrl(StringUtils.substringAfter(key, channelName + "/"));
            if (contentKey.isPresent()) {
                keys.add(contentKey.get());
            }
        }
        return key;
    }

    @Override
    public SortedSet<ContentKey> query(DirectionQuery query) {
        logger.trace("query {}", query);
        if (query.isNext()) {
            return next(query);
        } else {
            return previous(query);
        }
    }

    private SortedSet<ContentKey> previous(DirectionQuery query) {
        DateTime startTime = query.getContentKey().getTime();
        SortedSet<ContentKey> orderedKeys = new TreeSet<>();
        int hourCount = 0;
        DateTime limitTime = TimeUtil.getEarliestTime((int) query.getTtlDays()).minusDays(1);
        while (orderedKeys.size() < query.getCount() && hourCount < 6) {
            SortedSet<ContentKey> queryByTime = queryByTime(query.getChannelName(), startTime, TimeUtil.Unit.HOURS, query.getTraces());
            queryByTime.addAll(orderedKeys);
            orderedKeys = ContentKeyUtil.filter(queryByTime, query.getContentKey(), limitTime, query.getCount(), false, query.isStable());
            startTime = startTime.minusHours(1);
            hourCount++;
        }

        while (orderedKeys.size() < query.getCount() && startTime.isAfter(limitTime)) {
            SortedSet<ContentKey> queryByTime = queryByTime(query.getChannelName(), startTime, TimeUtil.Unit.DAYS, query.getTraces());
            queryByTime.addAll(orderedKeys);
            orderedKeys = ContentKeyUtil.filter(queryByTime, query.getContentKey(), limitTime, query.getCount(), false, query.isStable());
            startTime = startTime.minusDays(1);
        }
        query.getTraces().add("s3 previous returning", orderedKeys);
        return orderedKeys;
    }

    private SortedSet<ContentKey> next(DirectionQuery query) {
        query.getTraces().add("s3 next", query);
        ListObjectsRequest request = new ListObjectsRequest()
                .withBucketName(s3BucketName)
                .withPrefix(query.getChannelName() + "/")
                .withMarker(query.getChannelName() + "/" + query.getContentKey().toUrl())
                .withMaxKeys(query.getCount());
        SortedSet<ContentKey> keys = iterateListObjects(query.getChannelName(), request, query.getCount());
        query.getTraces().add("s3 next returning", keys);
        return keys;
    }

    private String getS3ContentKey(String channelName, ContentKey key) {
        return channelName + "/" + key.toUrl();
    }

    public void delete(String channel) {
        new Thread(() -> {
            try {
                ContentKey limitKey = new ContentKey(TimeUtil.now(), "ZZZZZZ");
                callInternalDelete(channel, limitKey);
                logger.info("completed deletion of " + channel);
            } catch (Exception e) {
                logger.warn("unable to delete " + channel + " in " + s3BucketName, e);
            }
        }).start();
    }

    private void callInternalDelete(String channel, ContentKey limitKey) {
        String channelPath = channel + "/";
        //noinspection StatementWithEmptyBody
        while (internalDelete(channel, channelPath, limitKey)) {
        }
        internalDelete(channel, channelPath, limitKey);
    }

    private boolean internalDelete(String channel, String channelPath, ContentKey limitKey) {
        ListObjectsRequest request = new ListObjectsRequest();
        request.withBucketName(s3BucketName);
        request.withPrefix(channelPath);
        sender.send("channel." + channel + ".s3.requestA", 1);
        ObjectListing listing = s3Client.listObjects(request);
        List<DeleteObjectsRequest.KeyVersion> keys = new ArrayList<>();
        for (S3ObjectSummary objectSummary : listing.getObjectSummaries()) {
            ContentKey contentKey = ContentKey.fromUrl(StringUtils.substringAfter(objectSummary.getKey(), channelPath)).get();
            if (contentKey.compareTo(limitKey) < 0) {
                keys.add(new DeleteObjectsRequest.KeyVersion(objectSummary.getKey()));
            }
        }
        if (keys.isEmpty()) {
            return false;
        }
        DeleteObjectsRequest multiObjectDeleteRequest = new DeleteObjectsRequest(s3BucketName);
        multiObjectDeleteRequest.setKeys(keys);
        try {
            s3Client.deleteObjects(multiObjectDeleteRequest);
            logger.info("deleting more from " + channelPath + " deleted " + keys.size());
        } catch (MultiObjectDeleteException e) {
            logger.info("what happened? " + channelPath, e);
            return true;
        }
        return listing.isTruncated();
    }
}
