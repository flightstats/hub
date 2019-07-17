package com.flightstats.hub.dao.aws;

import com.amazonaws.services.s3.S3ResponseMetadata;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;
import com.flightstats.hub.config.properties.AppProperties;
import com.flightstats.hub.config.properties.S3Properties;
import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.metrics.StatsdReporter;
import com.flightstats.hub.metrics.Traces;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.DirectionQuery;
import com.flightstats.hub.model.TimeQuery;
import com.flightstats.hub.util.ChunkOutputStream;
import com.flightstats.hub.util.TimeUtil;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;

@SuppressWarnings("Duplicates")
@Singleton
@Slf4j
public class S3LargeContentDao implements ContentDao {

    private final HubS3Client s3Client;
    private final StatsdReporter statsdReporter;
    private final AppProperties appProperties;
    private final String bucketName;
    private final int maxChunkInMB;
    private final S3Util s3Util;

    @Inject
    public S3LargeContentDao(HubS3Client s3Client,
                             StatsdReporter statsdReporter,
                             AppProperties appPropertiesIn,
                             S3Properties s3Properties,
                             S3Util s3Util) {
        this.statsdReporter = statsdReporter;
        this.s3Client = s3Client;
        this.appProperties = appPropertiesIn;
        this.bucketName = s3Properties.getBucketName();
        this.maxChunkInMB = s3Properties.getMaxChunkInMB();
        this.s3Util = s3Util;

    }

    public void initialize() {
        s3Client.initialize();
    }

    @Override
    public Optional<ContentKey> getLatest(String channel, ContentKey limitKey, Traces traces) {
        throw new UnsupportedOperationException("use query interface");
    }

    public ContentKey insert(String channelName, Content content) {
        content.keyAndStart(TimeUtil.now());
        ContentKey key = content.getContentKey().get();
        ActiveTraces.getLocal().add("S3LargeContentDao.write ", key);
        long start = System.currentTimeMillis();
        int length = 0;
        List<PartETag> partETags = Collections.synchronizedList(new ArrayList<>());
        String s3Key = getS3ContentKey(channelName, key, content.isHistorical());
        String uploadId = "";
        boolean completed = false;
        try {
            ObjectMetadata metadata = S3SingleContentDao.createObjectMetadata(content, appProperties.isAppEncrypted());
            InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest(bucketName, s3Key, metadata);
            InitiateMultipartUploadResult initResponse = s3Client.initiateMultipartUpload(initRequest);
            uploadId = initResponse.getUploadId();
            ChunkOutputStream outputStream = new ChunkOutputStream(content.getThreads(), maxChunkInMB, chunk -> {
                try {
                    byte[] bytes = chunk.getBytes();
                    log.info("got bytes {} {}", s3Key, bytes.length);
                    ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                    UploadPartRequest uploadRequest = new UploadPartRequest()
                            .withBucketName(bucketName)
                            .withKey(s3Key)
                            .withUploadId(initResponse.getUploadId())
                            .withPartNumber(chunk.getCount())
                            .withInputStream(bais)
                            .withPartSize(bytes.length);
                    UploadPartResult uploadPart = s3Client.uploadPart(uploadRequest);
                    partETags.add(uploadPart.getPartETag());
                    log.info("wrote chunk {} {} {}", s3Key, chunk.getCount(), bytes.length);
                    return "ok";
                } catch (Exception e) {
                    log.warn("what happened POST to " + channelName + " for chunk " + chunk.getCount(), e);
                    throw e;
                }
            });

            InputStream stream = content.getStream();
            long copied = IOUtils.copyLarge(stream, outputStream);
            ActiveTraces.getLocal().add("S3LargeContentDao.write processed", copied);
            log.info("before complete key {} with {} parts", s3Key, partETags.size());
            outputStream.close();
            CompleteMultipartUploadRequest compRequest = new CompleteMultipartUploadRequest(bucketName, s3Key, uploadId, partETags);
            s3Client.completeMultipartUpload(compRequest);
            S3ResponseMetadata completedMetaData = s3Client.getCachedResponseMetadata(compRequest);
            log.info("completed key {} request id {} with {} parts", s3Key, completedMetaData.getRequestId(), partETags.size());
            completed = true;
            content.setSize(copied);
            long s3Length = getLength(s3Key, bucketName);
            if (s3Length != copied) {
                String message = "object is not the correct size. expected " + copied + ", found " + s3Length;
                log.warn(message);
                throw new RuntimeException(message);
            }
            return key;
        } catch (Exception e) {
            log.warn("unable to write large item to S3 " + channelName + " " + key, e);
            ActiveTraces.getLocal().add("S3LargeContentDao.error ", e.getMessage());
            if (StringUtils.isNotBlank(uploadId)) {
                if (completed) {
                    log.warn("deleting multipart " + channelName + " " + key, e);
                    delete(channelName, key);
                } else {
                    log.warn("aborting multipart " + channelName + " " + key, e);
                    AbortMultipartUploadRequest request = new AbortMultipartUploadRequest(bucketName, s3Key, uploadId);
                    s3Client.abortMultipartUpload(request);
                }
            }
            throw new RuntimeException(e);
        } finally {
            statsdReporter.time(channelName, "s3.put", start, length, "type:large");
        }
    }

