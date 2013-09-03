package com.flightstats.datahub.service;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.flightstats.datahub.app.config.metrics.PerChannelTimed;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.model.LinkedDataHubCompositeValue;
import com.flightstats.datahub.util.DataHubKeyRenderer;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.sun.jersey.api.Responses;
import com.sun.jersey.core.header.MediaTypes;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;

import static com.flightstats.datahub.service.CustomHttpHeaders.CREATION_DATE_HEADER;
import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * This resource represents a single value stored in the DataHub.
 */
@Path("/channel/{channelName: .*}/{id}")
public class ChannelContentResource {

	private final UriInfo uriInfo;
	private final DataHubService dataHubService;
	private final DataHubKeyRenderer keyRenderer;
	private final DateTimeFormatter dateTimeFormatter = ISODateTimeFormat.dateTime().withZoneUTC();

	@Inject
	public ChannelContentResource(UriInfo uriInfo, DataHubService dataHubService, DataHubKeyRenderer keyRenderer) {
		this.uriInfo = uriInfo;
		this.dataHubService = dataHubService;
		this.keyRenderer = keyRenderer;
	}

	@GET
	@Timed(name = "all-channels.fetch")
	@PerChannelTimed(operationName = "fetch", channelNamePathParameter = "channelName")
    @ExceptionMetered
	public Response getValue(@PathParam("channelName") String channelName, @PathParam("id") String id, @HeaderParam("Accept") String accept) {
		DataHubKey key = keyRenderer.fromString(id);
		Optional<LinkedDataHubCompositeValue> optionalResult = dataHubService.getValue(channelName, key);

		if (!optionalResult.isPresent()) {
			throw new WebApplicationException(Response.Status.NOT_FOUND);
		}
		LinkedDataHubCompositeValue compositeValue = optionalResult.get();

		MediaType actualContentType = getContentType(compositeValue);

		if (contentTypeIsNotCompatible(accept, actualContentType)) {
			return Responses.notAcceptable().build();
		}

		Response.ResponseBuilder builder = Response.status(Response.Status.OK)
												   .type(actualContentType)
												   .entity(compositeValue.getData())
												   .header(CREATION_DATE_HEADER.getHeaderName(),
														   dateTimeFormatter.print(new DateTime(key.getDate())));

		addOptionalHeader("Content-Encoding", compositeValue.getValue().getContentEncoding(), builder);
		addOptionalHeader("Content-Language", compositeValue.getValue().getContentLanguage(), builder);

		addPreviousLink(compositeValue, builder);
		addNextLink(compositeValue, builder);
		return builder.build();
	}

	private void addOptionalHeader(String headerName, Optional<String> headerValue, Response.ResponseBuilder builder) {
		if(headerValue.isPresent()){
			builder.header(headerName, headerValue.get());
		}
	}

	private MediaType getContentType(LinkedDataHubCompositeValue compositeValue) {
		Optional<String> contentType = compositeValue.getContentType();
		if (contentType.isPresent() && !isNullOrEmpty(contentType.get())) {
			return MediaType.valueOf(contentType.get());
		}
		return MediaType.APPLICATION_OCTET_STREAM_TYPE;
	}

	private boolean contentTypeIsNotCompatible(String acceptHeader, final MediaType actualContentType) {
		List<MediaType> acceptableContentTypes = acceptHeader != null ?
				MediaTypes.createMediaTypes(acceptHeader.split(",")) :
				MediaTypes.GENERAL_MEDIA_TYPE_LIST;

		return !Iterables.any(acceptableContentTypes, new Predicate<MediaType>() {
			@Override
			public boolean apply(MediaType input) {
				return input.isCompatible(actualContentType);
			}
		});
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
