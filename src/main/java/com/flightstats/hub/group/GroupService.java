package com.flightstats.hub.group;

import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.exception.ConflictException;
import com.flightstats.hub.exception.NoSuchChannelException;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.util.ChannelNameUtils;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupService {
    private final static Logger logger = LoggerFactory.getLogger(GroupService.class);

    private final GroupDao groupDao;
    private final GroupValidator groupValidator;
    private final GroupProcessor groupProcessor;
    private final LastContentPath lastContentPath;
    private ChannelService channelService;

    @Inject
    public GroupService(GroupDao groupDao, GroupValidator groupValidator,
                        GroupProcessor groupProcessor, LastContentPath lastContentPath,
                        ChannelService channelService) {
        this.groupDao = groupDao;
        this.groupValidator = groupValidator;
        this.groupProcessor = groupProcessor;
        this.lastContentPath = lastContentPath;
        this.channelService = channelService;
    }

    public Optional<Group> upsertGroup(Group group) {
        logger.info("upsert group " + group);
        group = group.withDefaults();
        groupValidator.validate(group);
        Optional<Group> existingGroup = getGroup(group.getName());
        if (existingGroup.isPresent()) {
            Group existing = existingGroup.get();
            if (existing.equals(group)) {
                return existingGroup;
            } else if (!existing.allowedToChange(group)) {
                throw new ConflictException("{\"error\": \"channelUrl can not change. \"}");
            }
        }
        lastContentPath.initialize(group.getName(), group.getStartingKey(), GroupLeader.GROUP_LAST_COMPLETED);
        groupDao.upsertGroup(group);
        groupProcessor.notifyWatchers();
        return existingGroup;
    }

    public Optional<Group> getGroup(String name) {
        return groupDao.getGroup(name);
    }

    public Iterable<Group> getGroups() {
        return groupDao.getGroups();
    }

    public GroupStatus getGroupStatus(Group group) {
        GroupStatus.GroupStatusBuilder builder = GroupStatus.builder().group(group);
        String channel = ChannelNameUtils.extractFromChannelUrl(group.getChannelUrl());
        try {
            Optional<ContentKey> lastKey = channelService.getLatest(channel, true, false);
            if (lastKey.isPresent()) {
                builder.channelLatest(lastKey.get());
            }
        } catch (NoSuchChannelException e) {`
            logger.info("no channel found for " + channel);
        }
        groupProcessor.getStatus(group, builder);
        return builder.build();
    }

    public void delete(String name) {
        logger.info("deleting group " + name);
        groupDao.delete(name);
        groupProcessor.delete(name);
    }

}
