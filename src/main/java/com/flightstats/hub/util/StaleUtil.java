package com.flightstats.hub.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.net.URI;
import java.util.Map;
import java.util.function.Function;

@Singleton
public class StaleUtil {

    @Inject
    private static ObjectMapper mapper;

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
