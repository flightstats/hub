package com.flightstats.datahub.service;

import com.flightstats.datahub.dao.ChannelDao;
import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.exception.AlreadyExistsException;
import com.flightstats.datahub.model.exception.InvalidRequestException;
import com.google.inject.Inject;

//todo write unit tests.
public class DataHubService {
	private final ChannelDao channelDao;
	private final CreateChannelValidator createChannelValidator;

	@Inject
	public DataHubService(ChannelDao channelDao, CreateChannelValidator createChannelValidator) {
		this.channelDao = channelDao;
		this.createChannelValidator = createChannelValidator;
	}


	public Iterable<ChannelConfiguration> getChannels() {
		return channelDao.getChannels();
	}

	public ChannelConfiguration createChannel(String channelName, Long ttl) throws InvalidRequestException, AlreadyExistsException {
		createChannelValidator.validate(channelName);
		return channelDao.createChannel(channelName, ttl);
	}
}
