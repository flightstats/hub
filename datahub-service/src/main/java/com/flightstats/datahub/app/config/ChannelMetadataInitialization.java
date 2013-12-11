package com.flightstats.datahub.app.config;

import com.flightstats.datahub.dao.ChannelsCollectionDao;
import com.flightstats.datahub.util.ApplyOnce;
import com.google.common.base.Function;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a guice binding via TypeListener.  It provides a means for the
 * ChannelsCollectionDao to initialize (bootstrap) the channels metadata
 * column family.  Ideally, this only ever happens once and is forgotten...but realistically....
 * in the event that we spin up a new datahub this will help facilitate bootstrapping.
 * Also dev + ephemeral storage will like this.
 */
class ChannelMetadataInitialization implements TypeListener {

	private final static Logger logger = LoggerFactory.getLogger(ChannelMetadataInitialization.class);

	private final ApplyOnce<ChannelsCollectionDao, Void> initOnce = new ApplyOnce<>(
			new Function<ChannelsCollectionDao, Void>() {
				@Override
				public Void apply(ChannelsCollectionDao channelsCollectionDao) {
					logger.info("Bootstrapping channel metadata...");
					channelsCollectionDao.initializeMetadata();
					return null;
				}
			});

	@Override
	public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
		encounter.register(new InjectionListener<I>() {
			@Override
			public void afterInjection(Object instance) {
				ChannelsCollectionDao channelsCollectionDao = (ChannelsCollectionDao) instance;
				initOnce.apply(channelsCollectionDao);
			}
		});

	}

	public static AbstractMatcher<TypeLiteral<?>> buildTypeMatcher() {
		return new AbstractMatcher<TypeLiteral<?>>() {
			@Override
			public boolean matches(TypeLiteral<?> typeLiteral) {
				return ChannelsCollectionDao.class.isAssignableFrom(typeLiteral.getRawType());
			}
		};
	}

}
