package com.flightstats.datahub.app.config;

import com.flightstats.datahub.dao.CassandraChannelsCollection;
import com.google.inject.TypeLiteral;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class CassandraChannelMetadataInitializationTest {

	@Test
	public void testHear() throws Exception {
		//GIVEN
		CassandraChannelMetadataInitialization testClass = new CassandraChannelMetadataInitialization();
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
		CassandraChannelMetadataInitialization testClass = new CassandraChannelMetadataInitialization();
		//WHEN
		TypeLiteral<Object> type = mock(TypeLiteral.class);
		TypeEncounter<Object> encounter = mock(TypeEncounter.class);
		CassandraChannelsCollection channelsCollection = mock(CassandraChannelsCollection.class);
		testClass.hear(type, encounter);

		//THEN
		ArgumentCaptor<InjectionListener> captor = ArgumentCaptor.forClass(InjectionListener.class);
		verify(encounter).register(captor.capture());
		InjectionListener listener = captor.getValue();
		listener.afterInjection(channelsCollection);
		listener.afterInjection(channelsCollection);
		listener.afterInjection(channelsCollection);
		verify(channelsCollection).initializeMetadata();
	}
}