    private long getLength(String s3Key, String name) {
        GetObjectRequest request = new GetObjectRequest(name, s3Key);
        try (S3Object object = s3Client.getObject(request)) {
            ObjectMetadata metadata = object.getObjectMetadata();
            long contentLength = metadata.getContentLength();
            S3ResponseMetadata responseMetadata = s3Client.getCachedResponseMetadata(request);
            log.info("{} {} get content length {} {}", name, s3Key, contentLength, responseMetadata.getRequestId());
            ActiveTraces.getLocal().add("S3LargeContentDao.write completed length ", contentLength);
            return contentLength;
        } catch (Exception e) {
            log.warn("unable to get length" + name + " " + s3Key, e);
            return 0;
        }
    }

    @Override
    public void delete(String channelName, ContentKey key) {
        String s3ContentKey = getS3ContentKey(channelName, key, false);
        DeleteObjectRequest request = new DeleteObjectRequest(bucketName, s3ContentKey);
        s3Client.deleteObject(request);
        ActiveTraces.getLocal().add("S3largeContentDao.deleted", s3ContentKey);
    }

    public Content get(final String channelName, final ContentKey key) {
        ActiveTraces.getLocal().add("S3LargeContentDao.read", key);
        try {
            return getS3Object(channelName, key);
        } catch (SocketTimeoutException e) {
            log.warn("SocketTimeoutException : unable to read " + channelName + " " + key);
            try {
                return getS3Object(channelName, key);
            } catch (Exception e2) {
                log.warn("unable to read second time " + channelName + " " + key + " " + e.getMessage(), e2);
                return null;
            }
        } catch (Exception e) {
            log.warn("unable to read " + channelName + " " + key, e);
            return null;
        } finally {
            ActiveTraces.getLocal().add("S3LargeContentDao.read completed");
        }
    }

    private Content getS3Object(String channelName, ContentKey key) throws IOException {
        long start = System.currentTimeMillis();
        try {
            GetObjectRequest request = new GetObjectRequest(bucketName, getS3ContentKey(channelName, key, false));
            try (S3Object object = s3Client.getObject(request)) {
                ObjectMetadata metadata = object.getObjectMetadata();
                Map<String, String> userData = metadata.getUserMetadata();
                Content.Builder builder = Content.builder();
                String type = userData.get("type");
                if (!type.equals("none")) {
                    builder.withContentType(type);
                }
                builder.withContentKey(key);
                builder.withStream(object.getObjectContent());
                builder.withLarge(true);
                return builder.build();
            }

        } catch (AmazonS3Exception e) {
            if (e.getStatusCode() != 404) {
                log.warn("AmazonS3Exception : unable to read " + channelName + " " + key, e);
            }
            return null;
        } finally {
            statsdReporter.time(channelName, "s3.get", start, "type:single");
        }
    }

    @Override
    public SortedSet<ContentKey> queryByTime(TimeQuery query) {
        throw new UnsupportedOperationException("the large dao only deals with large objects, queries are tracked using the small dao");
    }

    @Override
    public SortedSet<ContentKey> query(DirectionQuery query) {
        throw new UnsupportedOperationException("the large dao only deals with large objects, queries are tracked using the small dao");
    }

    private String getS3ContentKey(String channelName, ContentKey key, boolean isHistorical) {
        return isHistorical ?
                channelName + "/" + key.toUrl()
                : channelName + "/large/" + key.toUrl();
    }

    @Override
    public void deleteBefore(String channel, ContentKey limitKey) {
        try {
            s3Util.delete(channel + "/large/", limitKey, bucketName, s3Client);
            log.info("completed deletion of " + channel);
        } catch (Exception e) {
            log.warn("unable to delete  {} in {}", channel, bucketName, e);
        }
    }

    @Override
    public ContentKey insertHistorical(String channelName, Content content) {
        return insert(channelName, content);
    }

    public void delete(String channel) {
        Traces traces = ActiveTraces.getLocal();
        new Thread(() -> {
            try {
                ContentKey limitKey = new ContentKey(TimeUtil.now(), "ZZZZZZ");
                ActiveTraces.start("S3LargeContentDao.delete", traces, limitKey);
                deleteBefore(channel, limitKey);
            } finally {
                ActiveTraces.end();
            }
        }).start();
    }

}