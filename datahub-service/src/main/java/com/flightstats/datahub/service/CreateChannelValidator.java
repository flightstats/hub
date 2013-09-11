package com.flightstats.datahub.service;

import com.flightstats.datahub.dao.ChannelDao;
import com.flightstats.datahub.model.ChannelCreationRequest;
import com.flightstats.datahub.model.exception.AlreadyExistsException;
import com.flightstats.datahub.model.exception.InvalidRequestException;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.inject.Inject;

public class CreateChannelValidator {
    private final ChannelDao channelDao;

    @Inject
    public CreateChannelValidator(ChannelDao channelDao) {
        this.channelDao = channelDao;
    }

    public void validate(ChannelCreationRequest request) throws InvalidRequestException, AlreadyExistsException {
        Optional<String> channelNameOptional = request != null ? request.getName() : Optional.<String>absent();
        validateNameWasGiven(channelNameOptional);
        String channelName = channelNameOptional.get().trim();
        ensureNotAllBlank(channelName);
        checkForInvalidCharacters(channelName);
        validateChannelUniqueness(channelName);
    }

    private void validateNameWasGiven(Optional<String> channelName) throws InvalidRequestException {
        if ((channelName == null) || !channelName.isPresent()) {
            throw new InvalidRequestException("{\"error\": \"Channel name wasn't given\"}");
        }
    }

    private void ensureNotAllBlank(String channelName) throws InvalidRequestException {
        if (Strings.nullToEmpty(channelName).trim().isEmpty()) {
            throw new InvalidRequestException("{\"error\": \"Channel name cannot be blank\"}");
        }
    }

    private void checkForInvalidCharacters(String channelName) throws InvalidRequestException {
        if (!channelName.matches("^[a-zA-Z0-9_]+$")) {
            throw new InvalidRequestException("{\"error\": \"Channel name must only contain characters a-z, A-Z, and 0-9\"}");
        }
    }

    private void validateChannelUniqueness(String channelName) throws AlreadyExistsException {
        if (channelDao.channelExists(channelName)) {
            throw new AlreadyExistsException("{\"error\": \"Channel name " + channelName + " already exists\"}");
        }
    }
}
