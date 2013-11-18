package com.flightstats.datahub.service;

import com.flightstats.datahub.dao.ChannelDao;
import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.model.LinkedDataHubCompositeValue;
import com.flightstats.datahub.model.ValueInsertionResult;
import com.flightstats.datahub.model.exception.AlreadyExistsException;
import com.flightstats.datahub.model.exception.InvalidRequestException;
import com.google.common.base.Optional;
import com.google.inject.Inject;

import java.util.concurrent.Callable;

public class DataHubService {
	private final ChannelDao channelDao;
	private final ChannelInsertionPublisher channelInsertionPublisher;
    private ChannelLockExecutor channelLockExecutor;

    @Inject
	public DataHubService(ChannelDao channelDao, ChannelInsertionPublisher channelInsertionPublisher, ChannelLockExecutor channelLockExecutor) {
		this.channelDao = channelDao;
		this.channelInsertionPublisher = channelInsertionPublisher;
        this.channelLockExecutor = channelLockExecutor;
    }

	public Iterable<ChannelConfiguration> getChannels() {
		return channelDao.getChannels();
	}

	public ChannelConfiguration createChannel(String channelName, Long ttl) throws InvalidRequestException, AlreadyExistsException {
		return channelDao.createChannel(channelName, ttl);
	}

	public boolean channelExists(String channelName) {
		return channelDao.channelExists(channelName);
	}

	public ChannelConfiguration getChannelConfiguration(String channelName) {
		return channelDao.getChannelConfiguration(channelName);
	}

	public Optional<DataHubKey> findLastUpdatedKey(String channelName) {
		return channelDao.findLastUpdatedKey(channelName);
	}

    public ValueInsertionResult insert(final String channelName, final byte[] data, final Optional<String> contentType,
                                       final Optional<String> contentLanguage) throws Exception {
        return channelLockExecutor.execute(channelName, new Callable<ValueInsertionResult>() {
            @Override
            public ValueInsertionResult call() throws Exception {
                ValueInsertionResult result = channelDao.insert(channelName, contentType, contentLanguage, data);
                channelInsertionPublisher.publish(channelName, result);
                return result;
            }
        });
    }

	public Optional<LinkedDataHubCompositeValue> getValue(String channelName, DataHubKey key) {
		return channelDao.getValue(channelName, key);
	}

	public void updateChannelMetadata(ChannelConfiguration channelConfiguration) {
		channelDao.updateChannelMetadata(channelConfiguration);
	}

}
