package com.flightstats.datahub.service;

import com.flightstats.datahub.dao.ChannelDao;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.model.LinkedDataHubCompositeValue;
import com.flightstats.datahub.util.DataHubKeyRenderer;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

import static com.flightstats.datahub.service.CustomHttpHeaders.CREATION_DATE_HEADER;
import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * This resource represents a single value stored in the DataHub.
 */
@Path("/channel/{channelName: .*}/{id}")
public class ChannelContentResource {

    private final UriInfo uriInfo;
    private final ChannelDao channelDao;
    private final DataHubKeyRenderer keyRenderer;
    private final DateTimeFormatter dateTimeFormatter = ISODateTimeFormat.dateTime().withZoneUTC();

    @Inject
    public ChannelContentResource(UriInfo uriInfo, ChannelDao channelDao, DataHubKeyRenderer keyRenderer) {
        this.uriInfo = uriInfo;
        this.channelDao = channelDao;
        this.keyRenderer = keyRenderer;
    }

    @GET
    public Response getValue(@PathParam("channelName") String channelName, @PathParam("id") String id) {
        DataHubKey key = keyRenderer.fromString(id);
        Optional<LinkedDataHubCompositeValue> optionalResult = channelDao.getValue(channelName, key);

        if (!optionalResult.isPresent()) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        LinkedDataHubCompositeValue columnValue = optionalResult.get();
        Response.ResponseBuilder builder = Response.status(Response.Status.OK);

        String contentType = columnValue.getContentType();
        // Only if we had a content type stored with the data do we specify one here.
        // If unspecified, the framework will default to application/octet-stream
        if (!isNullOrEmpty(contentType)) {
            builder.type(contentType);
        }
        builder.entity(columnValue.getData());

        builder.header(CREATION_DATE_HEADER.getHeaderName(), dateTimeFormatter.print(new DateTime(key.getDate())));
        addPreviousLink(columnValue, builder);
        return builder.build();
    }

    private void addPreviousLink(LinkedDataHubCompositeValue columnValue, Response.ResponseBuilder builder) {
        Optional<DataHubKey> previous = columnValue.getPrevious();
        if (previous.isPresent()) {
            URI previousUrl = URI.create(uriInfo.getRequestUri().resolve(".") + keyRenderer.keyToString(previous.get()));
            builder.header("Link", "<" + previousUrl + ">;rel=\"previous\"");
        }
    }


}
