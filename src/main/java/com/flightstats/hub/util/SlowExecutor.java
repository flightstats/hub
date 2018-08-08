package com.flightstats.hub.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class SlowExecutor {

    private static final Logger logger = LoggerFactory.getLogger(SlowExecutor.class);
    private static Set<String> current = new HashSet<>();
    private static ExecutorService executorService = Executors.newCachedThreadPool();

    static synchronized void runAsync(String name, Runnable runnable) {
        if (current.contains(name)) {
            logger.info("ignoring already running {}", name);
            return;
        }

        executorService.submit(() -> {
            try {
                current.add(name);
                logger.info("running {}", name);
                runnable.run();
                logger.info("removing {}", name);
            } finally {
                current.remove(name);
            }
        });

    }
}
