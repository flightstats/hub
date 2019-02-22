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
import com.flightstats.hub.app.HubProperties;
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
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.ConstructorProperties;
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
public class S3LargeContentDao implements ContentDao {

    private final static Logger logger = LoggerFactory.getLogger(S3LargeContentDao.class);

    private final boolean useEncrypted = HubProperties.isAppEncrypted();

    @Inject
    private StatsdReporter statsdReporter;
    @Inject
    private HubS3Client s3Client;
    @Inject
    private S3BucketName s3BucketName;

    @java.beans.ConstructorProperties({"statsdReporter", "s3Client", "s3BucketName"})
    public S3LargeContentDao(StatsdReporter statsdReporter, HubS3Client s3Client, S3BucketName s3BucketName) {
        this.statsdReporter = statsdReporter;
        this.s3Client = s3Client;
        this.s3BucketName = s3BucketName;
    }

    public S3LargeContentDao() {
    }

    public static S3LargeContentDaoBuilder builder() {
        return new S3LargeContentDaoBuilder();
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
        String name = s3BucketName.getS3BucketName();
        String uploadId = "";
        boolean completed = false;
        try {
            ObjectMetadata metadata = S3SingleContentDao.createObjectMetadata(content, useEncrypted);
            InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest(name, s3Key, metadata);
            InitiateMultipartUploadResult initResponse = s3Client.initiateMultipartUpload(initRequest);
            uploadId = initResponse.getUploadId();
            ChunkOutputStream outputStream = new ChunkOutputStream(content.getThreads(), chunk -> {
                try {
                    byte[] bytes = chunk.getBytes();
                    logger.info("got bytes {} {}", s3Key, bytes.length);
                    ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                    UploadPartRequest uploadRequest = new UploadPartRequest()
                            .withBucketName(name)
                            .withKey(s3Key)
                            .withUploadId(initResponse.getUploadId())
                            .withPartNumber(chunk.getCount())
                            .withInputStream(bais)
                            .withPartSize(bytes.length);
                    UploadPartResult uploadPart = s3Client.uploadPart(uploadRequest);
                    partETags.add(uploadPart.getPartETag());
                    logger.info("wrote chunk {} {} {}", s3Key, chunk.getCount(), bytes.length);
                    return "ok";
                } catch (Exception e) {
                    logger.warn("what happened POST to " + channelName + " for chunk " + chunk.getCount(), e);
                    throw e;
                }
            });

            InputStream stream = content.getStream();
            long copied = IOUtils.copyLarge(stream, outputStream);
            ActiveTraces.getLocal().add("S3LargeContentDao.write processed", copied);
            logger.info("before complete key {} with {} parts", s3Key, partETags.size());
            outputStream.close();
            CompleteMultipartUploadRequest compRequest = new CompleteMultipartUploadRequest(name, s3Key, uploadId, partETags);
            s3Client.completeMultipartUpload(compRequest);
            S3ResponseMetadata completedMetaData = s3Client.getCachedResponseMetadata(compRequest);
            logger.info("completed key {} request id {} with {} parts", s3Key, completedMetaData.getRequestId(), partETags.size());
            completed = true;
            content.setSize(copied);
            long s3Length = getLength(s3Key, name);
            if (s3Length != copied) {
                String message = "object is not the correct size. expected " + copied + ", found " + s3Length;
                logger.warn(message);
                throw new RuntimeException(message);
            }
            return key;
        } catch (Exception e) {
            logger.warn("unable to write large item to S3 " + channelName + " " + key, e);
            ActiveTraces.getLocal().add("S3LargeContentDao.error ", e.getMessage());
            if (StringUtils.isNotBlank(uploadId)) {
                if (completed) {
                    logger.warn("deleting multipart " + channelName + " " + key, e);
                    delete(channelName, key);
                } else {
                    logger.warn("aborting multipart " + channelName + " " + key, e);
                    AbortMultipartUploadRequest request = new AbortMultipartUploadRequest(name, s3Key, uploadId);
                    s3Client.abortMultipartUpload(request);
                }
            }
            throw new RuntimeException(e);
        } finally {
            statsdReporter.time(channelName, "s3.put", start, length, "type:large");
        }
    }

    private long getLength(String s3Key, String name) throws IOException {
        GetObjectRequest request = new GetObjectRequest(name, s3Key);
        try (S3Object object = s3Client.getObject(request)) {
            ObjectMetadata metadata = object.getObjectMetadata();
            long contentLength = metadata.getContentLength();
            S3ResponseMetadata responseMetadata = s3Client.getCachedResponseMetadata(request);
            logger.info("{} {} get content length {} {}", name, s3Key, contentLength, responseMetadata.getRequestId());
            ActiveTraces.getLocal().add("S3LargeContentDao.write completed length ", contentLength);
            return contentLength;
        } catch (Exception e) {
            logger.warn("unable to get length" + name + " " + s3Key, e);
            return 0;
        }
    }

    @Override
    public void delete(String channelName, ContentKey key) {
        String s3ContentKey = getS3ContentKey(channelName, key, false);
        DeleteObjectRequest request = new DeleteObjectRequest(s3BucketName.getS3BucketName(), s3ContentKey);
        s3Client.deleteObject(request);
        ActiveTraces.getLocal().add("S3largeContentDao.deleted", s3ContentKey);
    }

    public Content get(final String channelName, final ContentKey key) {
        ActiveTraces.getLocal().add("S3LargeContentDao.read", key);
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
            ActiveTraces.getLocal().add("S3LargeContentDao.read completed");
        }
    }

    private Content getS3Object(String channelName, ContentKey key) throws IOException {
        long start = System.currentTimeMillis();
        try {
            GetObjectRequest request = new GetObjectRequest(s3BucketName.getS3BucketName(), getS3ContentKey(channelName, key, false));
            S3Object object = s3Client.getObject(request);
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
        } catch (AmazonS3Exception e) {
            if (e.getStatusCode() != 404) {
                logger.warn("AmazonS3Exception : unable to read " + channelName + " " + key, e);
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
            S3Util.delete(channel + "/large/", limitKey, s3BucketName.getS3BucketName(), s3Client);
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
                ActiveTraces.start("S3LargeContentDao.delete", traces, limitKey);
                deleteBefore(channel, limitKey);
            } finally {
                ActiveTraces.end();
            }
        }).start();
    }

    public static class S3LargeContentDaoBuilder {
        private StatsdReporter statsdReporter;
        private HubS3Client s3Client;
        private S3BucketName s3BucketName;

        S3LargeContentDaoBuilder() {
        }

        public S3LargeContentDao.S3LargeContentDaoBuilder statsdReporter(StatsdReporter statsdReporter) {
            this.statsdReporter = statsdReporter;
            return this;
        }

        public S3LargeContentDaoBuilder s3Client(HubS3Client s3Client) {
            this.s3Client = s3Client;
            return this;
        }

        public S3LargeContentDaoBuilder s3BucketName(S3BucketName s3BucketName) {
            this.s3BucketName = s3BucketName;
            return this;
        }

        public S3LargeContentDao build() {
            return new S3LargeContentDao(statsdReporter, s3Client, s3BucketName);
        }

        public String toString() {
            return "com.flightstats.hub.dao.aws.S3LargeContentDao.S3LargeContentDaoBuilder(statsdReporter=" + this.statsdReporter + ", s3Client=" + this.s3Client + ", s3BucketName=" + this.s3BucketName + ")";
        }
    }
}