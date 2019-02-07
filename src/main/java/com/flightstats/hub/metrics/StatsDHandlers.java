package com.flightstats.hub.metrics;

import com.flightstats.hub.rest.RestClient;
import com.flightstats.hub.util.TimeUtil;
import com.sun.jersey.api.client.ClientResponse;
import com.timgroup.statsd.Event;
import com.timgroup.statsd.StatsDClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class StatsDHandlers {
    private final Logger logger = LoggerFactory.getLogger(StatsDHandlers.class);

    private StatsDFilter statsDFilter;
    private MetricsConfig metricsConfig;

    public StatsDHandlers(
            MetricsConfig metricsConfig,
            StatsDFilter statsDFilter
    ) {
        this.metricsConfig = metricsConfig;
        this.statsDFilter = statsDFilter;
    }

    private void reportWithFilteredClients(String metric, Consumer<StatsDClient> methodFunction) {
        List<StatsDClient> clients = statsDFilter.getFilteredClients(metric);
        clients.forEach(methodFunction);
    }

    public void insert(String channel, long start, MetricInsert type, int items, long bytes) {
        if (isTestChannel.apply(channel)) return;
        time(channel, "channel", start, bytes, "type:" + type.toString());
        count("channel.items", items, "type:" + type.toString(), "channel:" + channel);
    }

    public void event(String title, String text, String[] tags) {
        Event event = Event.builder()
                .withHostname(metricsConfig.getHostTag())
                .withPriority(Event.Priority.NORMAL)
                .withAlertType(Event.AlertType.WARNING)
                .withTitle(title)
                .withText(text)
                .build();
        reportWithFilteredClients(title, (statsDClient -> statsDClient.recordEvent(event, tags)));
    }

    public void count(String name, long value, String... tags) {
        reportWithFilteredClients(name, (statsDClient -> statsDClient.count(name, value, tags)));
    }

    public void increment(String name, String... tags) {
        reportWithFilteredClients(name, (statsDClient) -> statsDClient.increment(name, tags));
    }

    public void gauge(String name, double value, String... tags) {
//        statsd.gauge(name, value, tags);
    }

    public void time(String name, long start, String... tags) {
        reportWithFilteredClients(name, (statsDClient) -> statsDClient.time(name, System.currentTimeMillis() - start, tags));
    }

    public void time(String channel, String name, long start, String... tags) {
        // !isTestChannel


//        statsd.time(name, System.currentTimeMillis() - start, addChannelTag(channel, tags));
    }

    public void time(String channel, String name, long start, long bytes, String... tags) {
        // !isTestChannel
        time(channel, name, start, tags);
        count(name + ".bytes", bytes, addChannelTag(channel, tags));

    }

    private String[] addChannelTag(String channel, String... tags) {
        List<String> tagList = Arrays
                .stream(tags)
                .collect(Collectors.toList());
        tagList.add("channel:" + channel);
        return tagList.toArray(new String[0]);
    }

    public void mute() {
        logger.info("Attempting to mute datadog");
        String api_key = metricsConfig.getDataDogAPIKey();
        String app_key = metricsConfig.getDataDogAppKey();
        String name = metricsConfig.getHostTag();

        long fourMinutesInSeconds = 4 * 60;
        long nowMillis = TimeUtil.now().getMillis();
        long fourMinutesFutureInSeconds = nowMillis / 1000 + fourMinutesInSeconds;

        if ("".equals(api_key) || "".equals(app_key)) {
            logger.warn("datadog api_key or app_key not defined");
            return;
        }
        String template = "{ \"message\": \"restarting\", \"scope\": \"name:%s\", \"end\": %d }";
        try {

            String data = String.format(template, name, fourMinutesFutureInSeconds);
            String url = "https://app.datadoghq.com/api/v1/downtime?api_key=" +
                    api_key +
                    "&application_key=" +
                    app_key;
            ClientResponse response = RestClient.defaultClient()
                    .resource(url)
                    .type(MediaType.APPLICATION_JSON)
                    .post(ClientResponse.class, data);

            int status = response.getStatus();
            if (status >= 200 && status <= 299) {
                logger.info("Muted datadog monitoring: " + name + " during restart");
            } else {
                logger.warn("Muting datadog monitoring failed: " + name + " status " + status);
            }
        } catch (Exception e) {
            logger.warn("Muting datadog error ", e);
        }
    }

    Function<String, Boolean> isTestChannel = (channel) -> channel.toLowerCase().startsWith("test_");

}
