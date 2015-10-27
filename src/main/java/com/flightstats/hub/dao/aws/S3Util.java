package com.flightstats.hub.dao.aws;

import com.amazonaws.services.s3.AmazonS3;
import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.dao.ContentKeyUtil;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.DirectionQuery;
import com.flightstats.hub.util.TimeUtil;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            SortedSet<ContentKey> queryByTime = dao.queryByTime(query.getChannelName(), startTime, TimeUtil.Unit.HOURS, query.getTraces());
            queryByTime.addAll(keys);
            keys = ContentKeyUtil.filter(queryByTime, query.getContentKey(), earliestTime, query.getCount(), false, query.isStable());
            startTime = startTime.minusHours(1);
            hourCount++;
        }

        while (keys.size() < query.getCount() && startTime.isAfter(earliestTime)) {
            SortedSet<ContentKey> queryByTime = dao.queryByTime(query.getChannelName(), startTime, TimeUtil.Unit.DAYS, query.getTraces());
            queryByTime.addAll(keys);
            keys = ContentKeyUtil.filter(queryByTime, query.getContentKey(), earliestTime, query.getCount(), false, query.isStable());
            startTime = startTime.minusDays(1);
        }
        query.getTraces().add("queryPrevious returning", keys);
        return keys;
    }
}
