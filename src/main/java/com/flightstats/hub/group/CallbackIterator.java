package com.flightstats.hub.group;

import com.flightstats.hub.cluster.SingleWatcher;
import com.flightstats.hub.cluster.Watcher;
import com.flightstats.hub.dao.SequenceLastUpdatedDao;
import com.flightstats.hub.util.RuntimeInterruptedException;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.api.CuratorEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class CallbackIterator implements Iterator<Long>, AutoCloseable {

    private final static Logger logger = LoggerFactory.getLogger(CallbackIterator.class);
    private final Object lock = new Object();

    private final AtomicLong latest = new AtomicLong(0);
    private long current;
    private Group group;
    private AtomicBoolean shouldExit = new AtomicBoolean(false);
    //todo - gfm - 6/5/14 - should this use the interface instead?
    private final SequenceLastUpdatedDao sequenceKey;
    private final SingleWatcher singleWatcher;

    @Inject
    public CallbackIterator(SequenceLastUpdatedDao sequenceKey, SingleWatcher singleWatcher) {
        this.sequenceKey = sequenceKey;
        this.singleWatcher = singleWatcher;
    }

    @Override
    public boolean hasNext() {
        while (!shouldExit.get()) {
            if (current < latest.get()) {
                current++;
                return true;
            }
            synchronized (lock) {
                try {
                    lock.wait(TimeUnit.MINUTES.toMillis(5));
                } catch (InterruptedException e) {
                    throw new RuntimeInterruptedException(e);
                }
            }
        }
        return false;
    }

    @Override
    public Long next() {
        return current;
    }

    public void start(long lastCompleted, Group group) {
        this.current = lastCompleted;
        this.group = group;
        addWatcher();
    }

    private void addWatcher() {
        final String channelName = StringUtils.substringAfter(group.getChannelUrl(), "/channel/");
        singleWatcher.register(new Watcher() {
            @Override
            public void callback(CuratorEvent event) {
                long sequence = sequenceKey.getLongValue(channelName);
                logger.debug("latest sequence {}", sequence);
                if (sequence > latest.get()) {
                    latest.set(sequence);
                    signal();
                }
            }

            @Override
            public String getPath() {
                return sequenceKey.getPath(channelName);
            }
        });
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("don't call this");
    }

    private void signal() {
        synchronized (lock) {
            lock.notify();
        }
    }

    @VisibleForTesting
    long getCurrent() {
        return current;
    }

    @Override
    public void close() {
        shouldExit.set(true);
        singleWatcher.unregister();
        signal();
    }
}
