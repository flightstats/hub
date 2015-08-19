package com.flightstats.hub.dao;

import com.flightstats.hub.channel.ChannelValidator;
import com.flightstats.hub.exception.ForbiddenRequestException;
import com.flightstats.hub.exception.InvalidRequestException;
import com.flightstats.hub.exception.NoSuchChannelException;
import com.flightstats.hub.metrics.MetricsSender;
import com.flightstats.hub.model.*;
import com.flightstats.hub.replication.Replicator;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class ChannelServiceImpl implements ChannelService {
    private final static Logger logger = LoggerFactory.getLogger(ChannelServiceImpl.class);

    private final ContentService contentService;
    private final ChannelConfigDao channelConfigDao;
    private final ChannelValidator channelValidator;
    private final Replicator replicator;
    private MetricsSender sender;

    @Inject
    public ChannelServiceImpl(ContentService contentService, ChannelConfigDao channelConfigDao,
                              ChannelValidator channelValidator, Replicator replicator,
                              MetricsSender sender) {
        this.contentService = contentService;
        this.channelConfigDao = channelConfigDao;
        this.channelValidator = channelValidator;
        this.replicator = replicator;
        this.sender = sender;
    }

    @Override
    public boolean channelExists(String channelName) {
        return channelConfigDao.channelExists(channelName);
    }

    @Override
    public ChannelConfig createChannel(ChannelConfig configuration) {
        logger.info("create channel {}", configuration);
        channelValidator.validate(configuration, true);
        configuration = ChannelConfig.builder().withChannelConfiguration(configuration).build();
        ChannelConfig created = channelConfigDao.createChannel(configuration);
        if (created.isReplicating()) {
            replicator.notifyWatchers();
        }
        return created;
    }

    @Override
    public ContentKey insert(String channelName, Content content) throws Exception {
        if (content.isNew() && isReplicating(channelName)) {
            throw new ForbiddenRequestException(channelName + " cannot modified while replicating");
        }
        long start = System.currentTimeMillis();
        ContentKey contentKey = contentService.insert(channelName, content);
        long time = System.currentTimeMillis() - start;
        sender.send("channel." + channelName + ".post", time);
        sender.send("channel." + channelName + ".post.bytes", content.getSize());
        sender.send("channel.ALL.post", time);
        return contentKey;
    }

    @Override
    public Collection<ContentKey> insert(String channelName, BatchContent batchContent) throws Exception {
        if (batchContent.isNew() && isReplicating(channelName)) {
            throw new ForbiddenRequestException(channelName + " cannot modified while replicating");
        }
        if (StringUtils.isEmpty(batchContent.getContentType())) {
            throw new InvalidRequestException("content type is required");
        }
        long start = System.currentTimeMillis();
        Collection<ContentKey> contentKeys = contentService.insert(channelName, batchContent);
        long time = System.currentTimeMillis() - start;
        sender.send("channel." + channelName + ".post", time);
        sender.send("channel." + channelName + ".post.bytes", batchContent.getSize());
        sender.send("channel.ALL.post", time);
        return contentKeys;
    }

    public boolean isReplicating(String channelName) {
        try {
            ChannelConfig configuration = getCachedChannelConfig(channelName);
            return configuration.isReplicating();
        } catch (NoSuchChannelException e) {
            return false;
        }
    }

    @Override
    public Optional<ContentKey> getLatest(String channel, boolean stable, boolean trace) {
        ChannelConfig channelConfig = getChannelConfig(channel);
        if (null == channelConfig) {
            return Optional.absent();
        }
        Traces traces = Traces.getTraces(trace);
        ContentKey limitKey = new ContentKey(TimeUtil.time(stable), "ZZZZZZ");
        Optional<ContentKey> latest = contentService.getLatest(channel, limitKey, traces);
        if (latest.isPresent()) {
            DateTime ttlTime = getTtlTime(channel);
            if (latest.get().getTime().isBefore(ttlTime)) {
                return Optional.absent();
            }
            traces.log(logger);
            return latest;
        }

        DirectionQuery query = DirectionQuery.builder()
                .channelName(channel)
                .contentKey(limitKey)
                .next(false)
                .stable(stable)
                .ttlDays(channelConfig.getTtlDays())
                .traces(traces)
                .count(1)
                .build();
        Collection<ContentKey> keys = getKeys(query);
        query.getTraces().log(logger);
        if (keys.isEmpty()) {
            return Optional.absent();
        } else {
            return Optional.of(keys.iterator().next());
        }
    }

    @Override
    public void deleteBefore(String name, ContentKey limitKey) {
        contentService.deleteBefore(name, limitKey);
    }

    @Override
    public Optional<Content> getValue(Request request) {
        DateTime ttlTime = getTtlTime(request.getChannel()).minusMinutes(15);
        if (request.getKey().getTime().isBefore(ttlTime)) {
            return Optional.absent();
        }
        return contentService.getValue(request.getChannel(), request.getKey());
    }

    @Override
    public ChannelConfig getChannelConfig(String channelName) {
        return channelConfigDao.getChannelConfig(channelName);
    }

    @Override
    public ChannelConfig getCachedChannelConfig(String channelName) {
        ChannelConfig channelConfig = channelConfigDao.getCachedChannelConfig(channelName);
        if (null == channelConfig) {
            throw new NoSuchChannelException(channelName);
        }
        return channelConfig;
    }

    @Override
    public Iterable<ChannelConfig> getChannels() {
        return channelConfigDao.getChannels();
    }

    @Override
    public Iterable<ChannelConfig> getChannels(String tag) {
        Collection<ChannelConfig> matchingChannels = new ArrayList<>();
        Iterable<ChannelConfig> channels = getChannels();
        for (ChannelConfig channel : channels) {
            if (channel.getTags().contains(tag)) {
                matchingChannels.add(channel);
            }
        }
        return matchingChannels;
    }

    @Override
    public Iterable<String> getTags() {
        Collection<String> matchingChannels = new HashSet<>();
        Iterable<ChannelConfig> channels = getChannels();
        for (ChannelConfig channel : channels) {
            matchingChannels.addAll(channel.getTags());
        }
        return matchingChannels;
    }

    @Override
    public ChannelConfig updateChannel(ChannelConfig configuration) {
        logger.info("updating channel {}", configuration);
        configuration = ChannelConfig.builder().withChannelConfiguration(configuration).build();
        ChannelConfig oldConfig = getChannelConfig(configuration.getName());
        channelValidator.validate(configuration, false);
        channelConfigDao.updateChannel(configuration);
        if (configuration.isReplicating()) {
            replicator.notifyWatchers();
        } else if (oldConfig != null && oldConfig.isReplicating()) {
            replicator.notifyWatchers();
        }
        return configuration;
    }

    @Override
    public Collection<ContentKey> queryByTime(TimeQuery query) {
        if (query == null) {
            return Collections.emptyList();
        }
        DateTime ttlTime = getTtlTime(query.getChannelName());
        DateTime stableTime = TimeUtil.time(query.isStable());
        return contentService.queryByTime(query)
                .stream()
                .filter(key -> key.getTime().isBefore(stableTime))
                .filter(key -> key.getTime().isAfter(ttlTime))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public Collection<ContentKey> getKeys(DirectionQuery query) {
        if (query.getCount() <= 0) {
            return Collections.emptySet();
        }
        DateTime ttlTime = getTtlTime(query.getChannelName());
        if (query.getContentKey().getTime().isBefore(ttlTime)) {
            query = query.withContentKey(new ContentKey(ttlTime, "0"));
        }
        query = query.withTtlDays(getTtlDays(query.getChannelName()));
        query.getTraces().add(query);
        List<ContentKey> keys = new ArrayList<>(contentService.getKeys(query));
        query.getTraces().add("keys", keys);
        return ContentKeyUtil.filter(keys, query.getContentKey(), ttlTime, query.getCount(), query.isNext(), query.isStable());
    }

    private DateTime getTtlTime(String channelName) {
        return TimeUtil.getEarliestTime(getTtlDays(channelName));
    }

    private int getTtlDays(String channelName) {
        return (int) getCachedChannelConfig(channelName).getTtlDays();
    }

    @Override
    public boolean delete(String channelName) {
        if (!channelConfigDao.channelExists(channelName)) {
            return false;
        }
        boolean replicating = isReplicating(channelName);
        contentService.delete(channelName);
        channelConfigDao.delete(channelName);
        if (replicating) {
            replicator.notifyWatchers();
        }
        return true;
    }
}
