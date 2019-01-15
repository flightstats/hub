package com.flightstats.hub.metrics;

import com.flightstats.hub.app.HubHost;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.rest.RestClient;
import com.sun.jersey.api.client.ClientResponse;
import com.timgroup.statsd.Event;
import com.timgroup.statsd.StatsDClient;
import org.joda.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

class DataDogMetricsService implements MetricsService {
    private final static Logger logger = LoggerFactory.getLogger(DataDogMetricsService.class);
    private final static StatsDClient statsd = DataDog.statsd;

    @Override
    public void insert(String channel, long start, Insert type, int items, long bytes) {
        if (shouldLog(channel)) {
            time(channel, "channel", start, bytes, "type:" + type.toString());
            count("channel.items", items, "type:" + type.toString(), "channel:" + channel);
        }
    }

    @Override
    public void event(String title, String text, String[] tags) {
        Event event = DataDog.getEventBuilder()
                .withTitle(title)
                .withText(text)
                .build();
        DataDog.statsd.recordEvent(event, tags);
    }

    @Override
    public void count(String name, long value, String... tags) {
        statsd.count(name, value, tags);
    }

    @Override
    public void increment(String name, String... tags) {
        statsd.increment(name, tags);
    }

    @Override
    public void gauge(String name, double value, String... tags) {
        statsd.gauge(name, value, tags);
    }

    @Override
    public void time(String name, long start, String... tags) {
        statsd.time(name, System.currentTimeMillis() - start, tags);
    }

    @Override
    public void time(String channel, String name, long start, String... tags) {
        if (shouldLog(channel)) {
            statsd.time(name, System.currentTimeMillis() - start, addChannelTag(channel, tags));
        }
    }

    @Override
    public void time(String channel, String name, long start, long bytes, String... tags) {
        if (shouldLog(channel)) {
            time(channel, name, start, tags);
            count(name + ".bytes", bytes, addChannelTag(channel, tags));
        }
    }

    @Override
    public void mute(){
        logger.info("Attempting to mute datadog");
        String api_key = HubProperties.getProperty("data_dog.api_key", "");
        String app_key = HubProperties.getProperty("data_dog.app_key", "");
        String name = HubHost.getLocalName();
        long end = (new Instant()).getMillis()/1000 + (4 * 60);

        if( "".equals(api_key) || "".equals(app_key)) {
            logger.warn("datadog api_key or app_key not defined");
            return;
        }

        String template = "{\n" +
                "      \"message\": \"restarting\",\n" +
                "      \"scope\": \"name:%s\",\n" +
                "      \"end\": %d\n" +
                "    }";
        String data = String.format(template,name, end);
        try {
            String url = "https://app.datadoghq.com/api/v1/downtime?api_key="
                    + api_key + "&application_key=" + app_key;
            ClientResponse response = RestClient.defaultClient().resource(url)
                    .type(MediaType.APPLICATION_JSON)
                    .post(ClientResponse.class, data);
            int status = response.getStatus();
            if (status >= 200 && status <= 299  ) {
                logger.info("Muted datadog monitoring: " + name + " during restart");
            } else {
                logger.warn("Muting datadog monitoring failed: " + name + " status " + status);
            }
        }catch(Exception e){
            logger.warn("Muting datadog error ", e);
        }
    }

    String[] addChannelTag(String channel, String... tags) {
        List<String> tagList = Arrays.stream(tags).collect(Collectors.toList());
        tagList.add("channel:" + channel);
        return tagList.toArray(new String[tagList.size()]);
    }

}
