package com.flightstats.hub.service;

public class LatestChannelItemResourceTest {

    //todo - gfm - 10/28/14 - kill?
/*	@Test
    public void testGetLatest() throws Exception {
		String channelName = "fooChan";
		ContentKey key = new ContentKey(1000);

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
	}*/
}
