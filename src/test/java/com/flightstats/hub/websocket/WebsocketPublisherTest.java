package com.flightstats.hub.websocket;

public class WebsocketPublisherTest {

/*    @Test
    public void testPublish() throws Exception {
		ContentKey contentKey = new ContentKey((short) 1000);

		HazelcastInstance hazelcastInstance = mock(HazelcastInstance.class);
		ITopic iTopic = mock(ITopic.class);

		when(hazelcastInstance.getTopic("ws:channelName")).thenReturn(iTopic);

		WebsocketPublisher testClass = new WebsocketPublisherImpl(hazelcastInstance);

		testClass.publish("channelName", contentKey);

		verify(iTopic).publish("1000");
	}

	@Test
	public void testSubscribe() throws Exception {
		HazelcastInstance hazelcastInstance = mock(HazelcastInstance.class);
		MessageListener messageListener = mock(MessageListener.class);
		ITopic iTopic = mock(ITopic.class);

		when(hazelcastInstance.getTopic("ws:channelName")).thenReturn(iTopic);

		WebsocketPublisher testClass = new WebsocketPublisherImpl(hazelcastInstance);

		testClass.subscribe("channelName", messageListener);
		verify(iTopic).addMessageListener(messageListener);
	}

	@Test
	public void testUnsubscribe() throws Exception {
		HazelcastInstance hazelcastInstance = mock(HazelcastInstance.class);
		ITopic iTopic = mock(ITopic.class);

		when(hazelcastInstance.getTopic("ws:channelName")).thenReturn(iTopic);

		WebsocketPublisher testClass = new WebsocketPublisherImpl(hazelcastInstance);

		testClass.unsubscribe("channelName", "todo");
		verify(iTopic).removeMessageListener("todo");
	}*/


}
