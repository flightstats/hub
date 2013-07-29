package com.flightstats.datahub.service;

import com.flightstats.datahub.dao.ChannelDao;
import com.flightstats.datahub.model.ValueInsertionResult;
import com.google.common.base.Optional;

import java.util.Arrays;
import java.util.concurrent.Callable;

class WriteAndDispatch implements Callable<ValueInsertionResult> {
	private final String channelName;
	private final Optional<String> contentType;
	private final Optional<String> contentEncoding;
	private final Optional<String> contentLanguage;
	private final byte[] data;
	private final ChannelDao channelDao;
	private final InsertionTopicProxy insertionTopicProxy;

	WriteAndDispatch(ChannelDao channelDao, InsertionTopicProxy insertionTopicProxy, String channelName, byte[] data, Optional<String> contentType, Optional<String> contentEncoding, Optional<String> contentLanguage) {
		this.channelDao = channelDao;
		this.insertionTopicProxy = insertionTopicProxy;
		this.channelName = channelName;
		this.contentType = contentType;
		this.contentEncoding = contentEncoding;
		this.contentLanguage = contentLanguage;
		this.data = data;
	}

	@Override
	public ValueInsertionResult call() throws Exception {
		ValueInsertionResult result = channelDao.insert(channelName, contentType, contentEncoding, contentLanguage, data);
		insertionTopicProxy.publish(channelName, result);
		return result;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		WriteAndDispatch that = (WriteAndDispatch) o;

		if (channelDao != null ? !channelDao.equals(that.channelDao) : that.channelDao != null) return false;
		if (channelName != null ? !channelName.equals(that.channelName) : that.channelName != null) return false;
		if (contentEncoding != null ? !contentEncoding.equals(that.contentEncoding) : that.contentEncoding != null)
			return false;
		if (contentLanguage != null ? !contentLanguage.equals(that.contentLanguage) : that.contentLanguage != null)
			return false;
		if (contentType != null ? !contentType.equals(that.contentType) : that.contentType != null) return false;
		if (!Arrays.equals(data, that.data)) return false;
		if (insertionTopicProxy != null ? !insertionTopicProxy.equals(that.insertionTopicProxy) : that.insertionTopicProxy != null)
			return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = channelName != null ? channelName.hashCode() : 0;
		result = 31 * result + (contentType != null ? contentType.hashCode() : 0);
		result = 31 * result + (contentEncoding != null ? contentEncoding.hashCode() : 0);
		result = 31 * result + (contentLanguage != null ? contentLanguage.hashCode() : 0);
		result = 31 * result + (data != null ? Arrays.hashCode(data) : 0);
		result = 31 * result + (channelDao != null ? channelDao.hashCode() : 0);
		result = 31 * result + (insertionTopicProxy != null ? insertionTopicProxy.hashCode() : 0);
		return result;
	}
}
