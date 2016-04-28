package com.flightstats.hub.dao.aws;

import com.amazonaws.services.s3.AmazonS3;
import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.metrics.NoOpMetricsSender;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.ContentPath;
import com.flightstats.hub.test.Integration;
import com.google.inject.Injector;
import org.apache.commons.lang3.RandomStringUtils;
import org.joda.time.DateTime;
import org.junit.BeforeClass;
import org.junit.Test;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;

public class S3VerifierTest {

    private static S3SingleContentDao s3SingleContentDao = null;
    private static Injector injector = null;

    @BeforeClass
    public static void setUpClass() throws Exception {
        injector = Integration.startAwsHub();
        ChannelService cs1 = injector.getInstance(ChannelService.class);

        AwsConnectorFactory factory = new AwsConnectorFactory();
        AmazonS3 s3Client = factory.getS3Client();
        S3BucketName bucketName = new S3BucketName("local", "hub-v2");
        s3SingleContentDao = new S3SingleContentDao(s3Client, bucketName, new NoOpMetricsSender());
    }

    @Test
    public void testZookeeperReplicationNoDate() {
        final boolean[] zkDateVerified = {false};
        LastContentPath lcp = HubProvider.getInstance(LastContentPath.class);

        String channelName = "testNewChanelNoZKRepTime_" + RandomStringUtils.randomAlphabetic(20);
        ChannelService cs = HubProvider.getInstance(ChannelService.class);
        boolean channelExists = cs.channelExists(channelName);
        if (!channelExists) {
            ChannelConfig channelConfig = ChannelConfig.builder().withName(channelName).build();
            cs.createChannel(channelConfig);
        }

        ContentKey key = new ContentKey();
        Content content = Content.builder()
                .withContentKey(key)
                .withContentLanguage("lang")
                .withContentType("text/plain")
                .withData(key.toString().getBytes())
                .build();
        s3SingleContentDao.write(channelName, content);

        S3Verifier s3Verifier = new S3Verifier();
        injector.injectMembers(s3Verifier);
        s3Verifier.runSingle(new S3TestHandler(new S3TestValidator() {
            @Override
            void validateResults(Object results) {
                ChannelConfig config = (ChannelConfig) results;
                if (config.getName().equals(channelName)) {
                    // this is our test channel grab the time from ZK and verify it's there and not too old
                    ContentPath cp = lcp.getOrNull(channelName, S3Verifier.CHANNEL_LATEST_VERIFIED);
                    assertNotNull(cp);
                    zkDateVerified[0] = true;
                }
            }
        }));

        if (!zkDateVerified[0]) {
            fail("Replication date for channel " + channelName + " not found.");
        }
    }

    private class S3TestHandler extends S3VerifierHandler {
        S3TestValidator validator = null;

        public S3TestHandler(S3TestValidator validator) {
            this.validator = validator;
        }

        @Override
        void singleS3Verification(DateTime startTime, ChannelConfig channel, DateTime endTime) {
            if (validator != null) {
                validator.validateResults(channel);
            }
        }
    }

    abstract class S3TestValidator {
        abstract void validateResults(Object results);
    }

}