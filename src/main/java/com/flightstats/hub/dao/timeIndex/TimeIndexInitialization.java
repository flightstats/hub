package com.flightstats.hub.dao.timeIndex;

import com.google.inject.TypeLiteral;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a guice binding via TypeListener.  It provides a means for the
 * TimeIndexCoordinator to initialize.
 *
 */
public class TimeIndexInitialization implements TypeListener {

    private final static Logger logger = LoggerFactory.getLogger(TimeIndexInitialization.class);

    @Override
    public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
        encounter.register(new InjectionListener<I>() {
            @Override
            public void afterInjection(Object instance) {
                logger.info("Bootstrapping Replication...");
                ((TimeIndexCoordinator)instance).startThread();
            }
        });
    }

    public static AbstractMatcher<TypeLiteral<?>> buildTypeMatcher() {
        return new AbstractMatcher<TypeLiteral<?>>() {
            @Override
            public boolean matches(TypeLiteral<?> typeLiteral) {
                return TimeIndexCoordinator.class.isAssignableFrom(typeLiteral.getRawType());
            }
        };
    }

}
