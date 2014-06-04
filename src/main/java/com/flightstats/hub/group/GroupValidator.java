package com.flightstats.hub.group;

import com.flightstats.hub.model.exception.InvalidRequestException;
import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;

public class GroupValidator {

    public void validate(Group group) {
        String name = group.getName();
        if (StringUtils.isEmpty(name)) {
            throw new InvalidRequestException("{\"error\": \"Group name is required\"}");
        }
        if (!name.matches("^[a-zA-Z0-9_]+$")) {
            throw new InvalidRequestException("{\"error\": \"Group name can only contain characters a-z, A-Z, _ and 0-9\"}");
        }
        if (name.length() > 48) {
            throw new InvalidRequestException("{\"error\": \"Group name must be less than 48 bytes\"}");
        }
        try {
            new URI(group.getCallbackUrl());
        } catch (URISyntaxException e) {
            throw new InvalidRequestException("{\"error\": \"Invalid callbackUrl\"}");
        }
        try {
            new URI(group.getChannelUrl());
        } catch (URISyntaxException e) {
            throw new InvalidRequestException("{\"error\": \"Invalid channelUrl\"}");
        }
        //todo - gfm - 6/3/14 - should this enforce channel url's format, or silently change it?
    }
}
