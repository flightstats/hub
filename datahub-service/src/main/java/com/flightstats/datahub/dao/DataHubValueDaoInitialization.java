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
 * DataHubValueDao to initialize (bootstrap) the values table.
 * Ideally, this only ever happens once and is forgotten...but realistically....
 * in the event that we spin up a new datahub this will help facilitate bootstrapping.
 * Also dev + ephemeral storage will like this.
 */
public class DataHubValueDaoInitialization implements TypeListener {

	private final static Logger logger = LoggerFactory.getLogger(DataHubValueDaoInitialization.class);

	private final ApplyOnce<DataHubValueDao, Void> initOnce = new ApplyOnce<>(
			new Function<DataHubValueDao, Void>() {
				@Override
				public Void apply(DataHubValueDao dataHubValueDao) {
					logger.info("Bootstrapping value table...");
					dataHubValueDao.initialize();
					return null;
				}
			});

	@Override
	public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
		encounter.register(new InjectionListener<I>() {
			@Override
			public void afterInjection(Object instance) {
                initOnce.apply((DataHubValueDao) instance);
			}
		});

	}

	public static AbstractMatcher<TypeLiteral<?>> buildTypeMatcher() {
		return new AbstractMatcher<TypeLiteral<?>>() {
			@Override
			public boolean matches(TypeLiteral<?> typeLiteral) {
				return DataHubValueDao.class.isAssignableFrom(typeLiteral.getRawType());
			}
		};
	}

}
