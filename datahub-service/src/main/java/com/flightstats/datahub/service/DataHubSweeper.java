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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Leaving this Resource around to prevent breaking tests for now.
 * It should probably go away.
 */
@Path("/sweep")
public class DataHubSweeper {

	@POST
	@Timed
	@Produces(MediaType.APPLICATION_JSON)
	public Response sweep() {
        return Response.ok().build();
	}
}
