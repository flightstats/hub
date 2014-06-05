package com.flightstats.hub.group;

import com.flightstats.hub.model.exception.ConflictException;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupService {
    private final static Logger logger = LoggerFactory.getLogger(GroupService.class);

    private final DynamoGroupDao dynamoGroupDao;
    private final GroupValidator groupValidator;
    private final GroupCallback groupCallback;

    @Inject
    public GroupService(DynamoGroupDao dynamoGroupDao, GroupValidator groupValidator, GroupCallback groupCallback) {
        this.dynamoGroupDao = dynamoGroupDao;
        this.groupValidator = groupValidator;
        this.groupCallback = groupCallback;
    }

    public Optional<Group> upsertGroup(Group group) {
        logger.info("upsert group " + group);
        groupValidator.validate(group);
        //todo - gfm - 6/3/14 - should this validate that the server url is correct and the channel is correct?
        Optional<Group> existingGroup = getGroup(group.getName());
        if (existingGroup.isPresent()) {
            if (!existingGroup.get().getChannelUrl().equals(group.getChannelUrl())) {
                throw new ConflictException("{\"error\": \"channelUrl may not change\"}");
            }
        }
        dynamoGroupDao.upsertGroup(group);
        groupCallback.notifyWatchers();
        return existingGroup;
    }

    public Optional<Group> getGroup(String name) {
        Optional<Group> group = dynamoGroupDao.getGroup(name);
        //todo - gfm - 6/5/14 - add latest completed and current
        return group;
    }

    public Iterable<Group> getGroups() {
        return dynamoGroupDao.getGroups();
    }

    public void delete(String name) {
        logger.info("deleting group " + name);
        dynamoGroupDao.delete(name);
        groupCallback.notifyWatchers();
    }

}
