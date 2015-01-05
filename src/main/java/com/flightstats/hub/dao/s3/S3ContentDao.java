package com.flightstats.hub.dao.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.DirectionQuery;
import com.flightstats.hub.model.Traces;
import com.flightstats.hub.spoke.PreviousUtil;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.base.Optional;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;

public class S3ContentDao implements ContentDao {

    private final static Logger logger = LoggerFactory.getLogger(S3ContentDao.class);

    private final AmazonS3 s3Client;
    private final boolean useEncrypted;
    private final int s3MaxQueryItems;
    private final String s3BucketName;

    @Inject
    public S3ContentDao(AmazonS3 s3Client,
                        @Named("app.encrypted") boolean useEncrypted,
                        S3BucketName s3BucketName,
                        @Named("s3.maxQueryItems") int s3MaxQueryItems) {
        this.s3Client = s3Client;
        this.useEncrypted = useEncrypted;
        this.s3MaxQueryItems = s3MaxQueryItems;
        this.s3BucketName = s3BucketName.getS3BucketName();
    }

    public void initialize() {
        logger.info("checking if bucket exists " + s3BucketName);
        if (s3Client.doesBucketExist(s3BucketName)) {
            logger.info("bucket exists " + s3BucketName);
            //todo - gfm - 11/28/14 - this should also verify read/write & query
            return;
        }
        logger.error("EXITING! unable to find bucket " + s3BucketName);
        throw new RuntimeException("unable to find bucket " + s3BucketName);
    }

    public ContentKey write(String channelName, Content content) {
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
        if (content.getUser().isPresent()) {
            metadata.addUserMetadata("user", content.getUser().get());
        }
        if (useEncrypted) {
            metadata.setSSEAlgorithm(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION);
        }
        PutObjectRequest request = new PutObjectRequest(s3BucketName, s3Key, stream, metadata);
        s3Client.putObject(request);
        return key;
    }

    public Content read(final String channelName, final ContentKey key) {
        try {
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
                logger.warn("unable to read " + channelName + " " + key, e);
            }
            return null;
        } catch (Exception e) {
            logger.warn("unable to read " + channelName + " " + key, e);
            return null;
        }
    }

    @Override
    public SortedSet<ContentKey> queryByTime(String channelName, DateTime startTime, TimeUtil.Unit unit, Traces traces) {
        traces.add("s3 query by time", channelName, startTime, unit);
        String timePath = unit.format(startTime);
        ListObjectsRequest request = new ListObjectsRequest();
        request.withBucketName(s3BucketName);
        request.withPrefix(channelName + "/" + timePath);
        request.withMaxKeys(s3MaxQueryItems);
        SortedSet<ContentKey> keys = new TreeSet<>();
        ObjectListing listing = s3Client.listObjects(request);
        String marker = addKeys(channelName, listing, keys);
        while (listing.isTruncated()) {
            request.withMarker(marker);
            listing = s3Client.listObjects(request);
            marker = addKeys(channelName, listing, keys);
        }
        traces.add("s3 returning ", keys);
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
        while (orderedKeys.size() < query.getCount() && hourCount < 6) {
            SortedSet<ContentKey> queryByTime = queryByTime(query.getChannelName(), startTime, TimeUtil.Unit.HOURS, query.getTraces());
            PreviousUtil.addToPrevious(query, queryByTime, orderedKeys);
            startTime = startTime.minusHours(1);
            hourCount++;
        }
        int dayCount = 0;
        while (orderedKeys.size() < query.getCount() && dayCount <= query.getTtlDays()) {
            SortedSet<ContentKey> queryByTime = queryByTime(query.getChannelName(), startTime, TimeUtil.Unit.DAYS, query.getTraces());
            PreviousUtil.addToPrevious(query, queryByTime, orderedKeys);
            startTime = startTime.minusDays(1);
            dayCount++;
        }
        query.getTraces().add("s3 previous returning", orderedKeys);
        return orderedKeys;
    }

    private SortedSet<ContentKey> next(DirectionQuery query) {
        query.getTraces().add("s3 next", query);
        SortedSet<ContentKey> keys = new TreeSet<>();
        ListObjectsRequest request = new ListObjectsRequest()
                .withBucketName(s3BucketName)
                .withPrefix(query.getChannelName() + "/")
                .withMarker(query.getChannelName() + "/" + query.getContentKey().toUrl())
                .withMaxKeys(query.getCount());
        ObjectListing listing = s3Client.listObjects(request);
        String marker = addKeys(query.getChannelName(), listing, keys);
        while (listing.isTruncated() && keys.size() < query.getCount()) {
            request.withMarker(marker);
            listing = s3Client.listObjects(request);
            marker = addKeys(query.getChannelName(), listing, keys);
        }
        query.getTraces().add("s3 next returning", keys);
        return keys;
    }

    private String getS3ContentKey(String channelName, ContentKey key) {
        return channelName + "/" + key.toUrl();
    }

    public void delete(String channel) {
        final String channelPath = channel + "/";
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //noinspection StatementWithEmptyBody
                    while (internalDelete(channelPath)) {
                    }
                    internalDelete(channelPath);
                    logger.info("completed deletion of " + channelPath);
                } catch (Exception e) {
                    logger.warn("unable to delete " + channelPath + " in " + s3BucketName, e);
                }
            }
        }).start();
    }

    private boolean internalDelete(String channelPath) {
        ListObjectsRequest request = new ListObjectsRequest();
        request.withBucketName(s3BucketName);
        request.withPrefix(channelPath);
        ObjectListing listing = s3Client.listObjects(request);
        List<DeleteObjectsRequest.KeyVersion> keys = new ArrayList<>();
        for (S3ObjectSummary objectSummary : listing.getObjectSummaries()) {
            keys.add(new DeleteObjectsRequest.KeyVersion(objectSummary.getKey()));
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
