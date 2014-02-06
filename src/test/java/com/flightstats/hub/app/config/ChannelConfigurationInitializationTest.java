package com.flightstats.hub.app.config;

import com.flightstats.hub.dao.ChannelConfigurationDao;
import com.flightstats.hub.dao.ChannelConfigurationInitialization;
import com.flightstats.hub.dao.dynamo.DynamoChannelConfigurationDao;
import com.google.inject.TypeLiteral;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ChannelConfigurationInitializationTest {

	@Test
	public void testHear() throws Exception {
		ChannelConfigurationInitialization testClass = new ChannelConfigurationInitialization();
		TypeLiteral<Object> type = mock(TypeLiteral.class);
		TypeEncounter<Object> encounter = mock(TypeEncounter.class);
		testClass.hear(type, encounter);

		verify(encounter).register(isA(InjectionListener.class));
	}

	@Test
	public void testInjectionListenerLifecycle_initOnlyCalledOnce() throws Exception {
		ChannelConfigurationInitialization testClass = new ChannelConfigurationInitialization();
		TypeLiteral<Object> type = mock(TypeLiteral.class);
		TypeEncounter<Object> encounter = mock(TypeEncounter.class);
		ChannelConfigurationDao channelConfigurationDao = mock(DynamoChannelConfigurationDao.class);
		testClass.hear(type, encounter);

		ArgumentCaptor<InjectionListener> captor = ArgumentCaptor.forClass(InjectionListener.class);
		verify(encounter).register(captor.capture());
		InjectionListener listener = captor.getValue();
		listener.afterInjection(channelConfigurationDao);
		listener.afterInjection(channelConfigurationDao);
		listener.afterInjection(channelConfigurationDao);
		verify(channelConfigurationDao).initialize();
	}
}
