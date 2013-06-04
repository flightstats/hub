package com.flightstats.datahub.service.eventing;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.UpgradeRequest;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.CountDownLatch;

import static junit.framework.TestCase.fail;
import static org.mockito.Mockito.*;

public class MetricsCustomWebSocketCreatorTest {

	@Test
	public void testCreateWebSocket() throws Exception {
		//GIVEN
		SubscriptionRoster subscriptions = new SubscriptionRoster();
		WebSocketChannelNameExtractor channelNameExtractor = new WebSocketChannelNameExtractor();
		int threadCt = 50;
		final CountDownLatch startLatch = new CountDownLatch(1);
		final CountDownLatch allStarted = new CountDownLatch(threadCt);
		final CountDownLatch allFinished = new CountDownLatch(threadCt);

		MetricRegistry registry = mock(MetricRegistry.class);
		final UpgradeRequest request = mock(UpgradeRequest.class);
		final Session session = mock(Session.class);

		when(request.getRequestURI()).thenReturn(URI.create("/channel/ubuibi/ws"));
		when(session.getRemoteAddress()).thenReturn(new InetSocketAddress(2133));
		when(session.getUpgradeRequest()).thenReturn(request);

		final MetricsCustomWebSocketCreator testClass = new MetricsCustomWebSocketCreator(registry, subscriptions, channelNameExtractor);

		//WHEN
		for (int i = 0; i < threadCt; i++) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						allStarted.countDown();
						startLatch.await();
						DataHubWebSocket socket = (DataHubWebSocket) testClass.createWebSocket(request, null);
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
		allFinished.await();
		//THEN
		verify(registry).register(eq("websocket-clients.channels.ubuibi"), any(Metric.class));
		verify(registry).remove("websocket-clients.channels.ubuibi");
	}
}
