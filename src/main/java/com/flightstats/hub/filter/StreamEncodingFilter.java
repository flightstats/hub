package com.flightstats.hub.filter;

import com.google.common.base.Joiner;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This exists to prevent gzip & deflate encodings for event-streams.
 */
@PreMatching
public final class StreamEncodingFilter implements ContainerResponseFilter {

    private final static Logger logger = LoggerFactory.getLogger(StreamEncodingFilter.class);

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) throws IOException {
        MediaType contentType = (MediaType) response.getHeaders().getFirst("Content-Type");
        if (contentType != null && StringUtils.contains(contentType.getSubtype(), "event-stream")) {
            handleEncoding(request);
        }
    }

    private void handleEncoding(ContainerRequestContext request) {
        List<String> acceptEncoding = request.getHeaders().get(HttpHeaders.ACCEPT_ENCODING);
        logger.trace("acceptEncoding {}", acceptEncoding);
        List<String> allowedEncoding = new ArrayList<>();
        if (acceptEncoding != null) {
            for (String encoding : acceptEncoding) {
                List<String> innerEncoding = new ArrayList<>();
                for (String token : encoding.split(",")) {
                    token = token.trim();
                    if (!FilterUtils.removedEncodings.contains(token)) {
                        innerEncoding.add(token);
                    }
                }
                String joined = Joiner.on(",").join(innerEncoding);
                if (StringUtils.isNotBlank(joined)) {
                    allowedEncoding.add(joined);
                }
            }
            logger.debug("removing from events {} ", allowedEncoding);
            if (allowedEncoding.isEmpty()) {
                request.getHeaders().remove(HttpHeaders.ACCEPT_ENCODING);
            } else {
                request.getHeaders().put(HttpHeaders.ACCEPT_ENCODING, allowedEncoding);
            }
        }
    }


}
