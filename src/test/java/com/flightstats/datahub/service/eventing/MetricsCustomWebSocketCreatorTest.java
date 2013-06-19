package com.flightstats.datahub.service.eventing;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.UpgradeRequest;
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
		WebSocketChannelNameExtractor channelNameExtractor = new WebSocketChannelNameExtractor();
		int threadCt = 50;
		String meterName = "websocket-clients.channels.ubuibi";

		final CountDownLatch startLatch = new CountDownLatch(1);
		final CountDownLatch allStarted = new CountDownLatch(threadCt);
		final CountDownLatch allFinished = new CountDownLatch(threadCt);

		MetricRegistry registry = mock(MetricRegistry.class);
		final UpgradeRequest request = mock(UpgradeRequest.class);
		final Session session = mock(Session.class);
		Counter counter = spy(new Counter());
        SubscriptionRoster subscriptionRoster = mock( SubscriptionRoster.class );


		when(request.getRequestURI()).thenReturn(URI.create("/channel/ubuibi/ws"));
		when(session.getRemoteAddress()).thenReturn(new InetSocketAddress(2133));
		when(session.getUpgradeRequest()).thenReturn(request);
		when(registry.counter(meterName)).thenReturn(counter);

		final MetricsCustomWebSocketCreator testClass = new MetricsCustomWebSocketCreator(registry, subscriptionRoster, channelNameExtractor);

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
		verify(registry, times(threadCt * 2)).counter(eq(meterName));    //once per inc, once per dec
		verify(counter, times(threadCt)).inc();
		verify(counter, times(threadCt)).dec();
		verify(registry).remove(meterName);
	}
}
