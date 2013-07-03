package com.flightstats.datahub.service;

import com.codahale.metrics.annotation.Timed;
import com.flightstats.datahub.dao.ChannelDao;
import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.model.LinkedDataHubCompositeValue;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * Remove any items from persistence that are beyond their TTL (time to live).
 */
@Path("/sweep")
public class DataHubSweeper {

	private final static Logger logger = LoggerFactory.getLogger(DataHubSweeper.class);

	private static final Long DEFAULT_SWEEP_PERIOD = TimeUnit.MINUTES.toMillis(1);
	private final Timer sweeperTimer;
	private final SweeperTask sweeperTask;

	@Inject
	public DataHubSweeper(@Named("sweeper.periodMillis") Long sweepPeriodMillis, ChannelDao dao,
	                      ChannelLockExecutor channelLockExecutor) {
		sweepPeriodMillis = sweepPeriodMillis == null ? DEFAULT_SWEEP_PERIOD : sweepPeriodMillis;

		sweeperTimer = new Timer(true);
		sweeperTask = new SweeperTask(dao, channelLockExecutor);
		sweeperTimer.scheduleAtFixedRate(sweeperTask, sweepPeriodMillis, sweepPeriodMillis);
	}

	// This naive implementation bothers me a bit:
	//  - It causes bursts of activity at each period rather than distributing cleanup more evenly.
	//  - Every member of the cluster cleans every channel rather than distributing the work.
	//  - It's sequential, so it can be slow when there are a lot of channels to check. Alternatively it could spawn
	//    a job per channel, but that might be hard on the system.
	static class SweeperTask extends TimerTask {

		private final ChannelDao dao;
		private final ChannelLockExecutor channelLockExecutor;

		SweeperTask(ChannelDao dao, ChannelLockExecutor channelLockExecutor) {
			this.dao = dao;
			this.channelLockExecutor = channelLockExecutor;
		}

		@Override
		public void run() {
			for (ChannelConfiguration channelConfiguration : dao.getChannels()) {
				try {
					sweepChannel(channelConfiguration);
				} catch (Exception e) {
					// Don't let anything escape or it will negate the parent Timer running this task and we'll never run again.
					logger.error("Failure in sweeper for channel " + channelConfiguration.getName(), e);
				}
			}
		}

		private void sweepChannel(final ChannelConfiguration channelConfiguration) throws Exception {
			if (channelConfiguration.getTtlMillis() == null) return;

			// Reaping has to be in it's own lock or two reaps at the same time could clash with each other over
			// what keys to reap and how that impacts the first/last channel pointers.
			final String channelName = channelConfiguration.getName();
			channelLockExecutor.execute(
				channelName + "_reap",
				new Callable() {
					@Override
					public Void call() throws Exception {
						Date reapDate = new Date(System.currentTimeMillis() - channelConfiguration.getTtlMillis());
						List<DataHubKey> reapableKeys = findReapableKeys(channelName, reapDate);
						if (!reapableKeys.isEmpty()) {
							logger.error("Sweeping " + reapableKeys.size() + " for " + channelName);
							fixPointersForChannel(channelName, reapableKeys);
							reapValues(channelName, reapableKeys);
						}
						return null;
					}
				});
		}

		private void fixPointersForChannel(
			final String channelName, final List<DataHubKey> reapableKeys) throws Exception {

			// Modifying the channel pointers can conflict with incoming inserts, so this step has to be synchronized.
			channelLockExecutor.execute(
				channelName,
				new Callable() {
					@Override
					public Void call() throws Exception {
						if (reapableKeys.isEmpty()) return null;

						DataHubKey lastReapKey = reapableKeys.get(reapableKeys.size() - 1);
						Optional<LinkedDataHubCompositeValue> lastReapKeyValue = dao.getValue(channelName, lastReapKey);
						if ( lastReapKeyValue.isPresent() &&  lastReapKeyValue.get().hasNext()) {
							dao.setFirstKey(channelName, lastReapKeyValue.get().getNext().get());
						} else {
							// Every item in the channel was reaped.
							dao.deleteFirstKey(channelName);
							dao.deleteLastUpdateKey(channelName);
						}
						return null;
					}
				});
		}

		private List<DataHubKey> findReapableKeys(String channelName, Date reapDate) {
			return new ArrayList<>(dao.findKeysInRange(channelName, new Date(0), reapDate));
		}

		private void reapValues(String channelName, List<DataHubKey> reapableKeys) {
			dao.delete(channelName, reapableKeys);
		}
	}

	@POST
	@Timed
	@Produces(MediaType.APPLICATION_JSON)
	public Response sweep() {
		sweeperTask.run();
		return Response.ok().build();
	}
}
