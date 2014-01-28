package com.flightstats.datahub.replication;

import com.google.inject.TypeLiteral;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a guice binding via TypeListener.  It provides a means for the
 * ReplicationDao to initialize (bootstrap) the values table.
 *
 */
public class ReplicationInitialization implements TypeListener {

    private final static Logger logger = LoggerFactory.getLogger(ReplicationInitialization.class);

    @Override
    public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
        encounter.register(new InjectionListener<I>() {
            @Override
            public void afterInjection(Object instance) {
                //todo - gfm - 1/27/14 - could expand this to start other replication bits.
                logger.info("Bootstrapping Replication...");
                ((DynamoReplicationDao)instance).initialize();
            }
        });
    }

    public static AbstractMatcher<TypeLiteral<?>> buildTypeMatcher() {
        return new AbstractMatcher<TypeLiteral<?>>() {
            @Override
            public boolean matches(TypeLiteral<?> typeLiteral) {
                return DynamoReplicationDao.class.isAssignableFrom(typeLiteral.getRawType());
            }
        };
    }

}
