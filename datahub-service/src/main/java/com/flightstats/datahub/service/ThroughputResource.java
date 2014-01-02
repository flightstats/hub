package com.flightstats.datahub.service;

import com.flightstats.datahub.app.ThroughputDriver;
import com.flightstats.datahub.dao.ChannelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

/**
 * todo - gfm - 1/2/14 - This is temporary, and should go away before production.
 */
@Path("/throughput/{rate}/{factor}")
public class ThroughputResource {

    private final static Logger logger = LoggerFactory.getLogger(ThroughputResource.class);

	private final ChannelService channelService;

	@Inject
	public ThroughputResource(ChannelService channelService) {
		this.channelService = channelService;
    }

	@GET
	public Response getChannels(@PathParam("rate") int rate, @PathParam("factor") double factor) {
        logger.info("starting with rate " + rate + " factor " + factor);
        ThroughputDriver driver = new ThroughputDriver(rate, factor, channelService);
        driver.start();
        return Response.ok().build();
	}


}
