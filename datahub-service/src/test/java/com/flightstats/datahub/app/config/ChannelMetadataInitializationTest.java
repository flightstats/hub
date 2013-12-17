package com.flightstats.datahub.app.config;

import com.flightstats.datahub.dao.CassandraChannelsCollectionDao;
import com.flightstats.datahub.dao.ChannelsCollectionDao;
import com.google.inject.TypeLiteral;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ChannelMetadataInitializationTest {

	@Test
	public void testHear() throws Exception {
		//GIVEN
		ChannelMetadataInitialization testClass = new ChannelMetadataInitialization();
		//WHEN
		TypeLiteral<Object> type = mock(TypeLiteral.class);
		TypeEncounter<Object> encounter = mock(TypeEncounter.class);
		testClass.hear(type, encounter);

		//THEN
		verify(encounter).register(isA(InjectionListener.class));
	}

	@Test
	public void testInjectionListenerLifecycle_initOnlyCalledOnce() throws Exception {
		//GIVEN
		ChannelMetadataInitialization testClass = new ChannelMetadataInitialization();
		//WHEN
		TypeLiteral<Object> type = mock(TypeLiteral.class);
		TypeEncounter<Object> encounter = mock(TypeEncounter.class);
		ChannelsCollectionDao channelsCollectionDao = mock(CassandraChannelsCollectionDao.class);
		testClass.hear(type, encounter);

		//THEN
		ArgumentCaptor<InjectionListener> captor = ArgumentCaptor.forClass(InjectionListener.class);
		verify(encounter).register(captor.capture());
		InjectionListener listener = captor.getValue();
		listener.afterInjection(channelsCollectionDao);
		listener.afterInjection(channelsCollectionDao);
		listener.afterInjection(channelsCollectionDao);
		verify(channelsCollectionDao).initializeMetadata();
	}
}
