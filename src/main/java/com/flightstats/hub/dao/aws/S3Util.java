package com.flightstats.hub.dao.aws;

import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.MultiObjectDeleteException;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.dao.ContentKeyUtil;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.ContentPath;
import com.flightstats.hub.model.DirectionQuery;
import com.flightstats.hub.util.TimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

@Slf4j
class S3Util {

    SortedSet<ContentKey> queryPrevious(DirectionQuery query, ContentDao dao) {
        DateTime endTime = query.getStartKey().getTime();
        DateTime queryTime = endTime;
        SortedSet<ContentKey> keys = new TreeSet<>();
        DateTime earliestTime = query.getEarliestTime().minusHours(1);
        while (keys.size() < query.getCount() && queryTime.isAfter(earliestTime)) {
            TimeUtil.Unit unit = TimeUtil.Unit.HOURS;
            Duration duration = new Duration(queryTime, endTime);
            if (duration.getStandardDays() >= 2) {
                earliestTime = query.getEarliestTime().minusDays(1);
                unit = TimeUtil.Unit.DAYS;
            }
            if (duration.getStandardDays() >= 31) {
                unit = TimeUtil.Unit.MONTHS;
            }
            SortedSet<ContentKey> contentKeys = dao.queryByTime(query.convert(queryTime, unit));
            contentKeys.addAll(keys);
            keys = ContentKeyUtil.filter(contentKeys, query);
            queryTime = queryTime.minus(unit.getDuration());
        }

        ActiveTraces.getLocal().add("queryPrevious returning", keys);
        return keys;
    }

    public void delete(String channelPath, ContentKey limitKey, String s3BucketName, HubS3Client s3Client) {
        //noinspection StatementWithEmptyBody
        while (internalDelete(channelPath, limitKey, s3BucketName, s3Client)) {
        }
        internalDelete(channelPath, limitKey, s3BucketName, s3Client);
    }

    private boolean internalDelete(String channelPath, ContentKey limitKey, String s3BucketName, HubS3Client s3Client) {
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
            log.info("deleting more from " + channelPath + " deleted " + keys.size());
            ActiveTraces.getLocal().add("S3Util.internalDelete", channelPath, keys.size());
        } catch (MultiObjectDeleteException e) {
            log.info("what happened? " + channelPath, e);
            return true;
        }
        return listing.isTruncated();
    }
}
