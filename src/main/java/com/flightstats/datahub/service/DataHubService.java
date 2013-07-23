package com.flightstats.datahub.service;

import com.flightstats.datahub.dao.ChannelDao;
import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.model.ValueInsertionResult;
import com.flightstats.datahub.model.exception.AlreadyExistsException;
import com.flightstats.datahub.model.exception.InvalidRequestException;
import com.google.common.base.Optional;
import com.google.inject.Inject;

import java.util.concurrent.Callable;

//todo write unit tests.
public class DataHubService {
	private final ChannelDao channelDao;
	private final CreateChannelValidator createChannelValidator;
	private final ChannelLockExecutor channelLockExecutor;
	private final InsertionTopicProxy insertionTopicProxy;

	@Inject
	public DataHubService(ChannelDao channelDao, CreateChannelValidator createChannelValidator, ChannelLockExecutor channelLockExecutor, InsertionTopicProxy insertionTopicProxy) {
		this.channelDao = channelDao;
		this.createChannelValidator = createChannelValidator;
		this.channelLockExecutor = channelLockExecutor;
		this.insertionTopicProxy = insertionTopicProxy;
	}

	public Iterable<ChannelConfiguration> getChannels() {
		return channelDao.getChannels();
	}

	public ChannelConfiguration createChannel(String channelName, Long ttl) throws InvalidRequestException, AlreadyExistsException {
		createChannelValidator.validate(channelName);
		return channelDao.createChannel(channelName, ttl);
	}

	public boolean channelExists(String channelName) {
		return channelDao.channelExists(channelName);
	}

	public ChannelConfiguration getChannelConfiguration(String channelName) {
		return channelDao.getChannelConfiguration(channelName);
	}

	public Optional<DataHubKey> findLatestId(String channelName) {
		return channelDao.findLatestId(channelName);
	}

	public ValueInsertionResult insert(String channelName, String contentType, byte[] data) throws Exception {
		Callable<ValueInsertionResult> task = new WriteAndDispatch(channelName, contentType, data);
		return channelLockExecutor.execute(channelName, task);
	}

	private class WriteAndDispatch implements Callable<ValueInsertionResult> {
		private final String channelName;
		private final String contentType;
		private final byte[] data;

		private WriteAndDispatch(String channelName, String contentType, byte[] data) {
			this.channelName = channelName;
			this.contentType = contentType;
			this.data = data;
		}

		@Override
		public ValueInsertionResult call() throws Exception {
			ValueInsertionResult result = channelDao.insert(channelName, contentType, data);
			insertionTopicProxy.publish(channelName, result);
			return result;
		}
	}

}
