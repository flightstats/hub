package com.flightstats.hub.dao;

import com.flightstats.hub.util.ApplyOnce;
import com.google.common.base.Function;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a guice binding via TypeListener.  It provides a means for the ChannelConfigurationDao to initialize.
 */
public class ChannelConfigurationInitialization implements TypeListener {

	private final static Logger logger = LoggerFactory.getLogger(ChannelConfigurationInitialization.class);

	private final ApplyOnce<ChannelConfigurationDao, Void> initOnce = new ApplyOnce<>(
			new Function<ChannelConfigurationDao, Void>() {
				@Override
				public Void apply(ChannelConfigurationDao channelConfigurationDao) {
					logger.info("Bootstrapping channel metadata...");
					channelConfigurationDao.initialize();
					return null;
				}
			});

	@Override
	public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
		encounter.register(new InjectionListener<I>() {
			@Override
			public void afterInjection(Object instance) {
				ChannelConfigurationDao channelConfigurationDao = (ChannelConfigurationDao) instance;
				initOnce.apply(channelConfigurationDao);
			}
		});

	}

	public static AbstractMatcher<TypeLiteral<?>> buildTypeMatcher() {
		return new AbstractMatcher<TypeLiteral<?>>() {
			@Override
			public boolean matches(TypeLiteral<?> typeLiteral) {
				return ChannelConfigurationDao.class.isAssignableFrom(typeLiteral.getRawType());
			}
		};
	}

}
