package com.flightstats.hub.group;

import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.exception.ConflictException;
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
    private final ChannelService channelService;

    @Inject
    public GroupService(DynamoGroupDao dynamoGroupDao, GroupValidator groupValidator,
                        GroupCallback groupCallback, ChannelService channelService) {
        this.dynamoGroupDao = dynamoGroupDao;
        this.groupValidator = groupValidator;
        this.groupCallback = groupCallback;
        this.channelService = channelService;
    }

    public Optional<Group> upsertGroup(Group group) {
        logger.info("upsert group " + group);
        groupValidator.validate(group);
        //todo - gfm - 6/3/14 - should this validate that the server url is correct and the channel is correct?
        Optional<Group> existingGroup = getGroup(group.getName());
        if (existingGroup.isPresent()) {
            //todo - gfm - 6/7/14 - prevent changes to existing groups
            if (!existingGroup.get().getChannelUrl().equals(group.getChannelUrl())) {
                throw new ConflictException("{\"error\": \"channelUrl may not change\"}");
            }
        }
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

    public List<GroupStatus> getGroupStatus() {
        Iterable<Group> groups = getGroups();
        List<GroupStatus> groupStatus = new ArrayList<>();
        for (Group group : groups) {
            GroupStatus.GroupStatusBuilder builder = GroupStatus.builder().group(group);
            Optional<ContentKey> lastUpdatedKey = channelService.findLastUpdatedKey(GroupUtil.getChannelName(group));
            if (lastUpdatedKey.isPresent()) {
                builder.channelLatest(lastUpdatedKey.get().getSequence());
            }
            builder.lastCompleted(groupCallback.getLastCompleted(group));
            groupStatus.add(builder.build());
        }
        return groupStatus;
    }

    public void delete(String name) {
        logger.info("deleting group " + name);
        dynamoGroupDao.delete(name);
        groupCallback.notifyWatchers();
    }

}
