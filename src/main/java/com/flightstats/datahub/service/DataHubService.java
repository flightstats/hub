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

import javax.ws.rs.core.UriInfo;
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

	public Optional<DataHubKey> findLastUpdatedKey(String channelName) {
		return channelDao.findLastUpdatedKey(channelName);
	}

	//todo: passing in UriInfo here is clearly not cool. figure out how to get rid of HTTP knowledge down here.
	public ValueInsertionResult insert(String channelName, Optional<String> contentType, byte[] data, UriInfo uriInfo, Optional<String> contentEncoding, Optional<String> contentLanguage) throws Exception {
		Callable<ValueInsertionResult> task = new WriteAndDispatch(channelName, contentType, contentEncoding, contentLanguage, data, uriInfo);
		return channelLockExecutor.execute(channelName, task);
	}

	public Optional<LinkedDataHubCompositeValue> getValue(String channelName, DataHubKey key) {
		return channelDao.getValue(channelName, key);
	}

	public void updateChannelMetadata(ChannelConfiguration channelConfiguration) {
		channelDao.updateChannelMetadata(channelConfiguration);
	}

	private class WriteAndDispatch implements Callable<ValueInsertionResult> {
		private final String channelName;
		private final Optional<String> contentType;
		private final Optional<String> contentEncoding;
		private final Optional<String> contentLanguage;
		private final byte[] data;
		private final UriInfo uriInfo;

		private WriteAndDispatch(String channelName, Optional<String> contentType, Optional<String> contentEncoding, Optional<String> contentLanguage, byte[] data, UriInfo uriInfo) {
			this.channelName = channelName;
			this.contentType = contentType;
			this.contentEncoding = contentEncoding;
			this.contentLanguage = contentLanguage;
			this.data = data;
			this.uriInfo = uriInfo;
		}

		@Override
		public ValueInsertionResult call() throws Exception {
			ValueInsertionResult result = channelDao.insert(channelName, contentType, contentEncoding, contentLanguage, data);
			insertionTopicProxy.publish(channelName, result, uriInfo);
			return result;
		}
	}

}
