package com.flightstats.datahub.service;

import com.flightstats.datahub.cluster.ChannelLockFactory;
import com.flightstats.datahub.dao.ChannelDao;
import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.model.LinkedDataHubCompositeValue;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * Remove any items from persistence that are beyond their TTL (time to live).
 */
public class DataHubSweeper {

	private final static Logger logger = LoggerFactory.getLogger(DataHubSweeper.class);

	private static final Long DEFAULT_SWEEP_PERIOD = TimeUnit.MINUTES.toMillis(5);
	private final Timer sweeper;

	@Inject
	public DataHubSweeper(@Named("sweeper.periodMillis") Long sweepPeriodMillis, ChannelDao dao,
	                      ChannelLockFactory channelLockFactory ) {
		sweepPeriodMillis = sweepPeriodMillis == null ? DEFAULT_SWEEP_PERIOD : sweepPeriodMillis;

		sweeper = new Timer(true);
		sweeper.scheduleAtFixedRate(new SweeperTask(dao, channelLockFactory), sweepPeriodMillis, sweepPeriodMillis);
	}

	// This naive implementation bothers me a bit:
	//  - It causes bursts of activity at each period rather than distributing cleanup more evenly.
	//  - Every member of the cluster cleans every channel rather than distributing the work.
	static class SweeperTask extends TimerTask {

		private final ChannelDao dao;
		private final ChannelLockFactory channelLockFactory;

		SweeperTask(ChannelDao dao, ChannelLockFactory channelLockFactory) {
			this.dao = dao;
			this.channelLockFactory = channelLockFactory;
		}

		@Override
		public void run() {
			for (ChannelConfiguration channelConfiguration : dao.getChannels()) {
				try {
					if (channelConfiguration.getTtlMillis() == null) continue;
					logger.info("Sweeping channel: " + channelConfiguration.getName());

					String channelName = channelConfiguration.getName();
					Date reapDate = new Date(System.currentTimeMillis() - channelConfiguration.getTtlMillis());

					Lock lock = channelLockFactory.newLock(channelName);
					lock.lock();
					try {
						List<DataHubKey> reapableKeys = findReapableKeys(channelName, reapDate);
						fixChannelPointers(channelName, reapableKeys);
						reapValues(channelName, reapableKeys);
					}
					finally {
						lock.unlock();
					}
				} catch ( Throwable t ) {

					// Don't let anything escape or it will negate the parent TimerTask and we'll never run again.
					logger.error( "Failure in sweeper for channel " + channelConfiguration.getName(), t );
				}
			}
		}

		private void fixChannelPointers(String channelName, List<DataHubKey> reapableKeys) {
			if (reapableKeys.isEmpty()) return;

			DataHubKey lastReapKey = reapableKeys.get(reapableKeys.size() - 1);
			Optional<LinkedDataHubCompositeValue> lastReapKeyValue = dao.getValue(channelName, lastReapKey);
			if (lastReapKeyValue.get().hasNext()) {
				dao.setFirstKey(channelName, lastReapKeyValue.get().getNext().get());
			} else {
				// Every item in the channel was reaped.
				dao.deleteFirstKey(channelName);
				dao.deleteLastUpdateKey(channelName);
			}
		}

		private void reapValues(String channelName, List<DataHubKey> reapableKeys) {
			dao.delete( channelName, reapableKeys );
		}

		private List<DataHubKey> findReapableKeys(String channelName, Date reapDate) {
			List<DataHubKey> toBeReaped = new ArrayList<>();
			Optional<DataHubKey> reapCandidate = dao.findFirstId(channelName);
			while (isReapable(reapCandidate, reapDate)) {
				toBeReaped.add(reapCandidate.get());
				reapCandidate = getNextKey(channelName, reapCandidate);
			}
			logger.info("Sweeping " + toBeReaped.size() + " entries from " + channelName );
			return toBeReaped;
		}

		private boolean isReapable(Optional<DataHubKey> currentKey, Date reapDate) {
			return currentKey.isPresent() && currentKey.get().getDate().before(reapDate);
		}

		private Optional<DataHubKey> getNextKey(String channelName, Optional<DataHubKey> currentKey) {
			Optional<LinkedDataHubCompositeValue> currentKeyValue = dao.getValue(channelName, currentKey.get());
			return currentKeyValue.get().getNext();
		}
	}
}
