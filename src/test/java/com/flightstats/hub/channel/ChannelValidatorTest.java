package com.flightstats.hub.channel;

import com.flightstats.hub.config.PropertiesLoader;
import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.exception.ConflictException;
import com.flightstats.hub.exception.ForbiddenRequestException;
import com.flightstats.hub.exception.InvalidRequestException;
import com.flightstats.hub.model.ChannelConfig;
import com.google.common.base.Strings;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.flightstats.hub.model.ChannelType.BATCH;
import static com.flightstats.hub.model.ChannelType.BOTH;
import static com.flightstats.hub.model.ChannelType.SINGLE;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

class ChannelValidatorTest {

    @Mock
    private Dao<ChannelConfig> channelConfigDao;
    private ChannelValidator validator;

    @BeforeEach
    void setUp() {
        initMocks(this);
        validator = new ChannelValidator(channelConfigDao);
        when(channelConfigDao.exists(any(String.class))).thenReturn(false);
        PropertiesLoader.getInstance().setProperty("hub.protect.channels", "false");
    }

    @Test
    void testAllGood() {
        validator.validate(getBuilder().name(Strings.repeat("A", 48)).build(), null, false);
    }

    @Test
    void testTooLong() {
        assertThrows(InvalidRequestException.class, () ->
                validator.validate(getBuilder().name(Strings.repeat("A", 49)).build(), null, false));
    }

    @Test
    void testChannelNameNull() {
        assertThrows(InvalidRequestException.class, () ->
                validator.validate(getBuilder().name(null).build(), null, false));
    }

    @Test
    void testChannelNameEmpty() {
        assertThrows(InvalidRequestException.class,
                () -> validator.validate(getBuilder().name("").build(), null, false));
    }

    @Test
    void testChannelNameBlank() {
        assertThrows(InvalidRequestException.class, () ->
                validator.validate(getBuilder().name("  ").build(), null, false));
    }

    @Test
    void testChannelExists() {
        String channelName = "achannel";
        when(channelConfigDao.exists(channelName)).thenReturn(true);
        ChannelConfig channelConfig = getBuilder().name(channelName).build();
        assertThrows(ConflictException.class, () -> validator.validate(channelConfig, null, false));
    }

    @Test
    void testInvalidSpaceCharacter() {
        assertThrows(InvalidRequestException.class, () ->
                validator.validate(getBuilder().name("my chan").build(), null, false));
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
        assertThrows(InvalidRequestException.class, () ->
                validator.validate(getBuilder().name("my#chan").build(), null, false));
    }

    @Test
    void testInvalidChannelTtlMax() {
        assertThrows(InvalidRequestException.class, () -> validator.validate(getBuilder()
                .name("mychan")
                .ttlDays(10)
                .maxItems(10)
                .build(), null, false));
    }

    @Test
    void testInvalidChannelTtlMutable() {
        assertThrows(InvalidRequestException.class, () -> validator.validate(getBuilder()
                .name("mychan")
                .ttlDays(10)
                .mutableTime(new DateTime())
                .build(), null, false));
    }

    @Test
    void testInvalidChannelMaxMutable() {
        assertThrows(InvalidRequestException.class, () -> validator.validate(getBuilder()
                .name("mychan")
                .mutableTime(new DateTime())
                .maxItems(10)
                .build(), null, false));
    }

