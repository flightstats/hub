package com.flightstats.hub.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubProvider;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.net.URI;
import java.util.Map;
import java.util.function.Function;

public class StaleUtil {

    private final static ObjectMapper mapper = HubProvider.getInstance(ObjectMapper.class);

    public static void addStaleEntities(ObjectNode root, int age, Function<DateTime, Map<DateTime, URI>> entitySupplier) {
        DateTime staleCutoff = DateTime.now().minusMinutes(age);

        ObjectNode stale = root.putObject("stale");
        stale.put("stale minutes", age);
        stale.put("stale cutoff", staleCutoff.toDateTime(DateTimeZone.UTC).toString());

        ArrayNode uris = stale.putArray("uris");
        entitySupplier.apply(staleCutoff).forEach((channelLastUpdate, channelURI) -> {
            ObjectNode node = mapper.createObjectNode();
            node.put("last update", channelLastUpdate.toDateTime(DateTimeZone.UTC).toString());
            node.put("uri", channelURI.toString());
            uris.add(node);
        });
    }
}
