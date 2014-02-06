package com.flightstats.hub.dao.s3;

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
 * This is a guice binding via TypeListener.  It provides a means for the S3Config to initialize.
 */
public class S3ConfigInitialization implements TypeListener {

	private final static Logger logger = LoggerFactory.getLogger(S3ConfigInitialization.class);

	private final ApplyOnce<S3Config, Void> initOnce = new ApplyOnce<>(
			new Function<S3Config, Void>() {
				@Override
				public Void apply(S3Config S3Config) {
					logger.info("Bootstrapping S3Config...");
					S3Config.initialize();
					return null;
				}
			});

	@Override
	public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
		encounter.register(new InjectionListener<I>() {
			@Override
			public void afterInjection(Object instance) {
				S3Config S3Config = (S3Config) instance;
				initOnce.apply(S3Config);
			}
		});

	}

	public static AbstractMatcher<TypeLiteral<?>> buildTypeMatcher() {
		return new AbstractMatcher<TypeLiteral<?>>() {
			@Override
			public boolean matches(TypeLiteral<?> typeLiteral) {
				return S3Config.class.isAssignableFrom(typeLiteral.getRawType());
			}
		};
	}

}