    @Test
    void testInvalidChannelMutableTime() {
        assertThrows(InvalidRequestException.class, () -> validator.validate(getBuilder()
                .name("mychan")
                .mutableTime(new DateTime().plusMinutes(1))
                .build(), null, false));
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

    @Test
    void testMutableTimeFuture() {
        assertThrows(InvalidRequestException.class, () -> validator.validate(getBuilder()
                .name("testMutableTimeFuture")
                .mutableTime(new DateTime().plusMinutes(1))
                .build(), null, false));

    }

    @Test
    void testInvalidBatchMutable() {
        assertThrows(InvalidRequestException.class, () -> validator.validate(getBuilder()
                .name("mychan")
                .storage(BATCH.name())
                .mutableTime(new DateTime())
                .build(), null, false));
    }

    @Test
    void testInvalidBothMutable() {
        assertThrows(InvalidRequestException.class, () -> validator.validate(getBuilder()
                .name("mychan")
                .storage(BOTH.name())
                .mutableTime(new DateTime())
                .build(), null, false));
    }

    @Test
    void testDescription1024() {
        validator.validate(getBuilder().name("desc").description(Strings.repeat("A", 1024)).build(), null, false);
    }

    @Test
    void testDescriptionTooBig() {
        assertThrows(InvalidRequestException.class, () ->
                validator.validate(getBuilder().name("toobig").description(Strings.repeat("A", 1025)).build(), null, false));
    }

    @Test
    void testTagSpace() {
        assertThrows(InvalidRequestException.class, () ->
                validator.validate(getBuilder().name("space").tags(Collections.singletonList("s p a c e")).build(), null, false));
    }

    @Test
    void testTagUnderscore() {
        assertThrows(InvalidRequestException.class, () ->
                validator.validate(getBuilder().name("underscore").tags(Collections.singletonList("under_score")).build(), null, false));
    }

    @Test
    void testTagValid() {
        validator.validate(getBuilder().name("valid1").tags(Collections.singletonList("abcdefghijklmnopqrstuvwxyz")).build(), null, false);
        validator.validate(getBuilder().name("valid2").tags(Collections.singletonList("ABCDEFGHIJKLMNOPQRSTUVWXYZ123456789")).build(), null, false);
    }

    @Test
    void testTagTooLong() {
        assertThrows(InvalidRequestException.class, () -> validator.validate(getBuilder().name("tooLongTag")
                .tags(Collections.singletonList(Strings.repeat("A", 49))).build(), null, false));
    }

    @Test
    void testTooManyTags() {
        List<String> tags = new ArrayList<>();
        for (int i = 0; i < 21; i++) {
            tags.add("" + i);
        }
        assertThrows(InvalidRequestException.class, () ->
                validator.validate(getBuilder().name("tooManyTags").tags(tags).build(), null, false));
    }

    @Test
    void testAllowColonAndDash() {
        validator.validate(getBuilder().name("colon").tags(Collections.singletonList("a:b")).build(), null, false);
        validator.validate(getBuilder().name("dash").tags(Collections.singletonList("a-b")).build(), null, false);
        validator.validate(getBuilder().name("colondash").tags(Collections.singletonList("a-b:c")).build(), null, false);
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

    @Test
    void testTooLongOwner() {
        assertThrows(InvalidRequestException.class, () ->
                validator.validate(getBuilder().name("A").owner(Strings.repeat("A", 49)).build(), null, false));
    }

    @Test
    void testValidStorage() {
        validator.validate(getBuilder().name("storage").storage(SINGLE.name()).build(), null, false);
        validator.validate(getBuilder().name("storage").storage("batch").build(), null, false);
        validator.validate(getBuilder().name("storage").storage("BoTh").build(), null, false);
    }

    @Test
    void testInvalidStorage() {
        assertThrows(InvalidRequestException.class, () ->
                validator.validate(getBuilder().name("storage").storage("stuff").build(), null, false));
    }

    @Test
    void testChangeStorageLoss() {
        PropertiesLoader.getInstance().setProperty("hub.protect.channels", "true");
        ChannelConfig single = getBuilder().name("storage").storage(SINGLE.name()).build();
        ChannelConfig batch = getBuilder().name("storage").storage(BATCH.name()).build();
        ChannelConfig both = getBuilder().name("storage").storage(BOTH.name()).build();
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
        PropertiesLoader.getInstance().setProperty("hub.protect.channels", "true");
        ChannelConfig oneTwo = getBuilder().name("testRemoveTags").tags(Arrays.asList("one", "two")).build();
        ChannelConfig oneThree = getBuilder().name("testRemoveTags").tags(Arrays.asList("one", "three")).build();
        ChannelConfig oneTwoThree = getBuilder().name("testRemoveTags").tags(Arrays.asList("one", "two", "three")).build();
        validator.validate(oneTwo, oneTwo, false);
        validator.validate(oneTwoThree, oneTwo, false);
        validateError(oneTwo, oneThree);
    }

    @Test
    void testTtlDaysLoss() {
        PropertiesLoader.getInstance().setProperty("hub.protect.channels", "true");
        ChannelConfig ten = getBuilder().name("testTtlDays").ttlDays(10).build();
        ChannelConfig eleven = getBuilder().name("testTtlDays").ttlDays(11).build();
        validator.validate(ten, ten, false);
        validateError(ten, eleven);
    }

    @Test
    void testMaxItemsLoss() {
        PropertiesLoader.getInstance().setProperty("hub.protect.channels", "true");
        ChannelConfig ten = getBuilder().name("testMaxItems").maxItems(10).build();
        ChannelConfig eleven = getBuilder().name("testMaxItems").maxItems(11).build();
        validator.validate(ten, ten, false);
        validateError(ten, eleven);
    }

    @Test
    void testReplicationLoss() {
        PropertiesLoader.getInstance().setProperty("hub.protect.channels", "true");
        ChannelConfig changed = getBuilder().name("testReplication").replicationSource("http://hub/channel/name1").build();
        ChannelConfig replication = getBuilder().name("testReplication").replicationSource("http://hub/channel/name").build();
        validator.validate(changed, changed, false);
        validator.validate(replication, replication, false);
        validateError(changed, replication);
    }

    @Test
    void testDataLossChange() {
        PropertiesLoader.getInstance().setProperty("hub.protect.channels", "false");
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
