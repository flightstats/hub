package com.flightstats.datahub.app.config;

import com.flightstats.datahub.dao.CqlValueOperations;
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
 * CqlValueOperations to initialize (bootstrap) the values table.
 * Ideally, this only ever happens once and is forgotten...but realistically....
 * in the event that we spin up a new datahub this will help facilitate bootstrapping.
 * Also dev + ephemeral storage will like this.
 */
class CqlValueOperationsInitialization implements TypeListener {

	private final static Logger logger = LoggerFactory.getLogger(CqlValueOperationsInitialization.class);

	private final ApplyOnce<CqlValueOperations, Void> initOnce = new ApplyOnce<>(
			new Function<CqlValueOperations, Void>() {
				@Override
				public Void apply(CqlValueOperations cqlValueOperations) {
					logger.info("Bootstrapping value table...");
					cqlValueOperations.initializeTable();
					return null;
				}
			});

	@Override
	public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
		encounter.register(new InjectionListener<I>() {
			@Override
			public void afterInjection(Object instance) {
                initOnce.apply((CqlValueOperations) instance);
			}
		});

	}

	public static AbstractMatcher<TypeLiteral<?>> buildTypeMatcher() {
		return new AbstractMatcher<TypeLiteral<?>>() {
			@Override
			public boolean matches(TypeLiteral<?> typeLiteral) {
				return CqlValueOperations.class.isAssignableFrom(typeLiteral.getRawType());
			}
		};
	}

}
