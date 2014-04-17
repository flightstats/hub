package com.flightstats.hub.service;

import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.ChannelConfiguration;
import com.flightstats.hub.model.exception.AlreadyExistsException;
import com.flightstats.hub.model.exception.InvalidRequestException;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.inject.Inject;

public class CreateChannelValidator {
    private final ChannelService channelService;

    @Inject
    public CreateChannelValidator(ChannelService channelService) {
        this.channelService = channelService;
    }

    public void validate(ChannelConfiguration request) throws InvalidRequestException, AlreadyExistsException {
        Optional<String> channelNameOptional =  Optional.absent();
        if (request != null) {
            channelNameOptional = Optional.fromNullable(request.getName());
        }

        validateNameWasGiven(channelNameOptional);
        String channelName = channelNameOptional.get().trim();
        ensureNotAllBlank(channelName);
        ensureSize(channelName);
        checkForInvalidCharacters(channelName);
        validateChannelUniqueness(channelName);
        validateRate(request);
        validateContentSize(request);
        validateTTL(request);
        validateDescription(request);
    }

    private void validateDescription(ChannelConfiguration request) {
        if (request.getDescription().length() > 1024) {
            throw new InvalidRequestException("{\"error\": \"Description must be less than 1024 bytes. \"}");
        }
    }

    private void validateTTL(ChannelConfiguration request) throws InvalidRequestException {
        if (request.getTtlDays() <= 0) {
            throw new InvalidRequestException("{\"error\": \"TTL must be greater than 0 (zero) \"}");
        }
    }


    private void validateContentSize(ChannelConfiguration request) throws InvalidRequestException {
        if (request.getContentSizeKB() <= 0) {
            throw new InvalidRequestException("{\"error\": \"Content Size must be greater than 0 (zero) \"}");
        }
    }

    private void validateRate(ChannelConfiguration request) throws InvalidRequestException {
        if (request.getPeakRequestRateSeconds() <= 0) {
            throw new InvalidRequestException("{\"error\": \"Peak Request Rate must be greater than 0 (zero) \"}");
        }
    }

    private void validateNameWasGiven(Optional<String> channelName) throws InvalidRequestException {
        if ((channelName == null) || !channelName.isPresent()) {
            throw new InvalidRequestException("{\"error\": \"Channel name wasn't given\"}");
        }
    }

    private void ensureSize(String channelName) throws InvalidRequestException {
        if (channelName.length() > 48) {
            throw new InvalidRequestException("{\"error\": \"Channel name is too long " + channelName + "\"}");
        }
    }

    private void ensureNotAllBlank(String channelName) throws InvalidRequestException {
        if (Strings.nullToEmpty(channelName).trim().isEmpty()) {
            throw new InvalidRequestException("{\"error\": \"Channel name cannot be blank\"}");
        }
    }

    private void checkForInvalidCharacters(String channelName) throws InvalidRequestException {
        if (!channelName.matches("^[a-zA-Z0-9_]+$")) {
            throw new InvalidRequestException("{\"error\": \"Channel name " + channelName + "must only contain characters a-z, A-Z, and 0-9\"}");
        }
    }

    private void validateChannelUniqueness(String channelName) throws AlreadyExistsException {
        if (channelService.channelExists(channelName)) {
            throw new AlreadyExistsException("{\"error\": \"Channel name " + channelName + " already exists\"}");
        }
    }
}
