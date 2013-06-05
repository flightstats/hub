package com.flightstats.datahub.service;

import com.flightstats.datahub.dao.ChannelDao;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.model.LinkedDataHubCompositeValue;
import com.flightstats.datahub.util.DataHubKeyRenderer;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.sun.jersey.api.Responses;
import com.sun.jersey.core.header.MediaTypes;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

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
	@Produces
	public Response getValue(@PathParam("channelName") String channelName, @PathParam("id") String id, @HeaderParam( "Accept" ) String accept ) {
		DataHubKey key = keyRenderer.fromString(id);
		Optional<LinkedDataHubCompositeValue> optionalResult = channelDao.getValue(channelName, key);

		if (!optionalResult.isPresent()) {
			throw new WebApplicationException(Response.Status.NOT_FOUND);
		}
		LinkedDataHubCompositeValue compositeValue = optionalResult.get();

		List<MediaType> acceptableContentTypes = accept != null ?
		                                         MediaTypes.createMediaTypes( accept.split( "," ) ) :
		                                         MediaTypes.GENERAL_MEDIA_TYPE_LIST;
		MediaType actualContentType = isNullOrEmpty(compositeValue.getContentType()) ?
		                              MediaType.APPLICATION_OCTET_STREAM_TYPE :
		                              MediaType.valueOf(compositeValue.getContentType());
		if (isCompatibleContentType(acceptableContentTypes, actualContentType)) {
			Response.ResponseBuilder builder = Response.status(Response.Status.OK)
				.type(actualContentType)
				.entity(compositeValue.getData())
				.header(CREATION_DATE_HEADER.getHeaderName(), dateTimeFormatter.print(new DateTime(key.getDate())));
			addPreviousLink(compositeValue, builder);
			addNextLink(compositeValue, builder);
			return builder.build();
		}
		else {
			return Responses.notAcceptable().build();
		}
	}

	private boolean isCompatibleContentType( List<MediaType> acceptableContentTypes, MediaType actualContentType )
	{
		boolean isCompatibleContentType = false;
		for ( MediaType acceptableContentType : acceptableContentTypes ) {
			if ( acceptableContentType.isCompatible( actualContentType ) ) {
				isCompatibleContentType = true;
				break;
			}
		}
		return isCompatibleContentType;
	}

	private void addPreviousLink(LinkedDataHubCompositeValue compositeValue, Response.ResponseBuilder builder) {
		addLink(builder, "previous", compositeValue.getPrevious());
	}

	private void addNextLink(LinkedDataHubCompositeValue compositeValue, Response.ResponseBuilder builder) {
		addLink(builder, "next", compositeValue.getNext());
	}

	private void addLink(Response.ResponseBuilder builder, String type, Optional<DataHubKey> key) {
		if (key.isPresent()) {
			URI linkUrl = URI.create(uriInfo.getRequestUri().resolve(".") + keyRenderer.keyToString(key.get()));
			builder.header("Link", "<" + linkUrl + ">;rel=\"" + type + "\"");
		}
	}


}
