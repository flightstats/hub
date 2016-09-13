package com.flightstats.hub.channel;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.exception.ConflictException;
import com.flightstats.hub.exception.InvalidRequestException;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.GlobalConfig;
import com.google.common.base.Strings;
import org.junit.Before;
import org.junit.Test;

import java.net.MalformedURLException;
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
        HubProperties.setProperty("hub.allow.channel.loss", "false");
    }

    @Test
    public void testAllGood() throws Exception {
        validator.validate(ChannelConfig.builder().withName(Strings.repeat("A", 48)).build(), null);
    }

    @Test(expected = InvalidRequestException.class)
    public void testTooLong() throws Exception {
        validator.validate(ChannelConfig.builder().withName(Strings.repeat("A", 49)).build(), null);
    }

    @Test(expected = InvalidRequestException.class)
    public void testChannelNameNull() throws Exception {
        validator.validate(ChannelConfig.builder().withName(null).build(), null);
    }

    @Test(expected = InvalidRequestException.class)
    public void testChannelNameEmpty() throws Exception {
        validator.validate(ChannelConfig.builder().withName("").build(), null);
    }

    @Test(expected = InvalidRequestException.class)
    public void testChannelNameBlank() throws Exception {
        validator.validate(ChannelConfig.builder().withName("  ").build(), null);
    }

    @Test(expected = ConflictException.class)
    public void testChannelExists() throws Exception {
        String channelName = "achannel";
        when(channelService.channelExists(channelName)).thenReturn(true);
        ChannelConfig channelConfig = ChannelConfig.builder().withName(channelName).build();
        validator.validate(channelConfig, channelConfig);
    }

    @Test(expected = InvalidRequestException.class)
    public void testInvalidSpaceCharacter() throws Exception {
        validator.validate(ChannelConfig.builder().withName("my chan").build(), null);
    }

    @Test
    public void testValidUnderscore() throws Exception {
        validator.validate(ChannelConfig.builder().withName("my_chan").build(), null);
    }

    @Test
    public void testValidHyphen() throws Exception {
        validator.validate(ChannelConfig.builder().withName("my-chan").build(), null);
    }

    @Test(expected = InvalidRequestException.class)
    public void testInvalidCharacter() throws Exception {
        validator.validate(ChannelConfig.builder().withName("my#chan").build(), null);
    }

    @Test(expected = InvalidRequestException.class)
    public void testInvalidChannelTtl() throws Exception {
        validator.validate(ChannelConfig.builder().withName("mychan")
                .withTtlDays(10)
                .withMaxItems(10)
                .build(), null);
    }

    @Test
    public void testDescription1024() throws Exception {
        validator.validate(ChannelConfig.builder().withName("desc").withDescription(Strings.repeat("A", 1024)).build(), null);
    }

    @Test(expected = InvalidRequestException.class)
    public void testDescriptionTooBig() throws Exception {
        validator.validate(ChannelConfig.builder().withName("toobig").withDescription(Strings.repeat("A", 1025)).build(), null);
    }

    @Test(expected = InvalidRequestException.class)
    public void testTagSpace() throws Exception {
        validator.validate(ChannelConfig.builder().withName("space").withTags(Arrays.asList("s p a c e")).build(), null);
    }

    @Test(expected = InvalidRequestException.class)
    public void testTagUnderscore() throws Exception {
        validator.validate(ChannelConfig.builder().withName("underscore").withTags(Arrays.asList("under_score")).build(), null);
    }

    @Test
    public void testTagValid() throws Exception {
        validator.validate(ChannelConfig.builder().withName("valid1").withTags(Arrays.asList("abcdefghijklmnopqrstuvwxyz")).build(), null);
        validator.validate(ChannelConfig.builder().withName("valid2").withTags(Arrays.asList("ABCDEFGHIJKLMNOPQRSTUVWXYZ123456789")).build(), null);
    }

    @Test(expected = InvalidRequestException.class)
    public void testTagTooLong() throws Exception {
        validator.validate(ChannelConfig.builder().withName("tooLongTag")
                .withTags(Arrays.asList(Strings.repeat("A", 49))).build(), null);
    }

    @Test(expected = InvalidRequestException.class)
    public void testTooManyTags() throws Exception {
        List<String> tags = new ArrayList<>();
        for (int i = 0; i < 21; i++) {
            tags.add("" + i);
        }
        validator.validate(ChannelConfig.builder().withName("tooManyTags")
                .withTags(tags).build(), null);
    }

    @Test
    public void testAllowColonAndDash() throws Exception {
        validator.validate(ChannelConfig.builder().withName("colon").withTags(Arrays.asList("a:b")).build(), null);
        validator.validate(ChannelConfig.builder().withName("dash").withTags(Arrays.asList("a-b")).build(), null);
        validator.validate(ChannelConfig.builder().withName("colondash").withTags(Arrays.asList("a-b:c")).build(), null);
    }

    @Test
    public void testReplicatedTag() {
        ChannelConfig config = ChannelConfig.builder().withReplicationSource("http://nowhere").build();
        assertTrue(config.getTags().contains("replicated"));
    }

    @Test
    public void testGlobalTag() {
        ChannelConfig config = ChannelConfig.builder().withGlobal(new GlobalConfig()).build();
        assertTrue(config.getTags().contains("global"));
    }

    @Test
    public void testHistoricalTag() {
        ChannelConfig config = ChannelConfig.builder().withHistorical(true).build();
        assertTrue(config.getTags().contains("historical"));
    }

    public void testOwner() throws Exception {
        validator.validate(ChannelConfig.builder().withName("A").withOwner(Strings.repeat("A", 48)).build(), null);
    }

    @Test(expected = InvalidRequestException.class)
    public void testTooLongOwner() throws Exception {
        validator.validate(ChannelConfig.builder().withName("A").withOwner(Strings.repeat("A", 49)).build(), null);
    }

    @Test
    public void testValidStorage() {
        validator.validate(ChannelConfig.builder().withName("storage").withStorage(ChannelConfig.SINGLE).build(), null);
        validator.validate(ChannelConfig.builder().withName("storage").withStorage("batch").build(), null);
        validator.validate(ChannelConfig.builder().withName("storage").withStorage("BoTh").build(), null);
    }

    @Test(expected = InvalidRequestException.class)
    public void testInvalidStorage() {
        validator.validate(ChannelConfig.builder().withName("storage").withStorage("stuff").build(), null);
    }

    @Test
    public void testGlobal() {
        GlobalConfig globalConfig = getGlobalConfig();
        validator.validate(ChannelConfig.builder()
                .withName("global")
                .withGlobal(globalConfig).build(), null);
    }

    @Test(expected = InvalidRequestException.class)
    public void testNoSatellite() {
        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.setMaster("http://master");
        validator.validate(ChannelConfig.builder()
                .withName("global")
                .withGlobal(globalConfig).build(), null);
    }

    @Test(expected = InvalidRequestException.class)
    public void testNoMaster() {
        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.addSatellite("http://master");
        validator.validate(ChannelConfig.builder()
                .withName("global")
                .withGlobal(globalConfig).build(), null);
    }

    @Test(expected = InvalidRequestException.class)
    public void testSatelliteSame() {
        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.setMaster("http://master");
        globalConfig.addSatellite("http://master/");
        validator.validate(ChannelConfig.builder()
                .withName("global")
                .withGlobal(globalConfig).build(), null);
    }

    @Test(expected = InvalidRequestException.class)
    public void testMasterUrl() throws MalformedURLException {
        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.setMaster("http:/master");
        globalConfig.addSatellite("http://satellite");
        validator.validate(ChannelConfig.builder()
                .withName("global")
                .withGlobal(globalConfig).build(), null);
    }

    @Test(expected = InvalidRequestException.class)
    public void testSatelliteUrl() throws MalformedURLException {
        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.setMaster("http:/master");
        globalConfig.addSatellite("http://satellite");
        globalConfig.addSatellite("ftp://satellite2");
        validator.validate(ChannelConfig.builder()
                .withName("global")
                .withGlobal(globalConfig).build(), null);
    }

    @Test(expected = InvalidRequestException.class)
    public void testHistoricalSwitch() throws Exception {
        ChannelConfig configA = ChannelConfig.builder().withName("A").withHistorical(false).build();
        ChannelConfig configB = ChannelConfig.builder().withName("B").withHistorical(true).build();
        validator.validate(configA, configB);
    }

    @Test(expected = InvalidRequestException.class)
    public void testHistoricalNotMax() throws Exception {
        ChannelConfig configA = ChannelConfig.builder().withName("A").withHistorical(true).withMaxItems(10).build();
        validator.validate(configA, null);
    }

    @Test
    public void testChangeStorageLoss() throws Exception {
        HubProperties.setProperty("hub.allow.channel.loss", "true");
        ChannelConfig single = ChannelConfig.builder().withName("storage").withStorage(ChannelConfig.SINGLE).build();
        ChannelConfig batch = ChannelConfig.builder().withName("storage").withStorage(ChannelConfig.BATCH).build();
        ChannelConfig both = ChannelConfig.builder().withName("storage").withStorage(ChannelConfig.BOTH).build();
        validator.validate(both, single);
        validator.validate(both, batch);
        validateError(single, both);
        validateError(batch, both);
        validateError(single, batch);
        validateError(batch, single);
    }

    @Test
    public void testRemoveTagsLoss() throws Exception {
        HubProperties.setProperty("hub.allow.channel.loss", "true");
        ChannelConfig oneTwo = ChannelConfig.builder().withName("testRemoveTags").withTags(Arrays.asList("one", "two")).build();
        ChannelConfig oneThree = ChannelConfig.builder().withName("testRemoveTags").withTags(Arrays.asList("one", "three")).build();
        ChannelConfig oneTwoThree = ChannelConfig.builder().withName("testRemoveTags").withTags(Arrays.asList("one", "two", "three")).build();
        validator.validate(oneTwo, oneTwo);
        validator.validate(oneTwoThree, oneTwo);
        validateError(oneTwo, oneThree);
    }

    @Test
    public void testTtlDaysLoss() throws Exception {
        HubProperties.setProperty("hub.allow.channel.loss", "true");
        ChannelConfig ten = ChannelConfig.builder().withName("testTtlDays").withTtlDays(10).build();
        ChannelConfig eleven = ChannelConfig.builder().withName("testTtlDays").withTtlDays(11).build();
        validator.validate(ten, ten);
        validateError(ten, eleven);
    }

    @Test
    public void testMaxItemsLoss() throws Exception {
        HubProperties.setProperty("hub.allow.channel.loss", "true");
        ChannelConfig ten = ChannelConfig.builder().withName("testMaxItems").withMaxItems(10).build();
        ChannelConfig eleven = ChannelConfig.builder().withName("testMaxItems").withMaxItems(11).build();
        validator.validate(ten, ten);
        validateError(ten, eleven);
    }

    @Test
    public void testReplicationLoss() throws Exception {
        HubProperties.setProperty("hub.allow.channel.loss", "true");
        ChannelConfig changed = ChannelConfig.builder().withName("testReplication").withReplicationSource("http://hub/channel/name1").build();
        ChannelConfig replication = ChannelConfig.builder().withName("testReplication").withReplicationSource("http://hub/channel/name").build();
        validator.validate(changed, changed);
        validator.validate(replication, replication);
        validateError(changed, replication);
    }

    @Test
    public void testGlobalLossSatellite() throws Exception {
        HubProperties.setProperty("hub.allow.channel.loss", "true");
        GlobalConfig twoSatellites = getGlobalConfig();
        twoSatellites.addSatellite("http://satellite2");
        ChannelConfig two = ChannelConfig.builder().withName("testGlobalLoss").withGlobal(twoSatellites).build();
        GlobalConfig globalConfigOld = getGlobalConfig();
        ChannelConfig one = ChannelConfig.builder().withName("testGlobalLoss").withGlobal(globalConfigOld).build();
        ChannelConfig none = ChannelConfig.builder().withName("testGlobalLoss").withGlobal(null).build();
        validator.validate(two, one);
        validateError(one, two);
        validateError(none, one);
    }

    @Test
    public void testGlobalLossMaster() throws Exception {
        HubProperties.setProperty("hub.allow.channel.loss", "true");
        GlobalConfig globalConfigOld = getGlobalConfig();
        ChannelConfig master = ChannelConfig.builder().withName("testGlobalLoss").withGlobal(globalConfigOld).build();
        ChannelConfig none = ChannelConfig.builder().withName("testGlobalLoss").withGlobal(null).build();
        GlobalConfig globalConfig = getGlobalConfig();
        globalConfig.setMaster("http://master2");
        ChannelConfig other = ChannelConfig.builder().withName("testGlobalLoss").withGlobal(globalConfig).build();
        validator.validate(master, none);
        validateError(none, master);
        validateError(other, master);
    }

    private GlobalConfig getGlobalConfig() {
        GlobalConfig twoSatellites = new GlobalConfig();
        twoSatellites.setMaster("http://master");
        twoSatellites.addSatellite("http://satellite");
        return twoSatellites;
    }


    private void validateError(ChannelConfig config, ChannelConfig oldConfig) {
        try {
            validator.validate(config, oldConfig);
            fail("expected exception");
        } catch (InvalidRequestException e) {
            //this is expected
        }
    }


}
