package com.flightstats.hub.group;

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

    public Group upsertGroup(Group group) {
        logger.info("creating group " + group);
        //todo - gfm - 5/30/14 - check that the group exists
        //todo - gfm - 5/30/14 - make sure the channel hasn't changed
        //todo - gfm - 5/30/14 - save in Dynamo
        //todo - gfm - 5/30/14 - update Group with latest value
        dynamoGroupDao.upsertGroup(group);

        return group;
    }

    public Group getGroup(String name) {
        return dynamoGroupDao.getGroup(name);
    }

    public Iterable<Group> getGroups() {
        return dynamoGroupDao.getGroups();
    }

    public void delete(String name) {
        dynamoGroupDao.delete(name);
    }

}
