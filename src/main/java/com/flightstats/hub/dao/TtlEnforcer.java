package com.flightstats.hub.dao;

import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.util.Commander;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.io.File;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class TtlEnforcer {

    private static final String LOST_AND_FOUND_DIR = "lost+found";
    private final Commander commander;

    @Inject
    public TtlEnforcer(Commander commander) {
        this.commander = commander;
    }

    public void deleteFilteredPaths(String path, ChannelService channelService,
                                    Consumer<ChannelConfig> channelConsumer) {
        try {
            String[] pathArray = Optional.ofNullable(new File(path).list()).orElse(new String[]{});
            Collection<ChannelConfig> channels = channelService.getChannels();
            channels.forEach(channelConsumer);
            Set<String> channelSet = channels.stream()
                    .map(ChannelConfig::getLowerCaseName)
                    .collect(Collectors.toSet());
            Stream.of(pathArray)
                    .map(String::toLowerCase)
                    .filter(channel -> !channelSet.contains(channel.toLowerCase()) && !channel.equals(LOST_AND_FOUND_DIR))
                    .forEach(dir -> {

                        String command = String.format("find %s/%s -type f --mmin +360 -delete", path, dir);
                        log.info("removing directory no longer associated with channel with command {}", command);
                        commander.runInBash(command, 1);
                    });
        } catch (Exception e) {
            log.error("unable to run {}", path, e);
        }
    }
}
