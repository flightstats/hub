package com.flightstats.hub.channel;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.exception.ConflictException;
import com.flightstats.hub.exception.ForbiddenRequestException;
import com.flightstats.hub.exception.InvalidRequestException;
import com.flightstats.hub.model.ChannelConfig;
import com.google.common.base.Strings;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChannelValidatorTest {

    private ChannelService channelService;
    private ChannelValidator validator;

    @BeforeEach
    void setUp() {
        channelService = mock(ChannelService.class);
        validator = new ChannelValidator(channelService);
        when(channelService.channelExists(any(String.class))).thenReturn(false);
        HubProperties.setProperty("hub.protect.channels", "false");
    }

    @Test
    void testAllGood() {
        try {
            validator.validate(getBuilder().name(Strings.repeat("A", 48)).build(), null, false);
        } catch (Exception ex) {
            assertNull(ex);
        }
    }

    @Test
    void testTooLong() {
        try {
            validator.validate(getBuilder().name(Strings.repeat("A", 49)).build(), null, false);
        } catch (Exception ex) {
            assertEquals(ex.getClass(), InvalidRequestException.class);
        }
    }

    @Test
    void testChannelNameNull() {
        try {
            validator.validate(getBuilder().name(null).build(), null, false);
        } catch (Exception ex) {
            assertEquals(ex.getClass(), InvalidRequestException.class);
        }
    }

    @Test
    void testChannelNameEmpty() {
        try {
            validator.validate(getBuilder().name("").build(), null, false);
        } catch (Exception ex) {
            assertEquals(ex.getClass(), InvalidRequestException.class);
        }
    }

    @Test
    void testChannelNameBlank() {
        try {
            validator.validate(getBuilder().name("  ").build(), null, false);
        } catch (Exception ex) {
            assertEquals(ex.getClass(), InvalidRequestException.class);
        }
    }

    @Test
    void testChannelExists() {
        try {
            String channelName = "achannel";
            when(channelService.channelExists(channelName)).thenReturn(true);
            ChannelConfig channelConfig = getBuilder().name(channelName).build();
            validator.validate(channelConfig, null, false);
        } catch (Exception ex) {
            assertEquals(ex.getClass(), ConflictException.class);
        }
    }

    @Test
    void testInvalidSpaceCharacter() {
        try {
            validator.validate(getBuilder().name("my chan").build(), null, false);
        } catch (Exception ex) {
            assertEquals(ex.getClass(), InvalidRequestException.class);
        }
    }

    @Test
    void testValidUnderscore() {
        validator.validate(getBuilder().name("my_chan").build(), null, false);
    }

    @Test
    void testValidHyphen() {
        validator.validate(getBuilder().name("my-chan").build(), null, false);
    }

    @Test
    void testInvalidCharacter() {
        try {
            validator.validate(getBuilder().name("my#chan").build(), null, false);
        } catch (Exception ex) {
            assertEquals(ex.getClass(), InvalidRequestException.class);
        }
    }

    @Test
    void testInvalidChannelTtlMax() {
        try {
            validator.validate(getBuilder()
                    .name("mychan")
                    .ttlDays(10)
                    .maxItems(10)
                    .build(), null, false);
        } catch (Exception ex) {
            assertEquals(ex.getClass(), InvalidRequestException.class);
        }
    }

//    @Test(expected = InvalidRequestException.class)
    @Test
    void testInvalidChannelTtlMutable() {
        try {
            validator.validate(getBuilder()
                    .name("mychan")
                    .ttlDays(10)
                    .mutableTime(new DateTime())
                    .build(), null, false);;
        } catch (Exception ex) {
            assertEquals(ex.getClass(), InvalidRequestException.class);
        }
    }

    @Test
    void testInvalidChannelMaxMutable() {
        try {
            validator.validate(getBuilder()
                    .name("mychan")
                    .mutableTime(new DateTime())
                    .maxItems(10)
                    .build(), null, false);
        } catch (Exception ex) {
            assertEquals(ex.getClass(), InvalidRequestException.class);
        }
    }

    @Test
    void testInvalidChannelMutableTime() {
        try {
            validator.validate(getBuilder()
                    .name("mychan")
                    .mutableTime(new DateTime().plusMinutes(1))
                    .build(), null, false);
        } catch (Exception ex) {
            assertEquals(ex.getClass(), InvalidRequestException.class);
        }
    }

    @Test
    void testMutableTime() {
        validator.validate(getBuilder()
                .name("mychan")
                .mutableTime(new DateTime())
                .build(), null, false);
    }

    @Test
    void testKeepForever() {
        validator.validate(getBuilder()
                .name("mychan")
                .keepForever(true)
                .build(), null, false);
    }

    @Test
    void testMutableTimeForward() {
        ChannelConfig first = getBuilder().name("mychan").mutableTime(new DateTime().minusDays(2)).build();
        ChannelConfig second = first.toBuilder().mutableTime(new DateTime().minusDays(1)).build();
        validator.validate(first, second, false);
        validator.validate(first, first, false);
        validateError(second, first);
    }

//    @Test(expected = InvalidRequestException.class)
    @Test
    void testMutableTimeFuture() {
        try {
            validator.validate(getBuilder()
                    .name("testMutableTimeFuture")
                    .mutableTime(new DateTime().plusMinutes(1))
                    .build(), null, false);
        } catch (Exception ex) {
            assertEquals(ex.getClass(), InvalidRequestException.class);
        }

    }

//    @Test(expected = InvalidRequestException.class)
    @Test
    void testInvalidBatchMutable() {

        try {
            validator.validate(getBuilder()
                    .name("mychan")
                    .storage(ChannelConfig.BATCH)
                    .mutableTime(new DateTime())
                    .build(), null, false);
        } catch (Exception ex) {
            assertEquals(ex.getClass(), InvalidRequestException.class);
        }
    }

//    @Test(expected = InvalidRequestException.class)
    @Test
    void testInvalidBothMutable() {
        try {
            validator.validate(getBuilder()
                    .name("mychan")
                    .storage(ChannelConfig.BOTH)
                    .mutableTime(new DateTime())
                    .build(), null, false);
        } catch (Exception ex) {
            assertEquals(ex.getClass(), InvalidRequestException.class);
        }

    }

    @Test
    void testDescription1024() {
        validator.validate(getBuilder().name("desc").description(Strings.repeat("A", 1024)).build(), null, false);
    }

    @Test
    void testDescriptionTooBig() {
        try {
            validator.validate(getBuilder().name("toobig").description(Strings.repeat("A", 1025)).build(), null, false);
        } catch (Exception ex) {
            assertEquals(ex.getClass(), InvalidRequestException.class);
        }
    }

    @Test
    void testTagSpace() {
        try {
            validator.validate(getBuilder().name("space").tags(Arrays.asList("s p a c e")).build(), null, false);
        } catch (Exception ex) {
            assertEquals(ex.getClass(), InvalidRequestException.class);
        }
    }

    @Test
    void testTagUnderscore() {
        try {
            validator.validate(getBuilder().name("underscore").tags(Arrays.asList("under_score")).build(), null, false);
        } catch (Exception ex) {
            assertEquals(ex.getClass(), InvalidRequestException.class);
        }
    }

    @Test
    void testTagValid() {
        validator.validate(getBuilder().name("valid1").tags(Arrays.asList("abcdefghijklmnopqrstuvwxyz")).build(), null, false);
        validator.validate(getBuilder().name("valid2").tags(Arrays.asList("ABCDEFGHIJKLMNOPQRSTUVWXYZ123456789")).build(), null, false);
    }

    @Test
    void testTagTooLong() {
        try {
            validator.validate(getBuilder().name("tooLongTag")
                    .tags(Collections.singletonList(Strings.repeat("A", 49))).build(), null, false);
        } catch (Exception ex) {
            assertEquals(ex.getClass(), InvalidRequestException.class);
        }
    }

    @Test
    void testTooManyTags() {
        try {
            List<String> tags = new ArrayList<>();
            for (int i = 0; i < 21; i++) {
                tags.add("" + i);
            }
            validator.validate(getBuilder().name("tooManyTags")
                    .tags(tags).build(), null, false);
        } catch (Exception ex) {
            assertEquals(ex.getClass(), InvalidRequestException.class);
        }
    }

    @Test
    void testAllowColonAndDash() {
        validator.validate(getBuilder().name("colon").tags(Arrays.asList("a:b")).build(), null, false);
        validator.validate(getBuilder().name("dash").tags(Arrays.asList("a-b")).build(), null, false);
        validator.validate(getBuilder().name("colondash").tags(Arrays.asList("a-b:c")).build(), null, false);
    }

    @Test
    void testReplicatedTag() {
        ChannelConfig config = getBuilder().replicationSource("http://nowhere").build();
        assertTrue(config.getTags().contains("replicated"));
    }

    @Test
    void testOwner() {
        validator.validate(getBuilder().name("A").owner(Strings.repeat("A", 48)).build(), null, false);
    }

//    @Test(expected = InvalidRequestException.class)
    @Test
    void testTooLongOwner() {
        try {
            validator.validate(getBuilder().name("A").owner(Strings.repeat("A", 49)).build(), null, false);
        } catch (Exception ex) {
            assertEquals(ex.getClass(), InvalidRequestException.class);
        }
    }

    @Test
    void testValidStorage() {
        validator.validate(getBuilder().name("storage").storage(ChannelConfig.SINGLE).build(), null, false);
        validator.validate(getBuilder().name("storage").storage("batch").build(), null, false);
        validator.validate(getBuilder().name("storage").storage("BoTh").build(), null, false);
    }

//    @Test(expected = InvalidRequestException.class)
    @Test
    void testInvalidStorage() {
        try {
            validator.validate(getBuilder().name("storage").storage("stuff").build(), null, false);
        } catch (Exception ex) {
            assertEquals(ex.getClass(), InvalidRequestException.class);
        }
    }

    @Test
    void testChangeStorageLoss() {
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
    void testRemoveTagsLoss() {
        HubProperties.setProperty("hub.protect.channels", "true");
        ChannelConfig oneTwo = getBuilder().name("testRemoveTags").tags(Arrays.asList("one", "two")).build();
        ChannelConfig oneThree = getBuilder().name("testRemoveTags").tags(Arrays.asList("one", "three")).build();
        ChannelConfig oneTwoThree = getBuilder().name("testRemoveTags").tags(Arrays.asList("one", "two", "three")).build();
        validator.validate(oneTwo, oneTwo, false);
        validator.validate(oneTwoThree, oneTwo, false);
        validateError(oneTwo, oneThree);
    }

    @Test
    void testTtlDaysLoss() {
        HubProperties.setProperty("hub.protect.channels", "true");
        ChannelConfig ten = getBuilder().name("testTtlDays").ttlDays(10).build();
        ChannelConfig eleven = getBuilder().name("testTtlDays").ttlDays(11).build();
        validator.validate(ten, ten, false);
        validateError(ten, eleven);
    }

    @Test
    void testMaxItemsLoss() {
        HubProperties.setProperty("hub.protect.channels", "true");
        ChannelConfig ten = getBuilder().name("testMaxItems").maxItems(10).build();
        ChannelConfig eleven = getBuilder().name("testMaxItems").maxItems(11).build();
        validator.validate(ten, ten, false);
        validateError(ten, eleven);
    }

    @Test
    void testReplicationLoss() {
        HubProperties.setProperty("hub.protect.channels", "true");
        ChannelConfig changed = getBuilder().name("testReplication").replicationSource("http://hub/channel/name1").build();
        ChannelConfig replication = getBuilder().name("testReplication").replicationSource("http://hub/channel/name").build();
        validator.validate(changed, changed, false);
        validator.validate(replication, replication, false);
        validateError(changed, replication);
    }

    @Test
    void testDataLossChange() {
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
