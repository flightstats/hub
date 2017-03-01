package com.flightstats.hub.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubHost;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubProvider;
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
    private final static ObjectMapper mapper = HubProvider.getInstance(ObjectMapper.class);

    @Override
    public void insert(String channel, long start, Insert type, int items, long bytes) {
        time(channel, "channel", start, bytes, "type:" + type.toString());
        count("channel.items", items, "type:" + type.toString(), "channel:" + channel);
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
    public void gauge(String name, double value, String... tags) {
        statsd.gauge(name, value, tags);
    }

    @Override
    public void time(String name, long start, String... tags) {
        statsd.time(name, System.currentTimeMillis() - start, tags);
    }

    @Override
    public void time(String channel, String name, long start, String... tags) {
        //todo gfm - should time be histogram instead?
        statsd.time(name, System.currentTimeMillis() - start, addChannelTag(channel, tags));
    }

    @Override
    public void time(String channel, String name, long start, long bytes, String... tags) {
        time(channel, name, start, tags);
        count(name + ".bytes", bytes, addChannelTag(channel, tags));
    }

    @Override
    public void mute(){
        String api_key = HubProperties.getProperty("datadog.api_key", "");
        String app_key = HubProperties.getProperty("datadog.app_key", "");
        String name = HubHost.getLocalName();
        if( "".equals(api_key) || "".equals(app_key)) return;

        String url = "https://app.datadoghq.com/api/v1/downtime?api_key="
                    + api_key + "&application_key=" + app_key;
        ObjectNode root = mapper.createObjectNode();
        root.put("scope", "name:" + name);
        root.put("message", "restarting");
        root.put("end", (new Instant()).getMillis() + (4*60*60));

        ClientResponse response = RestClient.defaultClient().resource(url)
                .type(MediaType.APPLICATION_JSON)
                .put(ClientResponse.class, root);
        if(response.getStatus()==200){
            logger.info("Muting datadog monitoring: " + name + " during restart");
        }else{
            logger.warn("Muting datadog monitoring failed: " + name);
        }
    }

    String[] addChannelTag(String channel, String... tags) {
        List<String> tagList = Arrays.stream(tags).collect(Collectors.toList());
        tagList.add("channel:" + channel);
        return tagList.toArray(new String[tagList.size()]);
    }

}
