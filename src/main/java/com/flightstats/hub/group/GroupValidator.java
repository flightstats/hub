package com.flightstats.hub.group;

import com.flightstats.hub.channel.ChannelValidator;
import com.flightstats.hub.exception.InvalidRequestException;
import com.flightstats.hub.util.ChannelNameUtils;
import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;

public class GroupValidator {

    void validate(Group group) {
        String name = group.getName();
        if (StringUtils.isEmpty(name)) {
            throw new InvalidRequestException("{\"error\": \"Group name is required\"}");
        }
        if (!name.matches(ChannelValidator.VALID_NAME)) {
            throw new InvalidRequestException("{\"error\": \"Group name can only contain characters a-z, A-Z, _ and 0-9\"}");
        }
        if (name.length() > 128) {
            throw new InvalidRequestException("{\"error\": \"Group name must be less than 128 bytes\"}");
        }
        if (group.getParallelCalls() <= 0) {
            throw new InvalidRequestException("{\"error\": \"Group parallelCalls must be greater than zero\"}");
        }
        try {
            new URI(group.getCallbackUrl());
        } catch (URISyntaxException e) {
            throw new InvalidRequestException("{\"error\": \"Invalid callbackUrl\"}");
        }
        if (!ChannelNameUtils.isValidChannelUrl(group.getChannelUrl())) {
            throw new InvalidRequestException("{\"error\": \"Invalid channelUrl\"}");
        }
        group = group.withBatch(StringUtils.upperCase(group.getBatch()));
        if (Group.MINUTE.equals(group.getBatch())
                || Group.SECOND.equals(group.getBatch())
                || Group.SINGLE.equals(group.getBatch())) {
            return;
        } else {
            throw new InvalidRequestException("{\"error\": \"Allowed values for batch are 'SINGLE', 'SECOND' and 'MINUTE'\"}");
        }
    }
}
