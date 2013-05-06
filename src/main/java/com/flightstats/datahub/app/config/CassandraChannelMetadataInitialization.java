package com.flightstats.datahub.app.config;

import com.flightstats.datahub.dao.CassandraChannelsCollection;
import com.flightstats.datahub.util.ApplyOnce;
import com.google.common.base.Function;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

class CassandraChannelMetadataInitialization implements TypeListener {

	private final ApplyOnce<CassandraChannelsCollection, Void> initOnce = new ApplyOnce<>(
			new Function<CassandraChannelsCollection, Void>() {
				@Override
				public Void apply(CassandraChannelsCollection channelsCollection) {
					channelsCollection.initializeMetadata();
					return null;
				}
			});

	@Override
	public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
		System.out.println(type);
		encounter.register(new InjectionListener<I>() {
			@Override
			public void afterInjection(Object instance) {
				CassandraChannelsCollection channelsCollection = (CassandraChannelsCollection) instance;
				initOnce.apply(channelsCollection);
			}
		});

	}

	public static AbstractMatcher<TypeLiteral<?>> buildTypeMatcher() {
		return new AbstractMatcher<TypeLiteral<?>>() {
			@Override
			public boolean matches(TypeLiteral<?> typeLiteral) {
				return CassandraChannelsCollection.class.isAssignableFrom(typeLiteral.getRawType());
			}
		};
	}

}
