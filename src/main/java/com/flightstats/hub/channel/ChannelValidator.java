package com.flightstats.hub.channel;

import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.exception.ConflictException;
import com.flightstats.hub.exception.InvalidRequestException;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.GlobalConfig;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.inject.Inject;

public class ChannelValidator {
    public static final String VALID_NAME = "^[a-zA-Z0-9_-]+$";
    private final ChannelService channelService;

    @Inject
    public ChannelValidator(ChannelService channelService) {
        this.channelService = channelService;
    }

    public void validate(ChannelConfig config, boolean isCreation, ChannelConfig oldConfig) throws InvalidRequestException, ConflictException {
        Optional<String> channelNameOptional = Optional.absent();
        if (config != null) {
            channelNameOptional = Optional.fromNullable(config.getName());
        }

        validateNameWasGiven(channelNameOptional);
        String channelName = channelNameOptional.get().trim();
        ensureNotAllBlank(channelName);
        ensureSize(channelName, "name");
        ensureSize(config.getOwner(), "owner");
        checkForInvalidCharacters(channelName);
        if (isCreation) {
            validateChannelUniqueness(channelName);
        }
        validateTTL(config);
        validateDescription(config);
        validateTags(config);
        validateStorage(config);
        validateGlobal(config);
        if (oldConfig != null) {
            validateHistorical(config, oldConfig);
        }
    }

    private void validateHistorical(ChannelConfig config, ChannelConfig oldConfig) {
        if (oldConfig.isHistorical() != config.isHistorical()) {
            throw new InvalidRequestException("the historical state of a channel can not change.");
        }
    }

    private void validateGlobal(ChannelConfig config) {
        if (config.isGlobal()) {
            GlobalConfig global = config.getGlobal();
            String master = global.getMaster();
            if (master == null) {
                throw new InvalidRequestException("{\"error\": \"A Master must exist\"}");
            }
            if (global.getSatellites().isEmpty()) {
                throw new InvalidRequestException("{\"error\": \"At least one Satellite must exist\"}");
            }
            if (global.getSatellites().contains(master)) {
                throw new InvalidRequestException("{\"error\": \"A Master can not also be a Satellite\"}");
            }
            if (!startsWithHttp(master)) {
                throw new InvalidRequestException("{\"error\": \"A Master must start with http or https\"}");
            }
            for (String satellite : global.getSatellites()) {
                if (!startsWithHttp(satellite)) {
                    throw new InvalidRequestException("{\"error\": \"Satellites must start with http or https\"}");
                }
            }
        }
    }

    private boolean startsWithHttp(String master) {
        return master.startsWith("http://") || master.startsWith("https://");
    }

    private void validateStorage(ChannelConfig config) {
        if (!config.isValidStorage()) {
            throw new InvalidRequestException("{\"error\": \"Valid storage values are SINGLE, BATCH and BOTH\"}");
        }
    }

    private void validateTags(ChannelConfig request) {
        if (request.getTags().size() > 20) {
            throw new InvalidRequestException("{\"error\": \"Channels are limited to 20 tags\"}");
        }
        for (String tag : request.getTags()) {
            if (!tag.matches("^[a-zA-Z0-9\\:\\-]+$")) {
                throw new InvalidRequestException("{\"error\": \"Tags must only contain characters a-z, A-Z, and 0-9\"}");
            }
            if (tag.length() > 48) {
                throw new InvalidRequestException("{\"error\": \"Tags must be less than 48 bytes. \"}");
            }
        }
    }

    private void validateDescription(ChannelConfig request) {
        if (request.getDescription().length() > 1024) {
            throw new InvalidRequestException("{\"error\": \"Description must be less than 1024 bytes. \"}");
        }
    }

    private void validateTTL(ChannelConfig request) throws InvalidRequestException {
        if (request.getTtlDays() == 0 && request.getMaxItems() == 0) {
            throw new InvalidRequestException("{\"error\": \"ttlDays or maxItems must be greater than 0 (zero) \"}");
        }
        if (request.getTtlDays() > 0 && request.getMaxItems() > 0) {
            throw new InvalidRequestException("{\"error\": \"Only one of ttlDays and maxItems can be defined \"}");
        }
        if (request.getMaxItems() > 5000) {
            throw new InvalidRequestException("{\"error\": \"maxItems must be less than 5000 \"}");
        }
    }

    private void validateNameWasGiven(Optional<String> channelName) throws InvalidRequestException {
        if ((channelName == null) || !channelName.isPresent()) {
            throw new InvalidRequestException("{\"error\": \"A channel has no name\"}");
        }
    }

    private void ensureSize(String value, String title) throws InvalidRequestException {
        int maxLength = 48;
        if (value == null) {
            return;
        }
        if (value.length() > maxLength) {
            throw new InvalidRequestException("{\"error\": \"Channel " + title + " is too long " + value + "\"}");
        }
    }

    private void ensureNotAllBlank(String channelName) throws InvalidRequestException {
        if (Strings.nullToEmpty(channelName).trim().isEmpty()) {
            throw new InvalidRequestException("{\"error\": \"Channel name cannot be blank\"}");
        }
    }

    private void checkForInvalidCharacters(String channelName) throws InvalidRequestException {
        if (!channelName.matches(VALID_NAME)) {
            throw new InvalidRequestException("{\"error\": \"Channel name " + channelName + "must only contain characters a-z, A-Z, and 0-9\"}");
        }
    }

    private void validateChannelUniqueness(String channelName) throws ConflictException {
        if (channelService.channelExists(channelName)) {
            throw new ConflictException("{\"error\": \"Channel name " + channelName + " already exists\"}");
        }
    }
}
