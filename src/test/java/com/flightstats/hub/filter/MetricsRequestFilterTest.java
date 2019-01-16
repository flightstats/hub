package com.flightstats.hub.filter;

import com.flightstats.hub.test.Integration;
import org.glassfish.jersey.server.internal.routing.UriRoutingContext;
import org.glassfish.jersey.uri.UriTemplate;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.container.ContainerRequestContext;
import java.util.Collections;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class MetricsRequestFilterTest {
    @BeforeClass
    public static void integrationSetup() {
        try {
            Integration.startAwsHub();
        } catch(Exception e) {
            Logger logger = LoggerFactory.getLogger(MetricsRequestFilterTest.class);
            logger.error("failed to start the hub ", e);
        }
    }

    @Test
    public void testGetRequestTemplate() {
        // GIVEN
        UriTemplate uriTemplate = new UriTemplate("/{Y}/{M}/{D}/{h}/{m}/{s}/{ms}/{hash}/{direction:[n|p].*}/{count:.+}");
        UriRoutingContext uriInfo = mock(UriRoutingContext.class);
        ContainerRequestContext request = mock(ContainerRequestContext.class);
        when(request.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getMatchedTemplates()).thenReturn(Collections.singletonList(uriTemplate));

        // WHEN
        String filteredRequestTemplate = MetricsRequestFilter.getRequestTemplate(request);

        // THEN
        assertEquals("/_Y_/_M_/_D_/_h_/_m_/_s_/_ms_/_hash_/_direction_np_/_count__", filteredRequestTemplate);
    }
}