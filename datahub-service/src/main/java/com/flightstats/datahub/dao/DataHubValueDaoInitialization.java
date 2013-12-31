package com.flightstats.datahub.dao;

import com.google.inject.TypeLiteral;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a guice binding via TypeListener.  It provides a means for the
 * ContentDao to initialize (bootstrap) the values table.
 * Ideally, this only ever happens once and is forgotten...but realistically....
 * in the event that we spin up a new datahub this will help facilitate bootstrapping.
 * Also dev + ephemeral storage will like this.
 */
public class DataHubValueDaoInitialization implements TypeListener {

	private final static Logger logger = LoggerFactory.getLogger(DataHubValueDaoInitialization.class);

	@Override
	public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
		encounter.register(new InjectionListener<I>() {
			@Override
			public void afterInjection(Object instance) {
                logger.info("Bootstrapping ContentDao...");
                ((ContentDao)instance).initialize();
			}
		});
	}

	public static AbstractMatcher<TypeLiteral<?>> buildTypeMatcher() {
		return new AbstractMatcher<TypeLiteral<?>>() {
			@Override
			public boolean matches(TypeLiteral<?> typeLiteral) {
				return ContentDao.class.isAssignableFrom(typeLiteral.getRawType());
			}
		};
	}

}
