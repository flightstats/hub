package com.flightstats.datahub.service;

import com.flightstats.datahub.dao.ChannelDao;
import com.flightstats.datahub.model.DataHubCompositeValue;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.util.DataHubKeyRenderer;
import com.google.inject.Inject;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * This resource represents a single value stored in the DataHub.
 */
@Path("/channel/{channelName: .*}/{id}")
public class ChannelContentResource {

    private final ChannelDao channelDao;
    private final DataHubKeyRenderer dataHubKeyRenderer;

    @Inject
    public ChannelContentResource(ChannelDao channelDao, DataHubKeyRenderer dataHubKeyRenderer) {
        this.channelDao = channelDao;
        this.dataHubKeyRenderer = dataHubKeyRenderer;
    }

    @GET
    public Response getValue(@PathParam("channelName") String channelName, @PathParam("id") String id) {
        DataHubKey key = dataHubKeyRenderer.fromString(id);
        DataHubCompositeValue columnValue = channelDao.getValue(channelName, key);
        if (columnValue == null) {
            //TODO: dont throw here???
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        Response.ResponseBuilder builder = Response.status(Response.Status.OK);

        String contentType = columnValue.getContentType();
        // Only if we had a content type stored with the data do we specify one here.
        // If unspecified, the framework will default to application/octet-stream
        if (!isNullOrEmpty(contentType)) {
            builder.type(contentType);
        }
        builder.entity(columnValue.getData());
        return builder.build();
    }


}
