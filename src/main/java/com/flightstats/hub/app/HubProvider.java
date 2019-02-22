package com.flightstats.hub.app;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

@Deprecated
public class HubProvider {

    private static Injector injector;

    public static <T> T getInstance(Class<T> type) {
        return injector.getInstance(type);
    }

    public static <T> T getInstance(Class<T> type, String name) {
        return injector.getInstance(Key.get(type, Names.named(name)));
    }

    public static <T> T getInstance(TypeLiteral<T> type, String name) {
        return injector.getInstance(Key.get(type, Names.named(name)));
    }

    public static Injector getInjector() {
        return injector;
    }

    static void setInjector(Injector injector) {
        HubProvider.injector = injector;
    }
}
