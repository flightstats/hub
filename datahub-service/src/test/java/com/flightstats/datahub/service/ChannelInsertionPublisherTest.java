package com.flightstats.datahub.service;

import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.model.ValueInsertionResult;
import com.flightstats.datahub.util.DataHubKeyRenderer;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.MessageListener;
import org.junit.Test;

import static org.mockito.Mockito.*;

public class ChannelInsertionPublisherTest {

	@Test
	public void testPublish() throws Exception {
		DataHubKey dataHubKey = new DataHubKey((short) 1000);

		HazelcastInstance hazelcastInstance = mock(HazelcastInstance.class);
		DataHubKeyRenderer keyRenderer = mock(DataHubKeyRenderer.class);
		ITopic iTopic = mock(ITopic.class);

		when(keyRenderer.keyToString(dataHubKey)).thenReturn("key message");
		when(hazelcastInstance.getTopic("ws:channelName")).thenReturn(iTopic);

		ChannelInsertionPublisher testClass = new ChannelInsertionPublisher(hazelcastInstance, keyRenderer);

		testClass.publish("channelName", new ValueInsertionResult(dataHubKey, null, null));

		verify(iTopic).publish("key message");
	}

	@Test
	public void testSubscribe() throws Exception {
		HazelcastInstance hazelcastInstance = mock(HazelcastInstance.class);
		MessageListener messageListener = mock(MessageListener.class);
		ITopic iTopic = mock(ITopic.class);

		when(hazelcastInstance.getTopic("ws:channelName")).thenReturn(iTopic);

		ChannelInsertionPublisher testClass = new ChannelInsertionPublisher(hazelcastInstance, null);

		testClass.subscribe("channelName", messageListener);
		verify(iTopic).addMessageListener(messageListener);
	}

	@Test
	public void testUnsubscribe() throws Exception {
		HazelcastInstance hazelcastInstance = mock(HazelcastInstance.class);
		MessageListener messageListener = mock(MessageListener.class);
		ITopic iTopic = mock(ITopic.class);

		when(hazelcastInstance.getTopic("ws:channelName")).thenReturn(iTopic);

		ChannelInsertionPublisher testClass = new ChannelInsertionPublisher(hazelcastInstance, null);

		testClass.unsubscribe("channelName", messageListener);
		verify(iTopic).removeMessageListener(messageListener);
	}


}
