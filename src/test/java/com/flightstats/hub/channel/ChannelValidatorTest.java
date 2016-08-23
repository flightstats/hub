package com.flightstats.hub.channel;

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
    }

    @Test
    public void testAllGood() throws Exception {
        validator.validate(ChannelConfig.builder().withName(Strings.repeat("A", 48)).build(), true, null);
    }

    @Test(expected = InvalidRequestException.class)
    public void testTooLong() throws Exception {
        validator.validate(ChannelConfig.builder().withName(Strings.repeat("A", 49)).build(), true, null);
    }

    @Test(expected = InvalidRequestException.class)
    public void testChannelNameNull() throws Exception {
        validator.validate(ChannelConfig.builder().withName(null).build(), true, null);
    }

    @Test(expected = InvalidRequestException.class)
    public void testChannelNameEmpty() throws Exception {
        validator.validate(ChannelConfig.builder().withName("").build(), true, null);
    }

    @Test(expected = InvalidRequestException.class)
    public void testChannelNameBlank() throws Exception {
        validator.validate(ChannelConfig.builder().withName("  ").build(), true, null);
    }

    @Test(expected = ConflictException.class)
    public void testChannelExists() throws Exception {
        String channelName = "achannel";
        when(channelService.channelExists(channelName)).thenReturn(true);
        validator.validate(ChannelConfig.builder().withName(channelName).build(), true, null);
    }

    @Test(expected = InvalidRequestException.class)
    public void testInvalidSpaceCharacter() throws Exception {
        validator.validate(ChannelConfig.builder().withName("my chan").build(), true, null);
    }

    @Test
    public void testValidUnderscore() throws Exception {
        validator.validate(ChannelConfig.builder().withName("my_chan").build(), true, null);
    }

    @Test
    public void testValidHyphen() throws Exception {
        validator.validate(ChannelConfig.builder().withName("my-chan").build(), true, null);
    }

    @Test(expected = InvalidRequestException.class)
    public void testInvalidCharacter() throws Exception {
        validator.validate(ChannelConfig.builder().withName("my#chan").build(), true, null);
    }

    @Test(expected = InvalidRequestException.class)
    public void testInvalidChannelTtl() throws Exception {
        validator.validate(ChannelConfig.builder().withName("mychan")
                .withTtlDays(10)
                .withMaxItems(10)
                .build(), true, null);
    }

    @Test
    public void testDescription1024() throws Exception {
        validator.validate(ChannelConfig.builder().withName("desc").withDescription(Strings.repeat("A", 1024)).build(), true, null);
    }

    @Test(expected = InvalidRequestException.class)
    public void testDescriptionTooBig() throws Exception {
        validator.validate(ChannelConfig.builder().withName("toobig").withDescription(Strings.repeat("A", 1025)).build(), true, null);
    }

    @Test(expected = InvalidRequestException.class)
    public void testTagSpace() throws Exception {
        validator.validate(ChannelConfig.builder().withName("space").withTags(Arrays.asList("s p a c e")).build(), true, null);
    }

    @Test(expected = InvalidRequestException.class)
    public void testTagUnderscore() throws Exception {
        validator.validate(ChannelConfig.builder().withName("underscore").withTags(Arrays.asList("under_score")).build(), true, null);
    }

    @Test
    public void testTagValid() throws Exception {
        validator.validate(ChannelConfig.builder().withName("valid1").withTags(Arrays.asList("abcdefghijklmnopqrstuvwxyz")).build(), true, null);
        validator.validate(ChannelConfig.builder().withName("valid2").withTags(Arrays.asList("ABCDEFGHIJKLMNOPQRSTUVWXYZ123456789")).build(), true, null);
    }

    @Test(expected = InvalidRequestException.class)
    public void testTagTooLong() throws Exception {
        validator.validate(ChannelConfig.builder().withName("tooLongTag")
                .withTags(Arrays.asList(Strings.repeat("A", 49))).build(), true, null);
    }

    @Test(expected = InvalidRequestException.class)
    public void testTooManyTags() throws Exception {
        List<String> tags = new ArrayList<>();
        for (int i = 0; i < 21; i++) {
            tags.add("" + i);
        }
        validator.validate(ChannelConfig.builder().withName("tooManyTags")
                .withTags(tags).build(), true, null);
    }

    @Test
    public void testAllowColonAndDash() throws Exception {
        validator.validate(ChannelConfig.builder().withName("colon").withTags(Arrays.asList("a:b")).build(), true, null);
        validator.validate(ChannelConfig.builder().withName("dash").withTags(Arrays.asList("a-b")).build(), true, null);
        validator.validate(ChannelConfig.builder().withName("colondash").withTags(Arrays.asList("a-b:c")).build(), true, null);
    }

    public void testOwner() throws Exception {
        validator.validate(ChannelConfig.builder().withName("A").withOwner(Strings.repeat("A", 48)).build(), true, null);
    }

    @Test(expected = InvalidRequestException.class)
    public void testTooLongOwner() throws Exception {
        validator.validate(ChannelConfig.builder().withName("A").withOwner(Strings.repeat("A", 49)).build(), true, null);
    }

    @Test
    public void testValidStorage() {
        validator.validate(ChannelConfig.builder().withName("storage").withStorage(ChannelConfig.SINGLE).build(), true, null);
        validator.validate(ChannelConfig.builder().withName("storage").withStorage("batch").build(), true, null);
        validator.validate(ChannelConfig.builder().withName("storage").withStorage("BoTh").build(), true, null);
    }

    @Test(expected = InvalidRequestException.class)
    public void testInvalidStorage() {
        validator.validate(ChannelConfig.builder().withName("storage").withStorage("stuff").build(), true, null);
    }

    @Test
    public void testGlobal() {
        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.setMaster("http://master");
        globalConfig.addSatellite("http://satellite");
        validator.validate(ChannelConfig.builder()
                .withName("global")
                .withGlobal(globalConfig).build(), true, null);
    }

    @Test(expected = InvalidRequestException.class)
    public void testNoSatellite() {
        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.setMaster("http://master");
        validator.validate(ChannelConfig.builder()
                .withName("global")
                .withGlobal(globalConfig).build(), true, null);
    }

    @Test(expected = InvalidRequestException.class)
    public void testNoMaster() {
        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.addSatellite("http://master");
        validator.validate(ChannelConfig.builder()
                .withName("global")
                .withGlobal(globalConfig).build(), true, null);
    }

    @Test(expected = InvalidRequestException.class)
    public void testSatelliteSame() {
        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.setMaster("http://master");
        globalConfig.addSatellite("http://master/");
        validator.validate(ChannelConfig.builder()
                .withName("global")
                .withGlobal(globalConfig).build(), true, null);
    }

    @Test(expected = InvalidRequestException.class)
    public void testMasterUrl() throws MalformedURLException {
        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.setMaster("http:/master");
        globalConfig.addSatellite("http://satellite");
        validator.validate(ChannelConfig.builder()
                .withName("global")
                .withGlobal(globalConfig).build(), true, null);
    }

    @Test(expected = InvalidRequestException.class)
    public void testSatelliteUrl() throws MalformedURLException {
        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.setMaster("http:/master");
        globalConfig.addSatellite("http://satellite");
        globalConfig.addSatellite("ftp://satellite2");
        validator.validate(ChannelConfig.builder()
                .withName("global")
                .withGlobal(globalConfig).build(), true, null);
    }

    @Test(expected = InvalidRequestException.class)
    public void testHistoricalSwitch() throws Exception {
        ChannelConfig configA = ChannelConfig.builder().withName("A").withHistorical(false).build();
        ChannelConfig configB = ChannelConfig.builder().withName("B").withHistorical(true).build();
        validator.validate(configA, false, configB);
    }

    @Test(expected = InvalidRequestException.class)
    public void testHistoricalNotMax() throws Exception {
        ChannelConfig configA = ChannelConfig.builder().withName("A").withHistorical(true).withMaxItems(10).build();
        validator.validate(configA, true, null);
    }
}
