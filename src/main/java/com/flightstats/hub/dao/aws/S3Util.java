package com.flightstats.hub.dao.aws;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.dao.ContentKeyUtil;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.ContentPath;
import com.flightstats.hub.model.DirectionQuery;
import com.flightstats.hub.util.TimeUtil;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class S3Util {

    private final static Logger logger = LoggerFactory.getLogger(S3Util.class);

    public static void initialize(String s3BucketName, AmazonS3 s3Client) {
        logger.info("checking if bucket exists " + s3BucketName);
        if (s3Client.doesBucketExist(s3BucketName)) {
            logger.info("bucket exists " + s3BucketName);
            return;
        }
        logger.error("EXITING! unable to find bucket " + s3BucketName);
        throw new RuntimeException("unable to find bucket " + s3BucketName);
    }

    public static SortedSet<ContentKey> queryPrevious(DirectionQuery query, ContentDao dao) {
        DateTime startTime = query.getContentKey().getTime();
        SortedSet<ContentKey> keys = new TreeSet<>();
        int hourCount = 0;
        DateTime earliestTime = TimeUtil.getEarliestTime((int) query.getTtlDays()).minusDays(1);
        while (keys.size() < query.getCount() && hourCount < 3) {
            SortedSet<ContentKey> queryByTime = dao.queryByTime(query.convert(startTime, TimeUtil.Unit.HOURS));
            queryByTime.addAll(keys);
            keys = ContentKeyUtil.filter(queryByTime, query.getContentKey(), earliestTime, query.getCount(), false, query.isStable());
            startTime = startTime.minusHours(1);
            hourCount++;
        }

        while (keys.size() < query.getCount() && startTime.isAfter(earliestTime)) {
            SortedSet<ContentKey> queryByTime = dao.queryByTime(query.convert(startTime, TimeUtil.Unit.DAYS));
            queryByTime.addAll(keys);
            keys = ContentKeyUtil.filter(queryByTime, query.getContentKey(), earliestTime, query.getCount(), false, query.isStable());
            startTime = startTime.minusDays(1);
        }
        query.getTraces().add("queryPrevious returning", keys);
        return keys;
    }

    public static void delete(String channelPath, ContentKey limitKey, String s3BucketName, AmazonS3 s3Client) {
        //noinspection StatementWithEmptyBody
        while (internalDelete(channelPath, limitKey, s3BucketName, s3Client)) {
        }
        internalDelete(channelPath, limitKey, s3BucketName, s3Client);
    }

    private static boolean internalDelete(String channelPath, ContentKey limitKey, String s3BucketName, AmazonS3 s3Client) {
        ListObjectsRequest request = new ListObjectsRequest();
        request.withBucketName(s3BucketName);
        request.withPrefix(channelPath);
        ObjectListing listing = s3Client.listObjects(request);
        List<DeleteObjectsRequest.KeyVersion> keys = new ArrayList<>();
        for (S3ObjectSummary objectSummary : listing.getObjectSummaries()) {
            ContentPath contentKey = ContentPath.fromUrl(StringUtils.substringAfter(objectSummary.getKey(), channelPath)).get();
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
