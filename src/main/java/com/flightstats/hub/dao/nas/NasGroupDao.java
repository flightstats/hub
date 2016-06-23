package com.flightstats.hub.dao.nas;

import com.flightstats.hub.group.Group;
import com.flightstats.hub.group.GroupDao;
import com.google.common.base.Optional;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collection;

public class NasGroupDao implements GroupDao {

    private final static Logger logger = LoggerFactory.getLogger(NasGroupDao.class);
    private final String groupPath;

    public NasGroupDao() {
        groupPath = NasUtil.getStoragePath() + "group/";
        logger.info("using channel path {}", groupPath);
    }

    @Override
    public Group upsertGroup(Group group) {
        NasUtil.writeJson(group.toJson(), group.getName(), groupPath);
        return group;
    }

    @Override
    public Optional<Group> getGroup(String name) {
        return Optional.fromNullable(NasUtil.readJson(groupPath, name, Group::fromJson));
    }

    @Override
    public Collection<Group> getGroups() {
        return NasUtil.getIterable(groupPath, Group::fromJson);
    }

    @Override
    public void delete(String name) {
        FileUtils.deleteQuietly(new File(groupPath + name));
    }

}
