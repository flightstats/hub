package com.flightstats.hub.model;

import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.io.IOException;

public final class HubDateTimeTypeAdapter extends TypeAdapter<DateTime> {

    private static DateTimeFormatter dateTimeFormatter = ISODateTimeFormat.dateOptionalTimeParser().withZoneUTC();

    @Override
    public DateTime read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }
        return deserialize(in.nextString());
    }

    public static DateTime deserialize(String json) {
        if (StringUtils.isEmpty(json)) return null;
        DateTime parsed;
        try {
            parsed = dateTimeFormatter.parseDateTime(json);
        } catch (Exception ignored) {
            throw new JsonSyntaxException(json, ignored);
        }
        return parsed;
    }

    @Override
    public void write(JsonWriter out, DateTime value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }
        out.value(ISODateTimeFormat.dateTime().print(value));
    }
}
