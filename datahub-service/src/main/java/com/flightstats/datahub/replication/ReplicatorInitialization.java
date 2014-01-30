package com.flightstats.datahub.replication;

import com.flightstats.datahub.util.ApplyOnce;
import com.google.common.base.Function;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ReplicatorInitialization implements TypeListener {

	private final static Logger logger = LoggerFactory.getLogger(ReplicatorInitialization.class);

	private final ApplyOnce<ReplicatorImpl, Void> initOnce = new ApplyOnce<>(
			new Function<ReplicatorImpl, Void>() {
				@Override
				public Void apply(ReplicatorImpl replicator) {
					logger.info("Initializing Replicator");
                    replicator.start();
					return null;
				}
			});

	@Override
	public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
		encounter.register(new InjectionListener<I>() {
			@Override
			public void afterInjection(Object instance) {
                initOnce.apply((ReplicatorImpl) instance);
			}
		});

	}

	public static AbstractMatcher<TypeLiteral<?>> buildTypeMatcher() {
		return new AbstractMatcher<TypeLiteral<?>>() {
			@Override
			public boolean matches(TypeLiteral<?> typeLiteral) {
				return ReplicatorImpl.class.isAssignableFrom(typeLiteral.getRawType());
			}
		};
	}

}
