package com.flightstats.hub.group;

import com.flightstats.hub.model.exception.ConflictException;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupService {
    private final static Logger logger = LoggerFactory.getLogger(GroupService.class);

    private final DynamoGroupDao dynamoGroupDao;

    @Inject
    public GroupService(DynamoGroupDao dynamoGroupDao) {
        this.dynamoGroupDao = dynamoGroupDao;
    }

    public Optional<Group> upsertGroup(Group group) {
        logger.info("upsert group " + group);
        Optional<Group> existingGroup = getGroup(group.getName());
        if (existingGroup.isPresent()) {
            if (!existingGroup.get().getChannelUrl().equals(group.getChannelUrl())) {
                throw new ConflictException("{\"error\": \"channelUrl may not change\"}");
            }
        }
        dynamoGroupDao.upsertGroup(group);
        return existingGroup;
    }

    public Optional<Group> getGroup(String name) {
        return dynamoGroupDao.getGroup(name);
    }

    public Iterable<Group> getGroups() {
        return dynamoGroupDao.getGroups();
    }

    public void delete(String name) {
        logger.info("deleting group " + name);
        dynamoGroupDao.delete(name);
    }

}
