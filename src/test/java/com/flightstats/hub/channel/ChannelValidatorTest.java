package com.flightstats.hub.channel;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.exception.ConflictException;
import com.flightstats.hub.exception.ForbiddenRequestException;
import com.flightstats.hub.exception.InvalidRequestException;
import com.flightstats.hub.model.ChannelConfig;
import com.google.common.base.Strings;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ChannelValidatorTest {

    private ChannelService channelService;
    private ChannelValidator validator;

    @Before
    public void setUp() throws Exception {
        channelService = mock(ChannelService.class);
        validator = new ChannelValidator(channelService);
        when(channelService.channelExists(any(String.class))).thenReturn(false);
        HubProperties.setProperty("hub.protect.channels", "false");
    }

    @Test
    public void testAllGood() throws Exception {
        validator.validate(getBuilder().name(Strings.repeat("A", 48)).build(), null, false);
    }

    @Test(expected = InvalidRequestException.class)
    public void testTooLong() throws Exception {
        validator.validate(getBuilder().name(Strings.repeat("A", 49)).build(), null, false);
    }

    @Test(expected = InvalidRequestException.class)
    public void testChannelNameNull() throws Exception {
        validator.validate(getBuilder().name(null).build(), null, false);
    }

    @Test(expected = InvalidRequestException.class)
    public void testChannelNameEmpty() throws Exception {
        validator.validate(getBuilder().name("").build(), null, false);
    }

    @Test(expected = InvalidRequestException.class)
    public void testChannelNameBlank() throws Exception {
        validator.validate(getBuilder().name("  ").build(), null, false);
    }

    @Test(expected = ConflictException.class)
    public void testChannelExists() throws Exception {
        String channelName = "achannel";
        when(channelService.channelExists(channelName)).thenReturn(true);
        ChannelConfig channelConfig = getBuilder().name(channelName).build();
        validator.validate(channelConfig, null, false);
    }

    @Test(expected = InvalidRequestException.class)
    public void testInvalidSpaceCharacter() throws Exception {
        validator.validate(getBuilder().name("my chan").build(), null, false);
    }

    @Test
    public void testValidUnderscore() throws Exception {
        validator.validate(getBuilder().name("my_chan").build(), null, false);
    }

    @Test
    public void testValidHyphen() throws Exception {
        validator.validate(getBuilder().name("my-chan").build(), null, false);
    }

    @Test(expected = InvalidRequestException.class)
    public void testInvalidCharacter() throws Exception {
        validator.validate(getBuilder().name("my#chan").build(), null, false);
    }

    @Test(expected = InvalidRequestException.class)
    public void testInvalidChannelTtlMax() throws Exception {
        validator.validate(getBuilder()
                .name("mychan")
                .ttlDays(10)
                .maxItems(10)
                .build(), null, false);
    }

    @Test(expected = InvalidRequestException.class)
    public void testInvalidChannelTtlMutable() throws Exception {
        validator.validate(getBuilder()
                .name("mychan")
                .ttlDays(10)
                .mutableTime(new DateTime())
                .build(), null, false);
    }

    @Test(expected = InvalidRequestException.class)
    public void testInvalidChannelMaxMutable() throws Exception {
        validator.validate(getBuilder()
                .name("mychan")
                .mutableTime(new DateTime())
                .maxItems(10)
                .build(), null, false);
    }

    @Test(expected = InvalidRequestException.class)
    public void testInvalidChannelMutableTime() throws Exception {
        validator.validate(getBuilder()
                .name("mychan")
                .mutableTime(new DateTime().plusMinutes(1))
                .build(), null, false);
    }

    @Test
    public void testMutableTime() throws Exception {
        validator.validate(getBuilder()
                .name("mychan")
                .mutableTime(new DateTime())
                .build(), null, false);
    }

    @Test
    public void testKeepForever() throws Exception {
        validator.validate(getBuilder()
                .name("mychan")
                .keepForever(true)
                .build(), null, false);
    }

    @Test
    public void testMutableTimeForward() throws Exception {
        ChannelConfig first = getBuilder().name("mychan").mutableTime(new DateTime().minusDays(2)).build();
        ChannelConfig second = first.toBuilder().mutableTime(new DateTime().minusDays(1)).build();
        validator.validate(first, second, false);
        validator.validate(first, first, false);
        validateError(second, first);
    }

    @Test(expected = InvalidRequestException.class)
    public void testMutableTimeFuture() throws Exception {
        validator.validate(getBuilder()
                .name("testMutableTimeFuture")
                .mutableTime(new DateTime().plusMinutes(1))
                .build(), null, false);
    }

    @Test(expected = InvalidRequestException.class)
    public void testInvalidBatchMutable() throws Exception {
        validator.validate(getBuilder()
                .name("mychan")
                .storage(ChannelConfig.BATCH)
                .mutableTime(new DateTime())
                .build(), null, false);
    }

    @Test(expected = InvalidRequestException.class)
    public void testInvalidBothMutable() throws Exception {
        validator.validate(getBuilder()
                .name("mychan")
                .storage(ChannelConfig.BOTH)
                .mutableTime(new DateTime())
                .build(), null, false);
    }

    @Test
    public void testDescription1024() throws Exception {
        validator.validate(getBuilder().name("desc").description(Strings.repeat("A", 1024)).build(), null, false);
    }

    @Test(expected = InvalidRequestException.class)
    public void testDescriptionTooBig() throws Exception {
        validator.validate(getBuilder().name("toobig").description(Strings.repeat("A", 1025)).build(), null, false);
    }

    @Test(expected = InvalidRequestException.class)
    public void testTagSpace() throws Exception {
        validator.validate(getBuilder().name("space").tags(Arrays.asList("s p a c e")).build(), null, false);
    }

    @Test(expected = InvalidRequestException.class)
    public void testTagUnderscore() throws Exception {
        validator.validate(getBuilder().name("underscore").tags(Arrays.asList("under_score")).build(), null, false);
    }

    @Test
    public void testTagValid() throws Exception {
        validator.validate(getBuilder().name("valid1").tags(Arrays.asList("abcdefghijklmnopqrstuvwxyz")).build(), null, false);
        validator.validate(getBuilder().name("valid2").tags(Arrays.asList("ABCDEFGHIJKLMNOPQRSTUVWXYZ123456789")).build(), null, false);
    }

    @Test(expected = InvalidRequestException.class)
    public void testTagTooLong() throws Exception {
        validator.validate(getBuilder().name("tooLongTag")
                .tags(Arrays.asList(Strings.repeat("A", 49))).build(), null, false);
    }

    @Test(expected = InvalidRequestException.class)
    public void testTooManyTags() throws Exception {
        List<String> tags = new ArrayList<>();
        for (int i = 0; i < 21; i++) {
            tags.add("" + i);
        }
        validator.validate(getBuilder().name("tooManyTags")
                .tags(tags).build(), null, false);
    }

    @Test
    public void testAllowColonAndDash() throws Exception {
        validator.validate(getBuilder().name("colon").tags(Arrays.asList("a:b")).build(), null, false);
        validator.validate(getBuilder().name("dash").tags(Arrays.asList("a-b")).build(), null, false);
        validator.validate(getBuilder().name("colondash").tags(Arrays.asList("a-b:c")).build(), null, false);
    }

    @Test
    public void testReplicatedTag() {
        ChannelConfig config = getBuilder().replicationSource("http://nowhere").build();
        assertTrue(config.getTags().contains("replicated"));
    }

    @Test
    public void testOwner() throws Exception {
        validator.validate(getBuilder().name("A").owner(Strings.repeat("A", 48)).build(), null, false);
    }

    @Test(expected = InvalidRequestException.class)
    public void testTooLongOwner() throws Exception {
        validator.validate(getBuilder().name("A").owner(Strings.repeat("A", 49)).build(), null, false);
    }

    @Test
    public void testValidStorage() {
        validator.validate(getBuilder().name("storage").storage(ChannelConfig.SINGLE).build(), null, false);
        validator.validate(getBuilder().name("storage").storage("batch").build(), null, false);
        validator.validate(getBuilder().name("storage").storage("BoTh").build(), null, false);
    }

    @Test(expected = InvalidRequestException.class)
    public void testInvalidStorage() {
        validator.validate(getBuilder().name("storage").storage("stuff").build(), null, false);
    }

    @Test
    public void testChangeStorageLoss() throws Exception {
        HubProperties.setProperty("hub.protect.channels", "true");
        ChannelConfig single = getBuilder().name("storage").storage(ChannelConfig.SINGLE).build();
        ChannelConfig batch = getBuilder().name("storage").storage(ChannelConfig.BATCH).build();
        ChannelConfig both = getBuilder().name("storage").storage(ChannelConfig.BOTH).build();
        validator.validate(both, single, false);
        validator.validate(both, batch, false);
        validateError(single, both);
        validateError(batch, both);
        validateError(single, batch);
        validateError(batch, single);
    }

    private ChannelConfig.ChannelConfigBuilder getBuilder() {
        return ChannelConfig.builder().owner("pwnd");
    }

    @Test
    public void testRemoveTagsLoss() throws Exception {
        HubProperties.setProperty("hub.protect.channels", "true");
        ChannelConfig oneTwo = getBuilder().name("testRemoveTags").tags(Arrays.asList("one", "two")).build();
        ChannelConfig oneThree = getBuilder().name("testRemoveTags").tags(Arrays.asList("one", "three")).build();
        ChannelConfig oneTwoThree = getBuilder().name("testRemoveTags").tags(Arrays.asList("one", "two", "three")).build();
        validator.validate(oneTwo, oneTwo, false);
        validator.validate(oneTwoThree, oneTwo, false);
        validateError(oneTwo, oneThree);
    }

    @Test
    public void testTtlDaysLoss() throws Exception {
        HubProperties.setProperty("hub.protect.channels", "true");
        ChannelConfig ten = getBuilder().name("testTtlDays").ttlDays(10).build();
        ChannelConfig eleven = getBuilder().name("testTtlDays").ttlDays(11).build();
        validator.validate(ten, ten, false);
        validateError(ten, eleven);
    }

    @Test
    public void testMaxItemsLoss() throws Exception {
        HubProperties.setProperty("hub.protect.channels", "true");
        ChannelConfig ten = getBuilder().name("testMaxItems").maxItems(10).build();
        ChannelConfig eleven = getBuilder().name("testMaxItems").maxItems(11).build();
        validator.validate(ten, ten, false);
        validateError(ten, eleven);
    }

    @Test
    public void testReplicationLoss() throws Exception {
        HubProperties.setProperty("hub.protect.channels", "true");
        ChannelConfig changed = getBuilder().name("testReplication").replicationSource("http://hub/channel/name1").build();
        ChannelConfig replication = getBuilder().name("testReplication").replicationSource("http://hub/channel/name").build();
        validator.validate(changed, changed, false);
        validator.validate(replication, replication, false);
        validateError(changed, replication);
    }

    @Test
    public void testDataLossChange() throws Exception {
        HubProperties.setProperty("hub.protect.channels", "false");
        ChannelConfig dataLoss = getBuilder().name("testDataLossChange").protect(false).build();
        ChannelConfig noLoss = getBuilder().name("testDataLossChange").protect(true).build();

        validateError(dataLoss, noLoss);
        validator.validate(noLoss, dataLoss, false);
        validator.validate(dataLoss, noLoss, true);

    }

    private void validateError(ChannelConfig config, ChannelConfig oldConfig) {
        try {
            validator.validate(config, oldConfig, false);
            fail("expected exception");
        } catch (ForbiddenRequestException | InvalidRequestException e) {
            //this is expected
        }
    }


}
