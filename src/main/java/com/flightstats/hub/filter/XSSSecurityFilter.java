package com.flightstats.hub.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import com.flightstats.hub.util.XSSSecurity;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.owasp.esapi.ESAPI;

import lombok.extern.slf4j.Slf4j;

@Provider
@Slf4j
public class XSSSecurityFilter implements ContainerRequestFilter {
    @Override
    public void filter(ContainerRequestContext request) throws IOException {
        cleanParameters(request.getUriInfo().getQueryParameters()) ;
        cleanParameters(request.getHeaders());

        log.debug("XSS-secured...");
        log.debug(request.getUriInfo().getQueryParameters().toString());
        log.debug(request.getHeaders().toString());
    }

    private void cleanParameters(MultivaluedMap<String, String> parameters) {
        for(Map.Entry<String, List<String>> params : parameters.entrySet()) {
            String key = params.getKey();
            List<String> values = params.getValue();

            List<String> cleanValues = new ArrayList<>();
            for(String value: values)
            {
                cleanValues.add(new XSSSecurity(value).strip());
            }

            parameters.put(key, cleanValues);
        }
    }
}
