package com.flightstats.hub.filter;

import com.google.common.base.Joiner;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;

/**
 * This exists to prevent gzip & deflate encodings for event-streams.
 */
@PreMatching
@Slf4j
public final class StreamEncodingFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        MediaType contentType = (MediaType) response.getHeaders().getFirst("Content-Type");
        if (contentType != null && StringUtils.contains(contentType.getSubtype(), "event-stream")) {
            handleEncoding(request);
        }
    }

    private void handleEncoding(ContainerRequestContext request) {
        List<String> acceptEncoding = request.getHeaders().get(HttpHeaders.ACCEPT_ENCODING);
        log.trace("acceptEncoding {}", acceptEncoding);
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
            log.debug("removing from events {} ", allowedEncoding);
            if (allowedEncoding.isEmpty()) {
                request.getHeaders().remove(HttpHeaders.ACCEPT_ENCODING);
            } else {
                request.getHeaders().put(HttpHeaders.ACCEPT_ENCODING, allowedEncoding);
            }
        }
    }


}
