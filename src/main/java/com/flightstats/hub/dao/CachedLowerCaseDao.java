package com.flightstats.hub.dao;

import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.cluster.WatchManager;
import com.flightstats.hub.cluster.Watcher;
import com.flightstats.hub.model.NamedType;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.curator.framework.api.CuratorEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * //todo - gfm - this maybe temporary, until all named cache items are case insensitive.
 */
public class CachedLowerCaseDao<T extends NamedType> implements Dao<T> {

    private final static Logger logger = LoggerFactory.getLogger(CachedLowerCaseDao.class);

    private final Dao<T> delegate;
    private final String path;
    private final WatchManager watchManager;
    private ConcurrentMap<String, T> cacheMap = new ConcurrentHashMap<>();
    private ExecutorService executor = Executors.newSingleThreadExecutor(
            new ThreadFactoryBuilder().setNameFormat("cachedDao-%d").build());

    public CachedLowerCaseDao(Dao<T> delegate, WatchManager watchManager, String path) {
        this.delegate = delegate;
        this.watchManager = watchManager;
        this.path = path;
        HubServices.register(new CachedDaoService());
    }

    @Override
    public void upsert(T t) {
        delegate.upsert(t);
        cacheMap.put(t.getName().toLowerCase(), t);
        notifyWatchers();
    }

    @Override
    public T get(String name) {
        String lowerCase = name.toLowerCase();
        T t = delegate.get(lowerCase);
        if (null != t) {
            cacheMap.put(lowerCase, t);
        }
        return t;
    }

    @Override
    public T getCached(String name) {
        String lowerCase = name.toLowerCase();
        T t = cacheMap.get(lowerCase);
        if (t != null) {
            return t;
        }
        return get(lowerCase);
    }

    @Override
    public Collection<T> getAll(boolean useCache) {
        if (useCache) {
            return cacheMap.values();
        }
        return delegate.getAll(false);
    }

    private void updateMap() {
        logger.trace("updating map {}", cacheMap.keySet());
        ConcurrentMap<String, T> newMap = new ConcurrentHashMap<>();
        Iterable<T> items = delegate.getAll(false);
        for (T named : items) {
            newMap.put(named.getName().toLowerCase(), named);
        }
        cacheMap = newMap;
        logger.trace("updated map {}", newMap.keySet());
    }

    @Override
    public void delete(String name) {
        delegate.delete(name.toLowerCase());
        cacheMap.remove(name.toLowerCase());
        notifyWatchers();
    }

    private void notifyWatchers() {
        watchManager.notifyWatcher(path);
    }

    @Override
    public boolean refresh() {
        updateMap();
        return true;
    }

    private void startWatcher() {
        CachedLowerCaseDao dao = this;
        watchManager.register(new Watcher() {
            @Override
            public void callback(CuratorEvent event) {
                executor.submit(dao::updateMap);
            }

            @Override
            public String getPath() {
                return path;
            }
        });
    }

    private class CachedDaoService extends AbstractIdleService {

        @Override
        protected void startUp() throws Exception {
            startWatcher();
        }

        @Override
        protected void shutDown() throws Exception {
            //do nothing
        }
    }
}
