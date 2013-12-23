package com.flightstats.datahub.service;

import com.flightstats.datahub.dao.ChannelService;
import com.flightstats.datahub.model.ContentKey;
import com.flightstats.datahub.model.SequenceContentKey;
import com.flightstats.datahub.util.DataHubKeyRenderer;
import com.google.common.base.Optional;
import org.junit.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LatestChannelItemResourceTest {

	@Test
	public void testGetLatest() throws Exception {
		String channelName = "fooChan";
		ContentKey key = new SequenceContentKey(1000);
		DataHubKeyRenderer keyRenderer = new DataHubKeyRenderer();

		UriInfo uriInfo = mock(UriInfo.class);
		ChannelService channelService = mock(ChannelService.class);

		when(channelService.findLastUpdatedKey(channelName)).thenReturn(Optional.of(key));
		when(uriInfo.getRequestUri()).thenReturn(URI.create("http://path/to/channel/lolcats/latest"));

		LatestChannelItemResource testClass = new LatestChannelItemResource(uriInfo, channelService);

		Response response = testClass.getLatest(channelName);
		assertEquals(Response.Status.SEE_OTHER.getStatusCode(), response.getStatus());
		List<Object> locations = response.getMetadata().get("Location");
		assertEquals(1, locations.size());
		assertEquals(URI.create("http://path/to/channel/lolcats/" + key.keyToString()), locations.get(0));
	}

	@Test
	public void testGetLatest_channelEmpty() throws Exception {
		String channelName = "fooChan";

		ChannelService channelService = mock(ChannelService.class);

		when(channelService.findLastUpdatedKey(channelName)).thenReturn(Optional.<ContentKey>absent());

		LatestChannelItemResource testClass = new LatestChannelItemResource(null, channelService);

        Response response = testClass.getLatest(channelName);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
	}
}
