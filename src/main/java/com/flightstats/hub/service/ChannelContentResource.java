package com.flightstats.hub.service;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.flightstats.hub.app.config.metrics.PerChannelTimed;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.Request;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.LinkedContent;
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

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * This resource represents a single value stored in the Hub.
 */
@Path("/channel/{channelName: .*}/{id}")
public class ChannelContentResource {

	private final UriInfo uriInfo;
	private final ChannelService channelService;
	private final DateTimeFormatter dateTimeFormatter = ISODateTimeFormat.dateTime().withZoneUTC();

	@Inject
	public ChannelContentResource(UriInfo uriInfo, ChannelService channelService) {
		this.uriInfo = uriInfo;
		this.channelService = channelService;
	}

    //todo - gfm - 1/22/14 - would be nice to have a head method, which doesn't fetch the body.

	@GET
	@Timed(name = "all-channels.fetch")
	@PerChannelTimed(operationName = "fetch", channelNameParameter = "channelName")
    @ExceptionMetered
	public Response getValue(@PathParam("channelName") String channelName, @PathParam("id") String id,
                             @HeaderParam("Accept") String accept, @HeaderParam("User") String user
    ) {
        Request request = Request.builder()
                .channel(channelName)
                .id(id)
                .user(user)
                .uri(uriInfo.getRequestUri())
                .build();
        Optional<LinkedContent> optionalResult = channelService.getValue(request);

		if (!optionalResult.isPresent()) {
			throw new WebApplicationException(Response.Status.NOT_FOUND);
		}
		LinkedContent linkedContent = optionalResult.get();

		MediaType actualContentType = getContentType(linkedContent);

		if (contentTypeIsNotCompatible(accept, actualContentType)) {
			return Responses.notAcceptable().build();
		}

        Content content = linkedContent.getValue();
        Response.ResponseBuilder builder = Response.status(Response.Status.OK)
												   .type(actualContentType)
												   .entity(linkedContent.getData())
												   .header(Headers.CREATION_DATE,
                                                           dateTimeFormatter.print(new DateTime(content.getMillis())));

        ChannelLinkBuilder.addOptionalHeader(Headers.USER, content.getUser(), builder);
        ChannelLinkBuilder.addOptionalHeader(Headers.LANGUAGE, content.getContentLanguage(), builder);

		addPreviousLink(linkedContent, builder);
		addNextLink(linkedContent, builder);
		return builder.build();
	}



	private MediaType getContentType(LinkedContent compositeValue) {
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

	private void addPreviousLink(LinkedContent compositeValue, Response.ResponseBuilder builder) {
		addLink(builder, "previous", compositeValue.getPrevious());
	}

	private void addNextLink(LinkedContent compositeValue, Response.ResponseBuilder builder) {
		addLink(builder, "next", compositeValue.getNext());
	}

	private void addLink(Response.ResponseBuilder builder, String type, Optional<ContentKey> key) {
		if (key.isPresent()) {
			URI linkUrl = URI.create(uriInfo.getRequestUri().resolve(".") + key.get().keyToString());
			builder.header("Link", "<" + linkUrl + ">;rel=\"" + type + "\"");
		}
	}


}
