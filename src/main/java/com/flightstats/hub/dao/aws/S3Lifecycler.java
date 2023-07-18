package com.flightstats.hub.dao.aws;

import com.flightstats.hub.cluster.DistributedAsyncLockRunner;
import com.flightstats.hub.config.properties.S3Properties;
import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.model.ChannelConfig;
import com.google.inject.Inject;
import com.google.inject.name.Named;

public class S3Lifecycler {

    private final S3MaintenanceManager mainLifecycleManager;
    private final S3MaintenanceManager drLifecycleManager;

    @Inject
    public S3Lifecycler(@Named("MAIN") HubS3Client s3Client,
                        @Named("DISASTER_RECOVERY") HubS3Client drS3Client,
                        DistributedAsyncLockRunner asyncLockRunner,
                        @Named("ChannelConfig") Dao<ChannelConfig> channelConfigDao,
                        MaxItemsEnforcer maxItemsEnforcer,
                        S3Properties s3Properties) {
        this.mainLifecycleManager = new S3MaintenanceManager(s3Client,   asyncLockRunner, channelConfigDao, maxItemsEnforcer, s3Properties.getBucketName(),                 s3Properties.getBucketPolicyMaxRules(S3MaintenanceManager.S3_LIFECYCLE_RULES_AVAILABLE), s3Properties.isConfigManagementEnabled());
        this.drLifecycleManager   = new S3MaintenanceManager(drS3Client, asyncLockRunner, channelConfigDao, maxItemsEnforcer, s3Properties.getDisasterRecoveryBucketName(), s3Properties.getBucketPolicyMaxRules(S3MaintenanceManager.S3_LIFECYCLE_RULES_AVAILABLE), s3Properties.isConfigManagementEnabled());
    }

    public S3MaintenanceManager getMainLifecycleManager() {
        return mainLifecycleManager;
    }

    public S3MaintenanceManager getDrLifecycleManager() {
        return drLifecycleManager;
    }
}
