package com.flightstats.hub.service.eventing;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.flightstats.hub.service.ChannelLinkBuilder;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class MetricsCustomWebSocketCreatorTest {

	@Test
	public void testCreateWebSocket() throws Exception {
		//GIVEN
		ChannelNameExtractor channelNameExtractor = new ChannelNameExtractor();
		int threadCt = 50;
		String meterName = "websocket-clients.channels.ubuibi";
		URI requestUri = URI.create("/channel/ubuibi/ws");

		final CountDownLatch startLatch = new CountDownLatch(1);
		final CountDownLatch createLatch = new CountDownLatch(1);
		final CountDownLatch allCreated = new CountDownLatch(threadCt);
		final CountDownLatch allStarted = new CountDownLatch(threadCt);
		final CountDownLatch allFinished = new CountDownLatch(threadCt);

		MetricRegistry registry = mock(MetricRegistry.class);
		final ServletUpgradeRequest request = mock(ServletUpgradeRequest.class);
		final Session session = mock(Session.class);
		Counter counter = spy(new Counter());
		SubscriptionRoster subscriptionRoster = mock(SubscriptionRoster.class);


		when(request.getRequestURI()).thenReturn(requestUri);
		when(request.getHeader("Host")).thenReturn("myhost:8080");
		when(session.getRemoteAddress()).thenReturn(new InetSocketAddress(2133));
		when(session.getUpgradeRequest()).thenReturn(request);
		when(registry.counter(meterName)).thenReturn(counter);

		final MetricsCustomWebSocketCreator testClass = new MetricsCustomWebSocketCreator(registry, subscriptionRoster, channelNameExtractor, mock(ChannelLinkBuilder.class));

		//WHEN
		for (int i = 0; i < threadCt; i++) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						allStarted.countDown();
						startLatch.await();
						HubWebSocket socket = (HubWebSocket) testClass.createWebSocket(request, null);
						allCreated.countDown();
						createLatch.await();
						socket.onConnect(session);            //lifecycle controlled by jetty framework
						socket.onDisconnect(200, "test");    //lifecycle controlled by jetty framework
					} catch (InterruptedException e) {
						fail("Boom");
					} finally {
						allFinished.countDown();
					}
				}
			}).start();
		}
		allStarted.await();
		startLatch.countDown();
		allCreated.await();
		createLatch.countDown();
		allFinished.await();
		//THEN
		verify(registry, times(threadCt * 2)).counter(eq(meterName));    //once per inc, once per dec
		verify(counter, times(threadCt)).inc();
		verify(counter, times(threadCt)).dec();
		verify(registry, times(1)).remove(meterName);
	}
}
