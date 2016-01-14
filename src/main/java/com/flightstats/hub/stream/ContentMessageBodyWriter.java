package com.flightstats.hub.stream;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.channel.MultiPartBulkBuilder;
import org.glassfish.jersey.media.sse.SseFeature;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;

@Produces("text/event-stream")
@Provider
public class ContentMessageBodyWriter implements MessageBodyWriter<StreamContent> {

    private static final URI APP_URL = UriBuilder.fromPath(HubProperties.getAppUrl()).build();

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == StreamContent.class && mediaType.equals(SseFeature.SERVER_SENT_EVENTS_TYPE);
    }

    @Override
    public long getSize(StreamContent content, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(StreamContent content, Class<?> type, Type genericType, Annotation[] annotations,
                        MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream output)
            throws IOException, WebApplicationException {
        URI uri = UriBuilder.fromUri(APP_URL).path("channel/" + content.getChannel()).build();
        MultiPartBulkBuilder.writeContent(content.getContent(), output, uri, content.getChannel(),
                content.isFirst(), true);
    }
}
