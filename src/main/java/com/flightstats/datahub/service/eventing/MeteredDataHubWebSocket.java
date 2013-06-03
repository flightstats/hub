package com.flightstats.datahub.service.eventing;

import com.codahale.metrics.MetricRegistry;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

//@WebSocket(maxMessageSize = 1024 * 10)    //10k
public class MeteredDataHubWebSocket
{
	private MetricRegistry registry;
	private final DataHubWebSocket dataHubWebSocket;


	public MeteredDataHubWebSocket( SubscriptionRoster subscriptions, MetricRegistry registry, DataHubWebSocket dataHubWebSocket )
	{
		this.registry = registry;
		this.dataHubWebSocket = dataHubWebSocket;
	}

//	@OnWebSocketClose
	public void onConnect( final Session session )
	{
		dataHubWebSocket.onConnect( session );
	}

//	@OnWebSocketClose
	public void onDisconnect( int statusCode, String reason )
	{
		dataHubWebSocket.onDisconnect( statusCode, reason );
	}
}
