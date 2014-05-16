package com.flightstats.hub.dao.encryption;

import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.Request;
import com.flightstats.hub.model.*;
import com.flightstats.hub.model.exception.ForbiddenRequestException;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class AuditChannelService implements ChannelService {
    private final static Logger logger = LoggerFactory.getLogger(AuditChannelService.class);
    public static final String AUDIT = "_audit";
    private final ChannelService channelService;

    @Inject
    public AuditChannelService(@BasicChannelService ChannelService channelService) {
        logger.info("got class " + channelService.getClass());
        this.channelService = channelService;
    }

    @Override
    public boolean channelExists(String channelName) {
        return channelService.channelExists(channelName);
    }

    @Override
    public ChannelConfiguration createChannel(ChannelConfiguration configuration) {
        if (isAuditChannel(configuration.getName())) {
            throw new ForbiddenRequestException("Audit Channels can not be created");
        }

        Set<String> tags = new HashSet<>(configuration.getTags());
        tags.add("audit");
        ChannelConfiguration channel = channelService.createChannel(configuration);

        ChannelConfiguration audit = ChannelConfiguration.builder()
                .withChannelConfiguration(configuration)
                .withName(configuration.getName() + AUDIT)
                .withDescription("auditing channel for " + configuration.getName())
                .withTags(tags)
                .build();

        channelService.createChannel(audit);

        return channel;
    }

    @Override
    public InsertedContentKey insert(String channelName, Content content) {
        if (isAuditChannel(channelName)) {
            throw new ForbiddenRequestException("Audit Channels do not allow inserts");
        }
        return channelService.insert(channelName, content);
    }

    @Override
    public Optional<LinkedContent> getValue(Request request) {
        Optional<LinkedContent> optional = channelService.getValue(request);
        if (optional.isPresent() && !isAuditChannel(request.getChannel())) {
            Audit audit = Audit.builder()
                    .user(request.getUser())
                    .uri(request.getUri())
                    .build();
            Content content = Content.builder()
                    .withContentType(MediaType.APPLICATION_JSON)
                    .withData(audit.toJson().getBytes())
                    .build();
            //todo - gfm - 5/15/14 - make this async
            channelService.insert(request.getChannel() + AUDIT, content);
        }
        return optional;
    }

    @Override
    public ChannelConfiguration getChannelConfiguration(String channelName) {
        return channelService.getChannelConfiguration(channelName);
    }

    @Override
    public Iterable<ChannelConfiguration> getChannels() {
        return channelService.getChannels();
    }

    @Override
    public Iterable<ChannelConfiguration> getChannels(String tag) {
        return channelService.getChannels(tag);
    }

    @Override
    public Iterable<String> getTags() {
        return channelService.getTags();
    }

    @Override
    public Optional<ContentKey> findLastUpdatedKey(String channelName) {
        return channelService.findLastUpdatedKey(channelName);
    }

    @Override
    public boolean isHealthy() {
        return channelService.isHealthy();
    }

    @Override
    public ChannelConfiguration updateChannel(ChannelConfiguration configuration) {
        if (isAuditChannel(configuration.getName())) {
            configuration.getTags().add("audit");
        }
        return channelService.updateChannel(configuration);
    }

    @Override
    public Collection<ContentKey> getKeys(String channelName, DateTime dateTime) {
        return channelService.getKeys(channelName, dateTime);
    }

    @Override
    public boolean delete(String channelName) {
        //todo - gfm - 5/15/14 - prevent deletion of _audit ?
        return channelService.delete(channelName);
    }

    public static boolean isAuditChannel(String name) {
        return StringUtils.endsWith(name, AUDIT);
    }
}
