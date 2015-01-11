package com.flightstats.hub.group;

import com.flightstats.hub.exception.ConflictException;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class GroupService {
    private final static Logger logger = LoggerFactory.getLogger(GroupService.class);

    private final DynamoGroupDao dynamoGroupDao;
    private final GroupValidator groupValidator;
    private final GroupCallback groupCallback;
    private final GroupContentKey groupContentKey;

    @Inject
    public GroupService(DynamoGroupDao dynamoGroupDao, GroupValidator groupValidator,
                        GroupCallback groupCallback, GroupContentKey groupContentKey) {
        this.dynamoGroupDao = dynamoGroupDao;
        this.groupValidator = groupValidator;
        this.groupCallback = groupCallback;
        this.groupContentKey = groupContentKey;
    }

    public Optional<Group> upsertGroup(Group group) {
        logger.info("upsert group " + group);
        group = group.withDefaults();
        groupValidator.validate(group);
        Optional<Group> existingGroup = getGroup(group.getName());
        if (existingGroup.isPresent()) {
            if (existingGroup.get().equals(group)) {
                return existingGroup;
            }
            throw new ConflictException("{\"error\": \"Groups are immutable\"}");
        }
        groupContentKey.initialize(group.getName(), group.getStartingKey());
        dynamoGroupDao.upsertGroup(group);
        groupCallback.notifyWatchers();
        return existingGroup;
    }

    public Optional<Group> getGroup(String name) {
        return dynamoGroupDao.getGroup(name);
    }

    public Iterable<Group> getGroups() {
        return dynamoGroupDao.getGroups();
    }

    public List<GroupStatus> getGroupStatuses() {
        Iterable<Group> groups = getGroups();
        List<GroupStatus> groupStatuses = new ArrayList<>();
        for (Group group : groups) {
            groupStatuses.add(getGroupStatus(group));
        }
        return groupStatuses;
    }

    public GroupStatus getGroupStatus(Group group) {
        GroupStatus.GroupStatusBuilder builder = GroupStatus.builder().group(group);
        //todo - gfm - 12/10/14 - fix this
            /*
            String channelName = ChannelNameUtils.extractFromChannelUrl(group.getChannelUrl());
            Optional<ContentKey> lastUpdatedKey = channelService.findLastUpdatedKey(channelName);
            if (lastUpdatedKey.isPresent()) {
                builder.channelLatest(lastUpdatedKey.get().getSequence());
            }*/
        builder.lastCompleted(groupCallback.getLastCompleted(group));
        return builder.build();
    }

    public void delete(String name) {
        logger.info("deleting group " + name);
        dynamoGroupDao.delete(name);
        groupCallback.delete(name);
    }

}
