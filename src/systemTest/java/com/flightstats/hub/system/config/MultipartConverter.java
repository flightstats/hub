package com.flightstats.hub.system.config;

import lombok.extern.slf4j.Slf4j;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;
import retrofit2.internal.EverythingIsNonNull;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.stream.Stream;

@Slf4j
class MultipartConverter implements Converter<ResponseBody, String> {
    @EverythingIsNonNull
    @Override public String convert(ResponseBody value) throws IOException {
        return value.string();
    }
}

@Slf4j
class MultipartConverterFactory extends Converter.Factory {
    public static MultipartConverterFactory create() {
        return new MultipartConverterFactory();
    }

    private MultipartConverterFactory() {
    }

    @Override public @Nullable Converter<ResponseBody, ?> responseBodyConverter(
            Type type, Annotation[] annotations, Retrofit retrofit) {
        if (Stream.of(annotations).anyMatch(annotation -> annotation.toString().contains("multipart"))) {
            return new MultipartConverter();
        }
        return null;
    }
}