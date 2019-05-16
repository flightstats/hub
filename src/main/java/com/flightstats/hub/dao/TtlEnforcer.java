package com.flightstats.hub.dao;

import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.util.Commander;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TtlEnforcer {
    private final static Logger logger = LoggerFactory.getLogger(TtlEnforcer.class);

    public static void enforce(String path, ChannelService channelService,
                               Consumer<ChannelConfig> channelConsumer) {
        try {
            String[] pathArray = Optional.ofNullable(new File(path).list()).orElse(new String[] {});
            Collection<ChannelConfig> channels = channelService.getChannels();
            channels.forEach(channelConsumer);
            Set<String> channelSet = channels.stream()
                    .map(ChannelConfig::getLowerCaseName)
                    .collect(Collectors.toSet());
            Stream.of(pathArray)
                    .map(String::toLowerCase)
                    .filter(channel -> !channelSet.contains(channel.toLowerCase()) && !channel.equals("lost+found"))
                    .forEach(dir -> {
                        String dirPath = path + "/" + dir;
                        logger.info("removing dir without channel {}", dirPath);
                        Commander.run(new String[]{"rm", "-rf", dirPath}, 1);
                    });
        } catch (Exception e) {
            logger.warn("unable to run " + path, e);
        }
    }
}
