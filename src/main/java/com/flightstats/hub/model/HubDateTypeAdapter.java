package com.flightstats.hub.model;

import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Copied from Gson.  Would be nice to be able to specify the date format.
 */
public final class HubDateTypeAdapter extends TypeAdapter<Date> {

    private final DateFormat enUsFormat
            = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, Locale.US);
    private final DateFormat localFormat
            = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT);
    private final DateFormat iso8601Format = buildIso8601Format();

    private static DateFormat buildIso8601Format() {
        //this is "yyyy-MM-dd'T'HH:mm:ss'Z'" in DateTypeAdapter
        DateFormat iso8601Format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        iso8601Format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return iso8601Format;
    }

    @Override
    public Date read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }
        return deserializeToDate(in.nextString());
    }

    private synchronized Date deserializeToDate(String json) {
        try {
            return localFormat.parse(json);
        } catch (ParseException ignored) {
        }
        try {
            return enUsFormat.parse(json);
        } catch (ParseException ignored) {
        }
        try {
            return iso8601Format.parse(json);
        } catch (ParseException e) {
            throw new JsonSyntaxException(json, e);
        }
    }

    @Override
    public synchronized void write(JsonWriter out, Date value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }
        String dateFormatAsString = iso8601Format.format(value);
        out.value(dateFormatAsString);
    }
}
