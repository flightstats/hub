package com.flightstats.datahub.service;

import com.flightstats.datahub.dao.ChannelService;
import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.exception.AlreadyExistsException;
import com.flightstats.datahub.model.exception.InvalidRequestException;
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
        checkForInvalidCharacters(channelName);
        //todo - gfm - 1/13/14 - check for length, max ??
        validateChannelUniqueness(channelName);
        validateRate(request);
        validateContentSize(request);
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
