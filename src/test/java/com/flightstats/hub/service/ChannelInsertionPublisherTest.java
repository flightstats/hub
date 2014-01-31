package com.flightstats.hub.service;

import com.codahale.metrics.MetricRegistry;
import com.flightstats.hub.metrics.MetricsTimer;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.SequenceContentKey;
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
		ContentKey contentKey = new SequenceContentKey((short) 1000);

		HazelcastInstance hazelcastInstance = mock(HazelcastInstance.class);
		ITopic iTopic = mock(ITopic.class);

		when(hazelcastInstance.getTopic("ws:channelName")).thenReturn(iTopic);

		ChannelInsertionPublisher testClass = new ChannelInsertionPublisher(hazelcastInstance, metricsTimer);

		testClass.publish("channelName", contentKey);

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
