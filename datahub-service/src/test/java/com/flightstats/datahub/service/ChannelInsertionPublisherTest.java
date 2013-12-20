package com.flightstats.datahub.service;

import com.codahale.metrics.MetricRegistry;
import com.flightstats.datahub.metrics.MetricsTimer;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.model.SequenceDataHubKey;
import com.flightstats.datahub.model.ValueInsertionResult;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.MessageListener;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;

public class ChannelInsertionPublisherTest {

    private MetricsTimer metricsTimer;

    @Before
    public void setUp() throws Exception {
        metricsTimer = new MetricsTimer(new MetricRegistry());
    }

    @Test
	public void testPublish() throws Exception {
		DataHubKey dataHubKey = new SequenceDataHubKey((short) 1000);

		HazelcastInstance hazelcastInstance = mock(HazelcastInstance.class);
		ITopic iTopic = mock(ITopic.class);

		when(hazelcastInstance.getTopic("ws:channelName")).thenReturn(iTopic);

		ChannelInsertionPublisher testClass = new ChannelInsertionPublisher(hazelcastInstance, metricsTimer);

		testClass.publish("channelName", new ValueInsertionResult(dataHubKey, null, null));

		verify(iTopic).publish("1000");
	}

	@Test
	public void testSubscribe() throws Exception {
		HazelcastInstance hazelcastInstance = mock(HazelcastInstance.class);
		MessageListener messageListener = mock(MessageListener.class);
		ITopic iTopic = mock(ITopic.class);

		when(hazelcastInstance.getTopic("ws:channelName")).thenReturn(iTopic);

		ChannelInsertionPublisher testClass = new ChannelInsertionPublisher(hazelcastInstance, metricsTimer);

		testClass.subscribe("channelName", messageListener);
		verify(iTopic).addMessageListener(messageListener);
	}

	@Test
	public void testUnsubscribe() throws Exception {
		HazelcastInstance hazelcastInstance = mock(HazelcastInstance.class);
		ITopic iTopic = mock(ITopic.class);

		when(hazelcastInstance.getTopic("ws:channelName")).thenReturn(iTopic);

		ChannelInsertionPublisher testClass = new ChannelInsertionPublisher(hazelcastInstance, metricsTimer);

		testClass.unsubscribe("channelName", "todo");
		verify(iTopic).removeMessageListener("todo");
	}


}
