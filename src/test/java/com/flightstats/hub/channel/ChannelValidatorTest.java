package com.flightstats.hub.channel;

import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.exception.ConflictException;
import com.flightstats.hub.exception.ForbiddenRequestException;
import com.flightstats.hub.exception.InvalidRequestException;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.flightstats.hub.model.ChannelType.BATCH;
import static com.flightstats.hub.model.ChannelType.BOTH;
import static com.flightstats.hub.model.ChannelType.SINGLE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Slf4j
@Execution(ExecutionMode.SAME_THREAD)
class ChannelValidatorTest {

    @Mock
    private Dao<ChannelConfig> channelConfigDao;
    private ChannelValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ChannelValidator(channelConfigDao);
    }

    @AfterEach
    void teardown() {
        log.info("test finished");
    }

    @Test
    void testAllGood() {
        ChannelConfig channelConfig = getBuilder().name(Strings.repeat("A", 48)).build();
        validator.validate(channelConfig, null, false);
    }

    @Test
    void testTooLong() {
        ChannelConfig channelConfig = getBuilder().name(Strings.repeat("A", 49)).build();
        assertThrows(InvalidRequestException.class, () ->
                validator.validate(channelConfig, null, false));
    }

    @Test
    void testChannelNameNull() {
        assertThrows(InvalidRequestException.class, () ->
                validator.validate(getBuilder().name(null).build(), null, false));
    }

    @Test
    void validatorShouldThrowForBiddenExceptionWhenMaxItemsChange() {
        ChannelConfig channelConfig = getBuilder()
                .name("forever")
                .keepForever(true)
                .protect(true)
                .build();

        log.info("In the test");
        log.info("channel value" + channelConfig.getKeepForever());
        assertThrows(ForbiddenRequestException.class,
                () -> validator.validate(channelConfig.toBuilder().keepForever(false).maxItems(300).build(), channelConfig, false));

    }

    @Test
    void validatorShouldThrowForBiddenExceptionWhenTtlDaysChange() {
        ChannelConfig channelConfig = getBuilder()
                .name("forever")
                .keepForever(true)
                .protect(true)
                .build();

        assertThrows(ForbiddenRequestException.class,
                () -> validator.validate(channelConfig.toBuilder().keepForever(false).ttlDays(300).build(), channelConfig, false));
    }

    @Test
    void validatorShouldNotThrowAnyExceptionToUpdateTtlDaysWhenKeepForeverSetAsTrue() {
        ChannelConfig single = ChannelConfig.builder()
                .owner("test Owner")
                .ttlDays(30)
                .keepForever(false)
                .name("defaults").build();
        assertDoesNotThrow(() -> validator.validate(single.toBuilder().keepForever(true).ttlDays(0).build(), single, true));
    }

    @Test
    void validatorShouldNotThrowAnyExceptionToUpdateMaxItemsWhenKeepForeverSetAsTrue() {
        ChannelConfig single = ChannelConfig.builder()
                .owner("test Owner")
                .maxItems(30)
                .keepForever(false)
                .name("defaults").build();
        assertDoesNotThrow(() -> validator.validate(single.toBuilder().keepForever(true).maxItems(0).build(), single, false));
    }

    @Test
    void testChannelNameEmpty() {
        ChannelConfig channelConfig = getBuilder().name("").build();
        assertThrows(InvalidRequestException.class,
                () -> validator.validate(channelConfig, null, false));
    }

    @Test
    void testChannelNameBlank() {
        ChannelConfig channelConfig = getBuilder().name("  ").build();
        assertThrows(InvalidRequestException.class, () ->
                validator.validate(channelConfig, null, false));
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
        ChannelConfig channelConfig = getBuilder().name("my chan").build();
        assertThrows(InvalidRequestException.class, () ->
                validator.validate(channelConfig, null, false));
    }

    @Test
    void testValidUnderscore() {
        ChannelConfig channelConfig = getBuilder().name("my_chan").build();
        validator.validate(channelConfig, null, false);
    }

    @Test
    void testValidHyphen() {
        ChannelConfig channelConfig = getBuilder().name("my-chan").build();
        validator.validate(channelConfig, null, false);
    }

    @Test
    void testInvalidCharacter() {
        ChannelConfig channelConfig = getBuilder().name("my#chan").build();
        assertThrows(InvalidRequestException.class, () ->
                validator.validate(channelConfig, null, false));
    }

    @Test
    void testInvalidChannelTtlMax() {
        ChannelConfig channelConfig = getBuilder()
                .name("mychan")
                .ttlDays(10)
                .maxItems(10)
                .build();
        assertThrows(InvalidRequestException.class,
                () -> validator.validate(channelConfig, null, false));
    }

    @Test
    void testInvalidChannelTtlMutable() {
        ChannelConfig channelConfig = getBuilder()
                .name("mychan")
                .ttlDays(10)
                .mutableTime(new DateTime())
                .build();
        assertThrows(InvalidRequestException.class,
                () -> validator.validate(channelConfig, null, false));
    }

    @Test
    void testInvalidChannelMaxMutable() {
        ChannelConfig channelConfig = getBuilder()
                .name("mychan")
                .mutableTime(new DateTime())
                .maxItems(10)
                .build();
        assertThrows(InvalidRequestException.class,
                () -> validator.validate(channelConfig, null, false));
    }

    @Test
    void testInvalidChannelMutableTime() {
        ChannelConfig channelConfig = getBuilder()
                .name("mychan")
                .mutableTime(new DateTime().plusMinutes(1))
                .build();
        assertThrows(InvalidRequestException.class,
                () -> validator.validate(channelConfig, null, false));
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
        ChannelConfig single = getBuilder().name("storage").storage(SINGLE.name()).protect(true).build();
        ChannelConfig batch = getBuilder().name("storage").storage(BATCH.name()).protect(true).build();
        ChannelConfig both = getBuilder().name("storage").storage(BOTH.name()).protect(true).build();
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
        ChannelConfig oneTwo = getBuilder().name("testRemoveTags").tags(Arrays.asList("one", "two")).protect(true).build();
        ChannelConfig oneThree = getBuilder().name("testRemoveTags").tags(Arrays.asList("one", "three")).protect(true).build();
        ChannelConfig oneTwoThree = getBuilder().name("testRemoveTags").tags(Arrays.asList("one", "two", "three")).protect(true).build();
        validator.validate(oneTwo, oneTwo, false);
        validator.validate(oneTwoThree, oneTwo, false);
        validateError(oneTwo, oneThree);
    }

    @Test
    void testTtlDaysLoss() {
        ChannelConfig ten = getBuilder().name("testTtlDays").ttlDays(10).protect(true).build();
        ChannelConfig eleven = getBuilder().name("testTtlDays").ttlDays(11).protect(true).build();
        validator.validate(ten, ten, false);
        validateError(ten, eleven);
    }

    @Test
    void testMaxItemsLoss() {
        ChannelConfig ten = getBuilder().name("testMaxItems").maxItems(10).protect(true).build();
        ChannelConfig eleven = getBuilder().name("testMaxItems").maxItems(11).protect(true).build();
        validator.validate(ten, ten, false);
        validateError(ten, eleven);
    }

    @Test
    void testReplicationLoss() {
        ChannelConfig changed = getBuilder().name("testReplication").protect(true).replicationSource("http://hub/channel/name1").build();
        ChannelConfig replication = getBuilder().name("testReplication").protect(true).replicationSource("http://hub/channel/name").build();
        validator.validate(changed, changed, false);
        validator.validate(replication, replication, false);
        validateError(changed, replication);
    }

    @Test
    void testDataLossChange() {
        ChannelConfig dataLoss = getBuilder().protect(false).name("testDataLossChange").build();
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

    @Test
    void mutableTimeFailOnUpdate_noUpdateDefaultStorage_throws() {
        ChannelConfig single = ChannelConfig.builder()
                .name("defaults").build();
        DateTime mutableTime = TimeUtil.now();
        assertThrows(InvalidRequestException.class, () -> validator.validate(single.toBuilder().mutableTime(mutableTime).build(), single, false));
    }

    @Test
    void mutableTimeFailOnUpdate_updateMutableChannelToBatch_throws() {
        DateTime mutableTime = TimeUtil.now();
        ChannelConfig historical = ChannelConfig.builder()
                .name("mutableTime")
                .mutableTime(mutableTime)
                .storage(SINGLE.name())
                .build();
        assertThrows(InvalidRequestException.class, () -> validator.validate(historical.toBuilder().storage(BATCH.name()).build(), historical, false));
    }

}
