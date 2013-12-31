package com.flightstats.datahub.dao;

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
 * ChannelMetadataDao to initialize (bootstrap) the channels metadata
 * column family.  Ideally, this only ever happens once and is forgotten...but realistically....
 * in the event that we spin up a new datahub this will help facilitate bootstrapping.
 * Also dev + ephemeral storage will like this.
 */
public class ChannelMetadataInitialization implements TypeListener {

	private final static Logger logger = LoggerFactory.getLogger(ChannelMetadataInitialization.class);

	private final ApplyOnce<ChannelMetadataDao, Void> initOnce = new ApplyOnce<>(
			new Function<ChannelMetadataDao, Void>() {
				@Override
				public Void apply(ChannelMetadataDao channelMetadataDao) {
					logger.info("Bootstrapping channel metadata...");
					channelMetadataDao.initializeMetadata();
					return null;
				}
			});

	@Override
	public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
		encounter.register(new InjectionListener<I>() {
			@Override
			public void afterInjection(Object instance) {
				ChannelMetadataDao channelMetadataDao = (ChannelMetadataDao) instance;
				initOnce.apply(channelMetadataDao);
			}
		});

	}

	public static AbstractMatcher<TypeLiteral<?>> buildTypeMatcher() {
		return new AbstractMatcher<TypeLiteral<?>>() {
			@Override
			public boolean matches(TypeLiteral<?> typeLiteral) {
				return ChannelMetadataDao.class.isAssignableFrom(typeLiteral.getRawType());
			}
		};
	}

}
